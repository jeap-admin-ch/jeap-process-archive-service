package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.RegistryConnector;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.RegistryConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {
    private final static File REFERENCE_FILE = new File("src/test/resources/unittest/existing.json");
    private final static ArchiveTypeReference TYPE_REFERENCE = new ArchiveTypeReference("JME", "Decree", 1);
    private final static ArchiveTypeVersion TYPE_VERSION = new ArchiveTypeVersion(1, "Decree_v1.avdl");
    private final static ArchiveTypeDescriptor TYPE_DESCRIPTOR = new ArchiveTypeDescriptor(
            TYPE_REFERENCE.getSystem(),
            TYPE_REFERENCE.getName(),
            "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
            List.of(TYPE_VERSION),
            30,
            null, null);
    private final static File SCHEMA_FILE = new File("Decree_v1.avdl");

    @Mock(lenient = true)
    private RegistryConnector registryConnector;

    @BeforeEach
    void setup() throws RegistryConnectorException {
        when(registryConnector.downloadCommonFilesFromRegistry(null)).thenReturn(Map.of());
        when(registryConnector.downloadCommonFilesFromRegistry(TYPE_REFERENCE.getSystem().toLowerCase())).thenReturn(Map.of());
        when(registryConnector.downloadDescriptorFromRegistry(eq(TYPE_REFERENCE))).thenReturn(TYPE_DESCRIPTOR);
        when(registryConnector.downloadSchemaFromRegistry(TYPE_REFERENCE, TYPE_VERSION.getSchema())).thenReturn(SCHEMA_FILE);
    }

    @Test
    void eventFileNotExists() {
        File eventReferenceFile = new File("notexists.json");
        RegistryException e = assertThrows(RegistryException.class, () -> new RegistryService(eventReferenceFile));
        assertEquals(FileNotFoundException.class, e.getCause().getClass());
    }

    @Test
    void fileNotValid() {
        File eventReferenceFile = new File("src/test/resources/unittest/invalid.json");
        assertThrows(RegistryException.class, () -> new RegistryService(eventReferenceFile));
    }

    @Test
    void eventNotExisting() throws RegistryConnectorException {
        when(registryConnector.downloadDescriptorFromRegistry(eq(TYPE_REFERENCE))).thenThrow(RegistryConnectorException.class);

        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        assertThrows(RegistryException.class, target::download);
    }

    @Test
    void schemaNotExisting() throws RegistryConnectorException {
        when(registryConnector.downloadSchemaFromRegistry(TYPE_REFERENCE, TYPE_VERSION.getSchema())).thenThrow(RegistryConnectorException.class);

        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        assertThrows(RegistryException.class, target::download);
    }

    @Test
    void valid() {
        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        List<DownloadedSchema> files = target.download().getSchemas();

        assertEquals(1, files.size());
        assertEquals(SCHEMA_FILE, files.get(0).getSchema());
    }

    @Test
    void emptyImportPath() {
        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        List<DownloadedSchema> files = target.download().getSchemas();

        assertEquals(1, files.size());
        assertEquals(Map.<String, File>of(), files.get(0).getImportPath());
    }

    @Test
    void commonFile() {
        File commonFile = new File("commonFile");
        when(registryConnector.downloadCommonFilesFromRegistry(null)).thenReturn(Map.of(commonFile.getName(), commonFile));

        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        List<DownloadedSchema> files = target.download().getSchemas();

        assertEquals(1, files.size());
        assertEquals(Map.of(commonFile.getName(), commonFile), files.get(0).getImportPath());
    }

    @Test
    void systemCommonFile() {
        File commonFile = new File("commonFile");
        File systemCommonFile = new File("systemCommonFile");
        when(registryConnector.downloadCommonFilesFromRegistry(null)).thenReturn(Map.of(commonFile.getName(), commonFile));
        when(registryConnector.downloadCommonFilesFromRegistry(TYPE_REFERENCE.getSystem().toLowerCase())).thenReturn(Map.of(systemCommonFile.getName(), systemCommonFile));

        RegistryService target = new RegistryService(REFERENCE_FILE, e -> registryConnector);
        List<DownloadedSchema> files = target.download().getSchemas();

        assertEquals(1, files.size());
        assertEquals(Map.of(commonFile.getName(), commonFile, systemCommonFile.getName(), systemCommonFile), files.get(0).getImportPath());
    }
}
