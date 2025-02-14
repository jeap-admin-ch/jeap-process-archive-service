package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.ArchiveTypeReference;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class RegistryConnectorTest {

    private final static GitReference BRANCH = GitReference.ofBranch("master");
    private final static String COMMIT_HASH = "380a6cf2637";
    private final static ArchiveTypeReference ARCHIVE_TYPE_REFERENCE_REFERENCE = new ArchiveTypeReference("JEAP", "Decree", 1);
    private final static ArchiveTypeVersion version1 = new ArchiveTypeVersion(1, "Decree_v1.avdl");

    @Test
    void downloadDescriptorFromRegistry(WireMockRuntimeInfo wm) throws RegistryConnectorException {

        stubFor(get(urlEqualTo("/raw/archive-types/jeap/decree/Decree.json?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/decree/Decree.json"))));

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        ArchiveTypeDescriptor result = target.downloadDescriptorFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE);

        assertNotNull(result);
        assertEquals("Decree", result.getArchiveType());
        assertEquals("JEAP", result.getSystem());
        assertEquals(version1, result.getVersions().get(0));
    }

    @Test
    void downloadDescriptorFromRegistry_atCommit(WireMockRuntimeInfo wm) throws RegistryConnectorException {
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/decree/Decree.json?at=380a6cf2637"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/decree/Decree.json"))));

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), GitReference.ofCommit(COMMIT_HASH));
        ArchiveTypeDescriptor result = target.downloadDescriptorFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE);

        assertNotNull(result);
        assertEquals("Decree", result.getArchiveType());
        assertEquals("JEAP", result.getSystem());
        assertEquals(version1, result.getVersions().get(0));
    }

    @Test
    void downloadDescriptorFromRegistryNotExisting(WireMockRuntimeInfo wm) {
        ArchiveTypeReference archiveTypeReference = new ArchiveTypeReference("JEAP", "NotExistingType", 1);

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        assertThrows(RegistryConnectorException.class, () -> target.downloadDescriptorFromRegistry(archiveTypeReference));
    }

    @Test
    void downloadSchemaFromRegistry(WireMockRuntimeInfo wm) throws RegistryConnectorException, IOException {

        stubFor(get(urlEqualTo("/raw/archive-types/jeap/decree/Decree_v1.avdl?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/decree/Decree_v1.avdl"))));

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        File result = target.downloadSchemaFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE, version1.getSchema());

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(FileUtils.readFileToString(result, StandardCharsets.UTF_8.name()).contains("protocol DecreeProtocol"));
    }

    @Test
    void downloadSchemaFromRegistryNotExisting(WireMockRuntimeInfo wm) {

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        assertThrows(RegistryConnectorException.class, () -> target.downloadSchemaFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE, "wrong"));
    }

    @Test
    void downloadCommonFilesFromRegistryForSystemEmpty(WireMockRuntimeInfo wm) {

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        Map<String, File> result = target.downloadCommonFilesFromRegistry("notexisting");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void downloadCommonFilesFromRegistryForJEAP(WireMockRuntimeInfo wm) throws IOException {

        stubFor(get(urlEqualTo("/raw/archive-types/jeap/_common?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("1\tch.admin.bit.jeap.processarchive.test.DecreeReference.avdl")));
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/_common/ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/_common/ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"))));

        RegistryConnector target = new RegistryConnector(wm.getHttpBaseUrl(), BRANCH);
        Map<String, File> result = target.downloadCommonFilesFromRegistry("jeap");

        assertNotNull(result);
        assertFalse(result.isEmpty(), "There is at least one common file for JEAP");
        assertNotNull(result.get("ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"));
        assertTrue(
                FileUtils.readFileToString(result.get("ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"), "utf-8")
                        .contains("protocol DecreeReferenceProtocol"), "valid content");
    }

    private static String loadResource(String path) {
        byte[] bytes = null;
        try {
            bytes = RegistryConnectorTest.class.getResourceAsStream(path).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(bytes);
    }
}
