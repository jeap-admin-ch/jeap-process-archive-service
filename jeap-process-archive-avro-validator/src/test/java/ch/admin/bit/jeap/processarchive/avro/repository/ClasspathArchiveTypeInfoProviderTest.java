package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AvroArchiveTypeRepository.class, ArchiveTypeLoader.class, ClasspathArchiveTypeInfoProvider.class, TestArchiveTypeProvider.class},
        properties = {"spring.cloud.vault.enabled=false"})
class ClasspathArchiveTypeInfoProviderTest {

    @Autowired
    private ClasspathArchiveTypeInfoProvider provider;

    @MockitoBean
    private ArchiveCryptoService archiveCryptoService;

    @Test
    void getArchiveTypes_returnsAllTypes() {
        List<ArchiveTypeInfo> types = provider.getArchiveTypes();

        assertThat(types).hasSize(3);
    }

    @Test
    void getArchiveTypes_mapsFieldsCorrectly() {
        List<ArchiveTypeInfo> types = provider.getArchiveTypes();

        ArchiveTypeInfo decreeV1 = types.stream()
                .filter(t -> t.getName().equals("Decree") && t.getVersion() == 1)
                .findFirst().orElseThrow();

        assertThat(decreeV1.getSystem()).isEqualTo("JME");
        assertThat(decreeV1.getReferenceIdType()).isEqualTo("ch.admin.bit.jeap.audit.type.JmeDecreeArtifact");
        assertThat(decreeV1.getExpirationDays()).isEqualTo(30);
        assertThat(decreeV1.getEncryptionKeyId()).isNotNull();
        assertThat(decreeV1.getEncryptionKeyId().getKeyId()).isEqualTo("jme-process-archive-example-key");
        assertThat(decreeV1.getEncryptionKeyReference()).isNull();
    }

    @Test
    void getArchiveTypes_mapsEncryptionKeyReference() {
        List<ArchiveTypeInfo> types = provider.getArchiveTypes();

        ArchiveTypeInfo documentV1 = types.stream()
                .filter(t -> t.getName().equals("DecreeDocument") && t.getVersion() == 1)
                .findFirst().orElseThrow();

        assertThat(documentV1.getSystem()).isEqualTo("JME");
        assertThat(documentV1.getExpirationDays()).isEqualTo(60);
        assertThat(documentV1.getEncryptionKeyReference()).isNotNull();
        assertThat(documentV1.getEncryptionKeyReference().getSecretEnginePath()).isEqualTo("jeap/test");
        assertThat(documentV1.getEncryptionKeyReference().getKeyName()).isEqualTo("test-key");
        assertThat(documentV1.getEncryptionKeyId()).isNull();
    }
}
