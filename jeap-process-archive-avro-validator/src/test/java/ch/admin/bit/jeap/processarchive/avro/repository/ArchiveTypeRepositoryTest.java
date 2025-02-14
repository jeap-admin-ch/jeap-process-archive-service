package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {ArchiveTypeRepository.class, ArchiveTypeLoader.class}, properties = {"spring.cloud.vault.enabled=false"})
class ArchiveTypeRepositoryTest {

    @Autowired
    private ArchiveTypeRepository repository;

    @MockitoBean
    private ArchiveCryptoService archiveCryptoService;

    @Test
    void requireArchiveType() {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("Decree")
                .version(2)
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        assertEquals("Decree", archiveType.getSchema().getName());
    }

    @Test
    void requireArchiveType_encrypted() {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("DecreeEncrypted")
                .version(1)
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        assertEquals("DecreeEncrypted", archiveType.getSchema().getName());
        assertEquals("transit/jme", archiveType.getEncryption().getSecretEnginePath());
    }

    @Test
    void requireArchiveType_encryptedByKeyId() {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system("JME")
                .name("DecreeEncryptedByKeyId")
                .version(1)
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        assertEquals("DecreeEncryptedByKeyId", archiveType.getSchema().getName());
        assertEquals("test-key", archiveType.getEncryptionKey().getKeyId());
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
