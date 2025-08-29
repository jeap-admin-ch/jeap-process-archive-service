package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypesCompilerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArchiveTypesCompilerMojoTest extends AbstractAvroMojoTest {

    private static final String EXPECTED_V1_METADATA = """
            public static final String ARCHIVE_TYPE_VERSION = "1";
            public static final String ARCHIVE_TYPE_NAME = "Decree";
            public static final String SYSTEM_NAME = "JEAP";
            public static final String REFERENCE_ID_TYPE = "ch.admin.bit.jeap.audit.type.JmeDecreeArchive";
            public static final int EXPIRATION_DAYS = 30;
            public static final String REGISTRY_URL = "gitUrl";
            public static final String REGISTRY_BRANCH = "my-branch";
            public static final String REGISTRY_COMMIT = "cafebabe";
            
            public static final Map
            <String, Object> ARCHIVE_TYPE_METADATA = Map.of(
            "archiveTypeVersion", ARCHIVE_TYPE_VERSION,
            "archiveTypeName", ARCHIVE_TYPE_NAME,
            "systemName", SYSTEM_NAME,
            "referenceIdType", REFERENCE_ID_TYPE,
            "expirationDays", EXPIRATION_DAYS
            ,"registryUrl", REGISTRY_URL
            ,"registryBranch", REGISTRY_BRANCH
            ,"registryCommit", REGISTRY_COMMIT
            );
            
            public Map
            <String, Object> metadata() {
            return ARCHIVE_TYPE_METADATA;
            }""";
    private static final String EXPECTED_V2_METADATA = """
            public static final String ARCHIVE_TYPE_VERSION = "2";
            public static final String ARCHIVE_TYPE_NAME = "Decree";
            public static final String SYSTEM_NAME = "JEAP";
            public static final String REFERENCE_ID_TYPE = "ch.admin.bit.jeap.audit.type.JmeDecreeArchive";
            public static final int EXPIRATION_DAYS = 30;
            public static final String REGISTRY_URL = "gitUrl";
            public static final String REGISTRY_BRANCH = "my-branch";
            public static final String REGISTRY_COMMIT = "cafebabe";
            public static final String COMPATIBILITY_MODE = "BACKWARD";
            
            public static final Map
            <String, Object> ARCHIVE_TYPE_METADATA = Map.of(
            "archiveTypeVersion", ARCHIVE_TYPE_VERSION,
            "archiveTypeName", ARCHIVE_TYPE_NAME,
            "systemName", SYSTEM_NAME,
            "referenceIdType", REFERENCE_ID_TYPE,
            "expirationDays", EXPIRATION_DAYS
            ,"registryUrl", REGISTRY_URL
            ,"registryBranch", REGISTRY_BRANCH
            ,"registryCommit", REGISTRY_COMMIT
            ,"compatibilityMode", COMPATIBILITY_MODE
            );
            
            public Map
            <String, Object> metadata() {
            return ARCHIVE_TYPE_METADATA;
            }""";

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute_generateAllArchiveTypes_allArchiveTypesGenerated() throws Exception {
        // arrange
        File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-registry");
        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        ArchiveTypesCompilerMojo myMojo = (ArchiveTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-archive-types");

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        // act
        myMojo.execute();

        // assert
        List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());

        assertEquals(3, filenames.stream().filter(f -> f.endsWith("/pom.xml")).count());

        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jeap/_common", 2);

        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1", 2);
        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2", 2);

        String decreeContentV1 = Files.readString(new File(testDirectory, "target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1/Decree.java").toPath());
        String decreeContentV2 = Files.readString(new File(testDirectory, "target/generated-sources/jeap/Decree/2/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v2/Decree.java").toPath());
        assertThat(decreeContentV1)
                .containsIgnoringWhitespaces(EXPECTED_V1_METADATA);
        assertThat(decreeContentV2)
                .containsIgnoringWhitespaces(EXPECTED_V2_METADATA);

        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/1/pom.xml", "<version>1-my-branch-SNAPSHOT</version>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/2/pom.xml", "<version>2-my-branch-SNAPSHOT</version>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/1/pom.xml", "<classifier>1-my-branch-SNAPSHOT</classifier>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/2/pom.xml", "<classifier>2-my-branch-SNAPSHOT</classifier>");
    }

    @Test
    public void execute_generateAllArchiveTypes_customPomTemplate() throws Exception {
        // arrange
        File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-registry-custom-pom");

        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        ArchiveTypesCompilerMojo myMojo = (ArchiveTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-archive-types");

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
        myMojo.setPomTemplateFile(new File(testDirectory, "archivetype-template.pom.xml"));

        // act
        myMojo.execute();

        // assert
        List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        Assert.assertFalse(filenames.isEmpty());

        filenames.forEach(System.out::println);

        assertThat(filenames.stream().filter(f -> f.endsWith("/pom.xml")))
                .hasSize(1);
        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1", 2);
        assertFileContains(testDirectory,
                "target/generated-sources/jeap/Decree/1/pom.xml",
                "<version>1-my-branch-SNAPSHOT</version>");
        assertFileContains(testDirectory,
                "target/generated-sources/jeap/Decree/1/pom.xml",
                "<custom.property>value</custom.property>");
    }

    @Test
    public void execute_generateAllArchiveTypes_correctClassifierForMasterBranch() throws Exception {
        // arrange
        File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-registry");
        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());

        ArchiveTypesCompilerMojo myMojo = (ArchiveTypesCompilerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "compile-archive-types");

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("master");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false); // not a valid git url provided, so we cannot fetch tags
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        // act
        myMojo.execute();

        // assert
        assertContentOfCreatedSourceDirectory(testDirectory,
                "target/generated-sources/jeap/Decree/1/src/main/java/ch/admin/bit/jeap/processarchive/test/decree/v1", 2);
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/1/pom.xml", "<version>1</version>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/2/pom.xml", "<version>2</version>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/1/pom.xml", "<classifier>1</classifier>");
        assertFileContains(testDirectory, "target/generated-sources/jeap/Decree/2/pom.xml", "<classifier>2</classifier>");
    }

    private void assertContentOfCreatedSourceDirectory(File testDirectory, String child, int count) {
        final File directory = new File(testDirectory, child);
        assertTrue(directory.exists());
        assertEquals(count, Objects.requireNonNull(directory.listFiles()).length);
    }

    private void assertFileContains(File baseDirectory, String filename, String text) throws IOException {
        String fileContent = Files.readString(new File(baseDirectory, filename).toPath());
        assertThat(fileContent)
                .contains(text);
    }
}
