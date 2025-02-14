package ch.admin.bit.jeap.processarchive.crypto;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveDataEncryptionTest {

    @Test
    void from() {
        ArchiveDataSchema archiveDataSchema = ArchiveDataSchema.builder()
                .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
                .system("test-system")
                .name("schemaname")
                .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
                .version(1)
                .fileExtension("avpr")
                .build();

        ArchiveDataEncryption encryption = ArchiveDataEncryption.from(archiveDataSchema);

        assertThat(encryption).isNull();
    }

    @Test
    void from_withEncryptionKeyId() {
        ArchiveDataSchema archiveDataSchema = ArchiveDataSchema.builder()
                .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
                .system("test-system")
                .name("schemaname")
                .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
                .version(1)
                .fileExtension("avpr")
                .encryptionKeyId(EncryptionKeyId.builder()
                        .keyId("keyId").build())
                .build();

        ArchiveDataEncryption encryption = ArchiveDataEncryption.from(archiveDataSchema);

        assertThat(encryption.getEncryptionKeyId().getKeyId())
                .isEqualTo("keyId");
    }

    @Test
    void from_withEncryptionKeyReference() {
        ArchiveDataSchema archiveDataSchema = ArchiveDataSchema.builder()
                .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
                .system("test-system")
                .name("schemaname")
                .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
                .version(1)
                .fileExtension("avpr")
                .encryptionKeyReference(EncryptionKeyReference.builder()
                        .keyName("keyName")
                        .secretEnginePath("path")
                        .build())
                .build();

        ArchiveDataEncryption encryption = ArchiveDataEncryption.from(archiveDataSchema);

        assertThat(encryption.getEncryptionKeyReference().getKeyName())
                .isEqualTo("keyName");
        assertThat(encryption.getEncryptionKeyReference().getSecretEnginePath())
                .isEqualTo("path");
    }
}
