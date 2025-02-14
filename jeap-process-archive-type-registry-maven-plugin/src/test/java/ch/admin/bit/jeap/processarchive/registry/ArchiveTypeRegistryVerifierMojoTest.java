package ch.admin.bit.jeap.processarchive.registry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ArchiveTypeRegistryVerifierMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    @Test
    void invalid(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "dirMissing");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageFindingMatch("File .* does not exist");
    }

    @Test
    void valid(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "valid");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertDoesNotThrow(target::execute);
    }

    @Test
    void incompatibleV2(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "incompatibleV2");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("Schemas Decree version 2 and 1 are not backward compatible");
    }

    @Test
    void collidingNamespaces(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "collidingNamespaces");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);
        assertThatThrownBy(target::execute)
                .hasMessageContaining("do not use a separate namespace per version");
    }

    private Mojo open(File basedir) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("registry");
        return lookupConfiguredMojo(session, execution);
    }
}
