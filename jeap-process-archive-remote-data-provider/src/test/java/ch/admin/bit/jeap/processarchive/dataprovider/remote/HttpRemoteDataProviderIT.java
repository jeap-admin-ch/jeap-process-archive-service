package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import ch.admin.bit.jeap.processarchive.domain.archive.DomainEventArchiveService;
import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventListenerAdapter;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventReceiver;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnableWireMock
class HttpRemoteDataProviderIT {

    private static final String TEST_REMOTE_PAYLOAD = "test-remote-payload";

    @MockitoBean
    private DomainEventArchiveService domainEventArchiveService;
    @MockitoBean
    private DomainEventReceiver domainEventReceiver;
    @MockitoBean
    private DomainEventArchiveConfigurationRepository domainEventArchiveConfigurationRepository;
    @MockitoBean
    private DomainEventListenerAdapter domainEventListenerAdapter;
    @MockitoBean
    private ArchivedArtifactCreatedEventProducer archivedArtifactCreatedEventProducer;
    @Autowired
    private HttpRemoteDataProvider httpRemoteDataProvider;

    @Value("${wiremock.server.baseUrl}")
    private String wiremockBaseUrl;

    @Test
    void when_referenceWithVersionFoundAndValidResponse_then_shouldCreateArchiveData() {
        final ArchiveDataReference reference = ArchiveDataReference.builder()
                .id("dataId-1234")
                .version(2)
                .build();
        when_referenceFoundAndValidResponse_then_shouldCreateArchiveData(reference, getEndpointTemplateVersioned());
    }

    @Test
    void when_referenceWithoutVersionFoundAndValidResponse_then_shouldCreateArchiveData() {
        final ArchiveDataReference reference = ArchiveDataReference.builder()
                .id("dataId-9876")
                .build();
        when_referenceFoundAndValidResponse_then_shouldCreateArchiveData(reference, getEndpointTemplateUnversioned());
    }

    @Test
    void when_NoVersionProvidedWithReferenceButVersionInEndpointTemplate_then_ShouldThrowException() {
        final ArchiveDataReference reference = ArchiveDataReference.builder()
                .id("dataId-1984")
                .build();
        assertThatThrownBy(() ->
                when_referenceFoundAndValidResponse_then_shouldCreateArchiveData(reference, getEndpointTemplateVersioned()))
        .isInstanceOf(RemoteDataProviderException.class)
        .hasMessageContaining("additional template uri parameters 'version'");
    }

    @Test
    void when_VersionProvidedWithReferenceButNoVersionInEndpointTemplate_then_ShouldThrowException()  {
        final ArchiveDataReference reference = ArchiveDataReference.builder()
                .id("dataId-1984")
                .version(2)
                .build();
        assertThatThrownBy(() ->
            when_referenceFoundAndValidResponse_then_shouldCreateArchiveData(reference, getEndpointTemplateUnversioned()))
                .isInstanceOf(RemoteDataProviderException.class)
                .hasMessageContaining("missing template uri parameters 'version'");
    }

    private void when_referenceFoundAndValidResponse_then_shouldCreateArchiveData(final ArchiveDataReference reference, String endpointTemplate) {
        // given
        String system = "test-system";
        String schema = "test-schema";
        String schemaVersion = "2";
        String contentType = MediaType.APPLICATION_XML_VALUE;
        String weather = "sunny";
        stubFor(get(urlEqualTo(getEndpointPath(reference)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", contentType)
                        .withHeader("Archive-Data-System", system)
                        .withHeader("Archive-Data-Schema", schema)
                        .withHeader("Archive-Data-Schema-Version", schemaVersion)
                        .withHeader("Archive-Metadata-weather", weather)
                        .withBody(TEST_REMOTE_PAYLOAD)
                ));

        // when
        ArchiveData archiveData = httpRemoteDataProvider.readArchiveData(endpointTemplate, null, reference);

        // then
        assertEquals(system, archiveData.getSystem());
        assertEquals(schema, archiveData.getSchema());
        assertEquals(2, archiveData.getSchemaVersion());
        assertEquals(contentType, archiveData.getContentType());
        assertArrayEquals(TEST_REMOTE_PAYLOAD.getBytes(StandardCharsets.UTF_8), archiveData.getPayload());
        assertEquals(1, archiveData.getMetadata().size());
        assertEquals("weather", archiveData.getMetadata().get(0).getName());
        assertEquals(weather, archiveData.getMetadata().get(0).getValue());
    }

    @Test
    void when_referenceIdNotFound_then_shouldThrow() {
        // given
        // no stubbed response -> 404
        final ArchiveDataReference reference = ArchiveDataReference.builder().id("invalid-ref-id").version(1).build();
        try {
            // when
            httpRemoteDataProvider.readArchiveData(getEndpointTemplateVersioned(), null, reference);
            fail("Expected exception");
        } catch (RemoteDataProviderException remoteDataProviderException) {
            // then
            assertTrue(remoteDataProviderException.getMessage().contains("HTTP status code '404"));
        }
    }

    @Test
    void when_missingMandatoryHeader_then_shouldThrow() {
        // given
        final ArchiveDataReference reference = ArchiveDataReference.builder().id("dataId-456").version(5).build();
        String contentType = MediaType.APPLICATION_XML_VALUE;
        stubFor(get(urlEqualTo(getEndpointPath(reference))).willReturn(aResponse()
                .withHeader("Content-Type", contentType)
                // No schema header
                .withBody(TEST_REMOTE_PAYLOAD)
        ));

        try {
            // when
            httpRemoteDataProvider.readArchiveData(getEndpointTemplateVersioned(), null, reference);
            fail("Expected exception");
        } catch (RemoteDataProviderException remoteDataProviderException) {
            // then
            assertTrue(remoteDataProviderException.getMessage().contains("missing mandatory header"));
        }
    }

    private String getEndpointTemplateUnversioned() {
        return wiremockBaseUrl + "/testdata/{id}";
    }

    private String getEndpointTemplateVersioned() {
        return getEndpointTemplateUnversioned() + "?version={version}";
    }

    private String getEndpointPath(ArchiveDataReference reference) {
        final String unversionedPath = "/testdata/" + reference.getId();
        if (reference.getVersion() == null) {
            return unversionedPath;
        }
        else {
            return unversionedPath + "?version=" + reference.getVersion();
        }
    }

}

