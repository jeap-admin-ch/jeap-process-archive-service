package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {ArchiveTypeRepository.class, ArchiveTypeLoader.class, TestArchiveTypeProvider.class}, properties = {"spring.cloud.vault.enabled=false"})
class ArchiveTypeRepositoryTest {

    @Autowired
    private ArchiveTypeRepository repository;

    @MockitoBean
    private ArchiveCryptoService archiveCryptoService;

    @Test
    void requireArchiveType_v1() {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("Decree")
                .version(1)
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        assertEquals("Decree", archiveType.getSchema().getName());
        assertEquals("jme-process-archive-example-key", archiveType.getEncryptionKey().getKeyId());
    }

    @Test
    void requireArchiveType_Document_v1() {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("DecreeDocument")
                .version(1)
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        assertEquals("DecreeDocument", archiveType.getSchema().getName());
        assertEquals("jeap/test", archiveType.getEncryption().getSecretEnginePath());
        assertEquals("test-key", archiveType.getEncryption().getKeyName());
    }

    @Test
    void requireArchiveType_shouldThrowExceptionIfNotFound() {
        int invalidVersion = 999;

        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("Decree")
                .version(invalidVersion)
                .build();

        assertThrows(ArchiveTypeLoaderException.class, () ->
                repository.requireArchiveType(schemaId));
    }
}
