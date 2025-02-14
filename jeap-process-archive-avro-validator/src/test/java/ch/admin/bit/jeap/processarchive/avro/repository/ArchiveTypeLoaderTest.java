package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveTypeLoaderTest {

    @Test
    @SneakyThrows
    void testLoad() {
        ArchiveTypeLoader loader = new ArchiveTypeLoader();
        Map<ArchiveTypeId, ArchiveType> archiveTypeIdArchiveTypeMap = loader.loadArchiveTypes();
        assertThat(archiveTypeIdArchiveTypeMap.keySet()).hasSize(4);
        assertThat(archiveTypeIdArchiveTypeMap.values().stream().filter(a -> a.getEncryption() != null).toList())
                .hasSize(1);
        assertThat(archiveTypeIdArchiveTypeMap.values().stream().filter(a -> a.getEncryptionKey() != null).toList())
                .hasSize(1);
        assertThat(archiveTypeIdArchiveTypeMap.values().stream().filter(a -> a.getEncryption() == null && a.getEncryptionKey() == null).toList())
                .hasSize(2);
    }
}
