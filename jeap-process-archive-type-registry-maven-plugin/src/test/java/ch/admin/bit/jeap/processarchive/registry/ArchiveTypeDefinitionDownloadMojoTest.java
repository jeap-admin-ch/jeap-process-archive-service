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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Objects;

class ArchiveTypeDefinitionDownloadMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    @Test
    void registryDownload(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "registryDownload");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);

        target.execute();

        assertTypeDefinitionsDownloaded(tmpDir);
    }

    @Test
    void registryDownload_atCommit(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "registryDownloadAtCommit");
        FileUtils.copyDirectory(testDir, tmpDir);
        Mojo target = open(tmpDir);

        target.execute();

        assertTypeDefinitionsDownloaded(tmpDir);
    }

    private static void assertTypeDefinitionsDownloaded(@TempDir File tmpDir) {
        assertTrue(new File(tmpDir, "target/classes/archive-types/jme").exists());
        assertTrue(new File(tmpDir, "target/classes/archive-types/jme/_common").exists());
        assertTrue(new File(tmpDir, "target/classes/archive-types/jme/decree/Decree.json").exists());
    }

    @BeforeAll
    static void setupTruststore() {
        String filePath = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource("truststore.jks")).getFile();
        System.setProperty("javax.net.ssl.trustStore", filePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    private Mojo open(File basedir) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution("download-archive-type-definitions");
        return lookupConfiguredMojo(session, execution);
    }
}
