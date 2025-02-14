package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.ArchiveTypeReference;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class RegistryConnectorTest {
    private final static String REPO_URL = "https://bitbucket.bit.admin.ch/projects/bit_jme/repos/jme-archive-type-registry/";
    private final static GitReference BRANCH = GitReference.ofBranch("master");
    private final static String COMMIT_HASH = "380a6cf2637";
    private final static ArchiveTypeReference ARCHIVE_TYPE_REFERENCE_REFERENCE = new ArchiveTypeReference("JME", "Decree", 1);
    private final static ArchiveTypeVersion version1 = new ArchiveTypeVersion(1, "Decree_v1.avdl");

    @BeforeAll
    static void setUp() {
        String filePath = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource("truststore.jks")).getFile();
        System.setProperty("javax.net.ssl.trustStore", filePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @Test
    void downloadDescriptorFromRegistry() throws RegistryConnectorException {
        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        ArchiveTypeDescriptor result = target.downloadDescriptorFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE);

        assertNotNull(result);
        assertEquals("Decree", result.getArchiveType());
        assertEquals("JME", result.getSystem());
        assertEquals(version1, result.getVersions().get(0));
    }

    @Test
    void downloadDescriptorFromRegistry_atCommit() throws RegistryConnectorException {
        RegistryConnector target = new RegistryConnector(REPO_URL, GitReference.ofCommit(COMMIT_HASH));
        ArchiveTypeDescriptor result = target.downloadDescriptorFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE);

        assertNotNull(result);
        assertEquals("Decree", result.getArchiveType());
        assertEquals("JME", result.getSystem());
        assertEquals(version1, result.getVersions().get(0));
    }

    @Test
    void downloadDescriptorFromRegistryNotExisting() {
        ArchiveTypeReference archiveTypeReference = new ArchiveTypeReference("JME", "NotExistingType", 1);

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        assertThrows(RegistryConnectorException.class, () -> target.downloadDescriptorFromRegistry(archiveTypeReference));
    }

    @Test
    void downloadSchemaFromRegistry() throws RegistryConnectorException, IOException {

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        File result = target.downloadSchemaFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE, version1.getSchema());

        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(FileUtils.readFileToString(result, StandardCharsets.UTF_8.name()).contains("protocol DecreeProtocol"));
    }

    @Test
    void downloadSchemaFromRegistryNotExisting() {

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        assertThrows(RegistryConnectorException.class, () -> target.downloadSchemaFromRegistry(ARCHIVE_TYPE_REFERENCE_REFERENCE, "wrong"));
    }

    @Test
    void downloadCommonFilesFromRegistry() {

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        Map<String, File> result = target.downloadCommonFilesFromRegistry(null);

        //Currently there are no such files, but there might be in future so we cannot rely on this...
        //So we do not check the result
        assertNotNull(result);
    }

    @Test
    void downloadCommonFilesFromRegistryForSystemEmpty() {

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        Map<String, File> result = target.downloadCommonFilesFromRegistry("notexisting");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void downloadCommonFilesFromRegistryForJME() throws IOException {

        RegistryConnector target = new RegistryConnector(REPO_URL, BRANCH);
        Map<String, File> result = target.downloadCommonFilesFromRegistry("jme");

        assertNotNull(result);
        assertFalse(result.isEmpty(), "There is at least one common file for JME");
        assertNotNull(result.get("ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"));
        assertTrue(
                FileUtils.readFileToString(result.get("ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"), "utf-8")
                        .contains("protocol DecreeReferenceProtocol"), "valid content");
    }
}
