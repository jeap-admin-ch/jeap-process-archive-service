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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.apache.commons.io.FileUtils.copyDirectory;

class ArchiveTypeDefinitionDownloadMojoTest extends AbstractMojoTestCase {
    private final static File RESOURCES_DIR = new File("src/test/resources/");

    private static String commitRef;
    private static String gitUrl;

    @BeforeAll
    static void createRepo() throws Exception {
        File repoDir = Files.createTempDirectory("test-git-service").toFile();
        repoDir.deleteOnExit();

        copyDirectory(new File("src/test/resources/valid"), repoDir);

        Git newRepo = Git.init()
                .setDirectory(repoDir)
                .setInitialBranch("main")
                .call();
        newRepo.add()
                .addFilepattern(".")
                .call();
        RevCommit initialRevision = newRepo.commit()
                .setMessage("Initial revision")
                .call();
        commitRef = initialRevision.getId().getName();
        newRepo.close();
        gitUrl = "file://" + repoDir.getAbsolutePath();
    }

    @Test
    void registryDownload(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "registryDownload");
        FileUtils.copyDirectory(testDir, tmpDir);
        setTestArchiveRepoUrlAndCommitRef(tmpDir);

        Mojo target = open(tmpDir);

        target.execute();

        assertTypeDefinitionsDownloaded(tmpDir);
    }

    @Test
    void registryDownload_atCommit(@TempDir File tmpDir) throws Exception {
        File testDir = new File(RESOURCES_DIR, "registryDownloadAtCommit");
        FileUtils.copyDirectory(testDir, tmpDir);
        setTestArchiveRepoUrlAndCommitRef(tmpDir);
        Mojo target = open(tmpDir);

        target.execute();

        assertTypeDefinitionsDownloaded(tmpDir);
    }

    @BeforeEach
    void setupTestDir() throws Exception {
        setUp();
    }

    private static void setTestArchiveRepoUrlAndCommitRef(File tmpDir) throws IOException {
        File testJson = new File(tmpDir, "src/main/processarchive/archive-type-definition-references.json");
        String content = FileUtils.readFileToString(testJson, "UTF-8");
        content = content.replace("TEST_REPO_URL", gitUrl);
        content = content.replace("TEST_COMMIT_REF", commitRef);
        FileUtils.writeStringToFile(testJson, content, "UTF-8");
    }

    private static void assertTypeDefinitionsDownloaded(@TempDir File tmpDir) {
        assertTrue(new File(tmpDir, "target/classes/archive-types/test").exists());
        assertTrue(new File(tmpDir, "target/classes/archive-types/test/_common").exists());
        assertTrue(new File(tmpDir, "target/classes/archive-types/test/decree/Decree.json").exists());
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
