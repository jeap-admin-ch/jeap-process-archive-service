package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.kafka.KafkaDomainEventConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain4EventBuilder;
import ch.admin.bit.jeap.processarchive.kafka.TestDomainEventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.test.DecreeReference;
import ch.admin.bit.jeap.processarchive.test.decree.v2.Decree;
import ch.admin.bit.jeap.processcontext.event.test.TestDomainEvent;
import ch.admin.bit.jeap.processcontext.event.test4.TestDomain4Event;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost:12332/testdata/{id}",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test"})
@Import({HashProviderTestConfig.class, TestTypeLoaderConfig.class})
@EnableWireMock(@ConfigureWireMock(port = 12332))
class ArchiveServiceRemoteDataExtractorIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-1";
    private static final String REFERENCE_ID = "dataId-1234";

    private static final String DOMAIN_EVENT_4_TOPIC = "test-event-4";

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;
    @MockitoBean
    private ArtifactArchivedListener artifactArchivedListener;
    @Captor
    private ArgumentCaptor<ArchivedArtifact> archivedArtifactArgumentCaptor;
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void when_eventReceived_then_shouldArchiveDataFromPayload() throws Exception {
        // given
        String system = "JME";
        String schema = "Decree";
        String schemaVersion = "1";
        String contentType = "avro/binary";
        String weather = "sunny";
        byte[] payload = createPayload();
        stubFor(get(urlEqualTo("/testdata/" + REFERENCE_ID)).willReturn(aResponse()
                .withHeader("Content-Type", contentType)
                .withHeader("Archive-Data-System", system)
                .withHeader("Archive-Data-Schema", schema)
                .withHeader("Archive-Data-Schema-Version", schemaVersion)
                .withHeader("Archive-Metadata-weather", weather)
                .withBody(payload)
        ));
        TestDomainEvent testDomainEvent = createTestEvent();

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertEquals(testDomainEvent.getProcessId(), archivedArtifact.getProcessId());
        assertArrayEquals(payload, archivedArtifact.getArchiveData().getPayload());
        assertEquals(contentType, archivedArtifact.getArchiveData().getContentType());
        assertEquals(system, archivedArtifact.getArchiveData().getSystem());
        assertEquals(schema, archivedArtifact.getArchiveData().getSchema());
        assertEquals(1, archivedArtifact.getArchiveData().getSchemaVersion());
        assertEquals(1, archivedArtifact.getArchiveData().getMetadata().size());
        assertEquals("weather", archivedArtifact.getArchiveData().getMetadata().get(0).getName());
        assertEquals(weather, archivedArtifact.getArchiveData().getMetadata().get(0).getValue());
    }

    @Test
    void when_eventReceived_then_shouldArchiveDataFromPayload_fromArchiveDataReferenceProvider() throws Exception {
        // given
        String system = "JME";
        String schema = "Decree";
        String schemaVersion = "1";
        String contentType = "avro/binary";
        String weather = "sunny";
        byte[] payload = createPayload();
        stubFor(get(urlEqualTo("/testdata/" + REFERENCE_ID)).willReturn(aResponse()
                .withHeader("Content-Type", contentType)
                .withHeader("Archive-Data-System", system)
                .withHeader("Archive-Data-Schema", schema)
                .withHeader("Archive-Data-Schema-Version", schemaVersion)
                .withHeader("Archive-Metadata-weather", weather)
                .withBody(payload)
        ));
        TestDomain4Event testDomainEvent = createTest4Event();

        // when
        kafkaTemplate.send(DOMAIN_EVENT_4_TOPIC, testDomainEvent).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertEquals(testDomainEvent.getProcessId(), archivedArtifact.getProcessId());
        assertArrayEquals(payload, archivedArtifact.getArchiveData().getPayload());
        assertEquals(contentType, archivedArtifact.getArchiveData().getContentType());
        assertEquals(system, archivedArtifact.getArchiveData().getSystem());
        assertEquals(schema, archivedArtifact.getArchiveData().getSchema());
        assertEquals(1, archivedArtifact.getArchiveData().getSchemaVersion());
        assertEquals(1, archivedArtifact.getArchiveData().getMetadata().size());
        assertEquals("weather", archivedArtifact.getArchiveData().getMetadata().get(0).getName());
        assertEquals(weather, archivedArtifact.getArchiveData().getMetadata().get(0).getValue());
    }

    private byte[] createPayload() throws IOException {
        SpecificRecord data = Decree.newBuilder()
                .setPayload("123")
                .setDecreeReference(DecreeReference.newBuilder()
                        .setType("decree-id")
                        .setId("123")
                        .build())
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
        datumWriter.write(data, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }

    private TestDomainEvent createTestEvent() {
        return TestDomainEventBuilder.builder()
                .idempotenceId("test")
                .dataId(REFERENCE_ID)
                .build();
    }

    private TestDomain4Event createTest4Event() {
        return TestDomain4EventBuilder.builder()
                .idempotenceId("test")
                .payloadData("payload-data", REFERENCE_ID)
                .build();
    }

    @BeforeEach
    void setUp() {
        kafkaDomainEventConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
