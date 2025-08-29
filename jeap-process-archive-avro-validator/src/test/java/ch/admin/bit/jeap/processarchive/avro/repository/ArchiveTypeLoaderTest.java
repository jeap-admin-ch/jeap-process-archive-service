package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveTypeLoaderTest {

    @Test
    @SneakyThrows
    void testLoad() {
        ArchiveTypeLoader loader = new ArchiveTypeLoader(new TestArchiveTypeProvider());

        Map<ArchiveTypeId, ArchiveType> archiveTypeIdArchiveTypeMap = loader.loadArchiveTypes();

        assertThat(archiveTypeIdArchiveTypeMap.keySet()).hasSize(2);
        ArchiveTypeId archiveTypeIdV1 = ArchiveTypeId.builder()
                .system("JME")
                .name("Decree")
                .version(1)
                .build();
        ArchiveTypeId archiveTypeIdV2 = ArchiveTypeId.builder()
                .system("JME")
                .name("Decree")
                .version(2)
                .build();
        assertThat(archiveTypeIdArchiveTypeMap).containsKeys(archiveTypeIdV1, archiveTypeIdV2);

        ArchiveType archiveTypeV1 = archiveTypeIdArchiveTypeMap.get(archiveTypeIdV1);
        assertThat(archiveTypeV1.getExpirationDays()).isEqualTo(30);
        assertThat(archiveTypeV1.getSchema().getName()).isEqualTo("Decree");
        assertThat(archiveTypeV1.getName()).isEqualTo("Decree");
        assertThat(archiveTypeV1.getSystem()).isEqualTo("JME");
        assertThat(archiveTypeV1.getVersion()).isEqualTo(1);
        assertThat(archiveTypeV1.getReferenceIdType()).isEqualTo("ch.admin.bit.jeap.audit.type.JmeDecreeArtifact");
        assertThat(archiveTypeV1.getEncryptionKey().getKeyId()).isEqualTo("jme-process-archive-example-key");
        assertThat(archiveTypeV1.getEncryption()).isNull();

        ArchiveType archiveTypeV2 = archiveTypeIdArchiveTypeMap.get(archiveTypeIdV2);
        assertThat(archiveTypeV2.getExpirationDays()).isEqualTo(60);
        assertThat(archiveTypeV2.getSchema().getName()).isEqualTo("Decree");
        assertThat(archiveTypeV2.getName()).isEqualTo("Decree");
        assertThat(archiveTypeV2.getSystem()).isEqualTo("JME");
        assertThat(archiveTypeV2.getVersion()).isEqualTo(2);
        assertThat(archiveTypeV2.getReferenceIdType()).isEqualTo("ch.admin.bit.jeap.audit.type.JmeDecreeArtifact");
        assertThat(archiveTypeV2.getEncryption().getSecretEnginePath()).isEqualTo("secret/engine");
        assertThat(archiveTypeV2.getEncryption().getKeyName()).isEqualTo("key-name");
        assertThat(archiveTypeV2.getEncryptionKey()).isNull();
    }
}
