package ch.admin.bit.jeap.processarchive.config.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigArchiveTypeRepositoryTest {

    private ConfigArchiveTypeRepository repository;

    @BeforeEach
    void setUp() {
        ProcessArchiveRegistryProperties properties = new ProcessArchiveRegistryProperties();

        ArchiveTypeDefinition type1 = new ArchiveTypeDefinition();
        type1.setArchiveType("MyType");
        type1.setVersion(1);
        type1.setSystem("Jme");
        type1.setExpirationDays(90);
        type1.setReferenceIdType("ch.admin.dept.FooBar");
        type1.setEncryptionKey("my-key");

        ArchiveTypeDefinition type2 = new ArchiveTypeDefinition();
        type2.setArchiveType("OtherType");
        type2.setVersion(1);
        type2.setSystem("Jme");
        type2.setExpirationDays(365);
        type2.setReferenceIdType("ch.admin.dept.OtherRef");

        properties.setTypes(List.of(type1, type2));

        ArchiveCryptoService cryptoService = mock(ArchiveCryptoService.class);
        when(cryptoService.encrypt(any(), any())).thenReturn(new byte[0]);

        repository = new ConfigArchiveTypeRepository(properties, cryptoService);
        repository.initialize();
    }

    @Test
    void getArchiveTypes_returnsAllTypes() {
        List<ArchiveTypeInfo> types = repository.getArchiveTypes();

        assertThat(types).hasSize(2);
    }

    @Test
    void getArchiveTypes_returnsCorrectMetadata() {
        List<ArchiveTypeInfo> types = repository.getArchiveTypes();

        ArchiveTypeInfo myType = types.stream()
                .filter(t -> t.getName().equals("MyType"))
                .findFirst().orElseThrow();

        assertThat(myType.getSystem()).isEqualTo("Jme");
        assertThat(myType.getName()).isEqualTo("MyType");
        assertThat(myType.getVersion()).isEqualTo(1);
        assertThat(myType.getReferenceIdType()).isEqualTo("ch.admin.dept.FooBar");
        assertThat(myType.getExpirationDays()).isEqualTo(90);
        assertThat(myType.getEncryptionKeyId()).isNotNull();
        assertThat(myType.getEncryptionKeyId().getKeyId()).isEqualTo("my-key");
    }

    @Test
    void getArchiveTypes_withoutEncryption() {
        List<ArchiveTypeInfo> types = repository.getArchiveTypes();

        ArchiveTypeInfo otherType = types.stream()
                .filter(t -> t.getName().equals("OtherType"))
                .findFirst().orElseThrow();

        assertThat(otherType.getSystem()).isEqualTo("Jme");
        assertThat(otherType.getName()).isEqualTo("OtherType");
        assertThat(otherType.getExpirationDays()).isEqualTo(365);
        assertThat(otherType.getEncryptionKeyId()).isNull();
        assertThat(otherType.getEncryptionKeyReference()).isNull();
    }
}
