package ch.admin.bit.jeap.processarchive.config.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ProcessArchiveRegistryPropertiesTest.Config.class)
@ActiveProfiles("config-registry-test")
class ProcessArchiveRegistryPropertiesTest {

    @EnableConfigurationProperties(ProcessArchiveRegistryProperties.class)
    static class Config {
    }

    @Autowired
    private ProcessArchiveRegistryProperties properties;

    @Test
    void propertiesAreCorrectlyBound() {
        assertThat(properties.getTypes()).hasSize(2);

        ArchiveTypeDefinition type0 = properties.getTypes().getFirst();
        assertThat(type0.getArchiveType()).isEqualTo("MyType");
        assertThat(type0.getVersion()).isEqualTo(1);
        assertThat(type0.getSystem()).isEqualTo("Jme");
        assertThat(type0.getExpirationDays()).isEqualTo(90);
        assertThat(type0.getReferenceIdType()).isEqualTo("ch.admin.dept.FooBar");
        assertThat(type0.getEncryptionKey()).isEqualTo("my-key");

        ArchiveTypeDefinition type1 = properties.getTypes().get(1);
        assertThat(type1.getArchiveType()).isEqualTo("OtherType");
        assertThat(type1.getVersion()).isEqualTo(2);
        assertThat(type1.getSystem()).isEqualTo("Jme");
        assertThat(type1.getExpirationDays()).isEqualTo(365);
        assertThat(type1.getReferenceIdType()).isEqualTo("ch.admin.dept.OtherRef");
        assertThat(type1.getEncryptionKey()).isNull();
    }
}
