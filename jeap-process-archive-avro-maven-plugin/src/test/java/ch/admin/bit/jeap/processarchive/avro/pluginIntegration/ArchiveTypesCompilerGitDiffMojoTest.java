package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypesCompilerMojo;
import ch.admin.bit.jeap.processarchive.avro.pluginIntegration.repo.TestRegistryRepo;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
@ExtendWith(SystemStubsExtension.class)
class ArchiveTypesCompilerGitDiffMojoTest {

    @Inject
    private MavenProject project;

    private TestRegistryRepo testRepo;

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    void createTestRepo() throws Exception {
        testRepo = TestRegistryRepo.testRepoWithTwoCommitsAddingArchiveTypeV1AndV2();
    }

    @AfterEach
    void deleteTestRepo() throws IOException {
        testRepo.delete();
    }

    private void configureMojo(ArchiveTypesCompilerMojo mojo) throws Exception {
        File repoDir = testRepo.repoDir().toFile();
        setVariableValueToObject(project, "basedir", repoDir);
        project.getBuild().setDirectory(new File(repoDir, "target").getAbsolutePath());
        setVariableValueToObject(mojo, "sourceDirectory", new File(repoDir, "archive-types"));
        setVariableValueToObject(mojo, "outputDirectory", new File(repoDir, "target/generated-sources"));

        mojo.setGenerateAllArchiveTypes(false);
        mojo.setCurrentBranch("master");
        mojo.setTrunkBranchName("master");
        mojo.setCommitId(testRepo.commits().get(testRepo.commits().size() - 1).name());
        mojo.setGitUrl(testRepo.url());
        mojo.setGroupIdPrefix("ch.bit.admin.test");
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_diff_noNewArchiveTypes(ArchiveTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(0);
        myMojo.execute();

        assertFileDoesNotExist("target/generated-sources/activ");
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_diff_singleNewArchiveType_noOldDescriptor(ArchiveTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(1);
        myMojo.execute();

        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileExists("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileDoesNotExist("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_diff_singleNewArchiveType_existingTypeShouldNotBeGenerated(ArchiveTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();

        testRepo.checkoutCommit(2);
        myMojo.execute();

        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileDoesNotExist("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_diff_twoNewArchiveTypes(ArchiveTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        testRepo.checkoutCommit(2);
        myMojo.execute();

        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileExists("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_diff_withGitToken(ArchiveTypesCompilerMojo myMojo) throws Exception {
        configureMojo(myMojo);
        environmentVariables.set("ARCHIVE_TYPE_REPO_GIT_TOKEN", "test-token-value");
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();
        testRepo.checkoutCommit(2);

        myMojo.execute();

        assertFileDoesNotExist("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    private void assertFileExists(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertTrue(Files.exists(path), "file " + file + " exists");
    }

    private void assertFileDoesNotExist(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertFalse(Files.exists(path), "file " + file + " does not exist");
    }
}
