package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypesCompilerMojo;
import ch.admin.bit.jeap.processarchive.avro.pluginIntegration.repo.TestRegistryRepo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArchiveTypesCompilerGitDiffMojoTest extends AbstractAvroMojoTest {

    private TestRegistryRepo testRepo;

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public EnvironmentVariablesRule environmentVariables = new EnvironmentVariablesRule();

    private ArchiveTypesCompilerMojo myMojo;

    @Before
    public void createTestRepo() throws Exception {
        testRepo = TestRegistryRepo.testRepoWithTwoCommitsAddingArchiveTypeV1AndV2();

        myMojo = (ArchiveTypesCompilerMojo) mojoRule.lookupConfiguredMojo(
                testRepo.repoDir().toFile(), "compile-archive-types");

        myMojo.setGenerateAllArchiveTypes(false);
        myMojo.setCurrentBranch("master");
        myMojo.setTrunkBranchName("master");
        myMojo.setCommitId(testRepo.commits().get(testRepo.commits().size() - 1).name());
        myMojo.setGitUrl(testRepo.url());
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
    }

    @After
    public void deleteTestRepo() throws IOException {
        testRepo.delete();
    }

    @Test
    public void execute_diff_noNewArchiveTypes() throws Exception {
        // act
        testRepo.checkoutCommit(0);
        myMojo.execute();

        // assert nothing is generated
        assertFileDoesNotExist("target/generated-sources/activ");
    }

    @Test
    public void execute_diff_singleNewArchiveType_noOldDescriptor() throws Exception {
        // act
        testRepo.checkoutCommit(1);
        myMojo.execute();

        // assert v1 is generated
        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileExists("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileDoesNotExist("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    public void execute_diff_singleNewArchiveType_existingTypeShouldNotBeGenerated() throws Exception {
        // set last tag on commit 1
        // commit 2 will add v2 of the event compared to commit 1
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();

        // act
        testRepo.checkoutCommit(2);
        myMojo.execute();

        // assert v1 not regenerated, v2 is generated
        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileDoesNotExist("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    public void execute_diff_twoNewArchiveTypes() throws Exception {
        // act
        testRepo.checkoutCommit(2);
        myMojo.execute();

        // assert v1 and v2 are generated
        assertFileExists("target/generated-sources/jeap/_common/src/main/java/ch/admin/bit/jeap/processarchive/test/DecreeReference.java");
        assertFileExists("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    @Test
    public void execute_diff_withGitToken() throws Exception {
        // Set the environment variable for the git token to make the plugin fetch tags using JGit and a token.
        // All other tests use the system git to fetch the tags as they don't set the token.
        environmentVariables.set("ARCHIVE_TYPE_REPO_GIT_TOKEN", "test-token-value");
        // set last tag on commit 1
        testRepo.repo().tag()
                .setObjectId(testRepo.commits().get(1))
                .setName("v1.0.0")
                .call();
        // checkout commit 2, which adds type v2
        testRepo.checkoutCommit(2);

        // act
        myMojo.execute();

        // assert v2 is generated
        assertFileDoesNotExist("target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java");
        assertFileExists("target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java");
    }

    private void assertFileExists(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertTrue("file " + file + " exists", Files.exists(path));
    }

    private void assertFileDoesNotExist(String file) {
        Path path = testRepo.repoDir().resolve(file);
        assertFalse("file " + file + " does not exist", Files.exists(path));
    }
}
