package ch.admin.bit.jeap.processarchive.registry;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.Mojo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MojoTest
class ArchiveTypeRegistryVerifierMojoTest {

    @Test
    @InjectMojo(goal = "registry")
    @Basedir("src/test/resources/dirMissing")
    void invalid(Mojo target) {
        assertThatThrownBy(target::execute)
                .hasMessageFindingMatch("File .* does not exist");
    }

    @Test
    @InjectMojo(goal = "registry")
    @Basedir("src/test/resources/valid")
    void valid(Mojo target) {
        assertDoesNotThrow(target::execute);
    }

    @Test
    @InjectMojo(goal = "registry")
    @Basedir("src/test/resources/incompatibleV2")
    void incompatibleV2(Mojo target) {
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Schemas Decree version 2 and 1 are not backward compatible");
    }

    @Test
    @InjectMojo(goal = "registry")
    @Basedir("src/test/resources/collidingNamespaces")
    void collidingNamespaces(Mojo target) {
        assertThatThrownBy(target::execute)
                .hasMessageContaining("do not use a separate namespace per version");
    }
}
