package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypesCompilerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@MojoTest
class ArchiveTypesCompilerMojoTest extends AbstractAvroMojoTest {

    @Inject
    private MavenProject project;

    private static final String EXPECTED_V1_METADATA = """
            public static final int ARCHIVE_TYPE_VERSION = 1;
            public static final String ARCHIVE_TYPE_NAME = "Decree";
            public static final String SYSTEM_NAME = "JEAP";
            public static final String REFERENCE_ID_TYPE = "ch.admin.bit.jeap.audit.type.JmeDecreeArchive";
            public static final int EXPIRATION_DAYS = 30;
            public static final String REGISTRY_URL = "gitUrl";
            public static final String REGISTRY_BRANCH = "my-branch";
            public static final String REGISTRY_COMMIT = "cafebabe";

            public static final Map
            <String, Object> ARCHIVE_TYPE_METADATA = Map.ofEntries(
            entry("archiveTypeVersion", ARCHIVE_TYPE_VERSION),
            entry("archiveTypeName", ARCHIVE_TYPE_NAME),
            entry("systemName", SYSTEM_NAME),
            entry("referenceIdType", REFERENCE_ID_TYPE),
            entry("schema", SCHEMA$),
            entry("expirationDays", EXPIRATION_DAYS)
            ,entry("registryUrl", REGISTRY_URL)
            ,entry("registryBranch", REGISTRY_BRANCH)
            ,entry("registryCommit", REGISTRY_COMMIT)
            );

            public Map
            <String, Object> metadata() {
            return ARCHIVE_TYPE_METADATA;
            }""";
    private static final String EXPECTED_V2_METADATA = """
            public static final int ARCHIVE_TYPE_VERSION = 2;
            public static final String ARCHIVE_TYPE_NAME = "Decree";
            public static final String SYSTEM_NAME = "JEAP";
            public static final String REFERENCE_ID_TYPE = "ch.admin.bit.jeap.audit.type.JmeDecreeArchive";
            public static final int EXPIRATION_DAYS = 30;
            public static final String REGISTRY_URL = "gitUrl";
            public static final String REGISTRY_BRANCH = "my-branch";
            public static final String REGISTRY_COMMIT = "cafebabe";
            public static final String COMPATIBILITY_MODE = "BACKWARD";

            public static final Map
            <String, Object> ARCHIVE_TYPE_METADATA = Map.ofEntries(
            entry("archiveTypeVersion", ARCHIVE_TYPE_VERSION),
            entry("archiveTypeName", ARCHIVE_TYPE_NAME),
            entry("systemName", SYSTEM_NAME),
            entry("referenceIdType", REFERENCE_ID_TYPE),
            entry("schema", SCHEMA$),
            entry("expirationDays", EXPIRATION_DAYS)
            ,entry("registryUrl", REGISTRY_URL)
            ,entry("registryBranch", REGISTRY_BRANCH)
            ,entry("registryCommit", REGISTRY_COMMIT)
            ,entry("compatibilityMode", COMPATIBILITY_MODE)
            );

            public Map
            <String, Object> metadata() {
            return ARCHIVE_TYPE_METADATA;
            }""";

    private File setupTestDirectory(Path tempDir, String resourcePath) throws Exception {
        File testDirectory = syncToTempDirectory(resourcePath, tempDir);
        FileUtils.copyDirectory(Paths.get(Paths.get("").toAbsolutePath().getParent().toString(), ".git").toFile(), Paths.get(testDirectory.getAbsolutePath(), ".git").toFile());
        return testDirectory;
    }

    private void pointToTempDir(ArchiveTypesCompilerMojo mojo, File testDirectory) throws IllegalAccessException {
        setVariableValueToObject(project, "basedir", testDirectory);
        project.getBuild().setDirectory(new File(testDirectory, "target").getAbsolutePath());
        setVariableValueToObject(mojo, "sourceDirectory", new File(testDirectory, "archive-types"));
        setVariableValueToObject(mojo, "outputDirectory", new File(testDirectory, "target/generated-sources"));
    }

    @Test
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_generateAllArchiveTypes_allArchiveTypesGenerated(ArchiveTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-registry");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        myMojo.execute();

        List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());

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
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry-custom-pom/pom.xml")
    void execute_generateAllArchiveTypes_customPomTemplate(ArchiveTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-registry-custom-pom");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("my-branch");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");
        myMojo.setPomTemplateFile(new File(testDirectory, "archivetype-template.pom.xml"));

        myMojo.execute();

        List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());

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
    @InjectMojo(goal = "compile-archive-types", pom = "src/test/resources/sample-registry/pom.xml")
    void execute_generateAllArchiveTypes_correctClassifierForMasterBranch(ArchiveTypesCompilerMojo myMojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = setupTestDirectory(tempDir, "src/test/resources/sample-registry");
        pointToTempDir(myMojo, testDirectory);

        myMojo.setGenerateAllArchiveTypes(true);
        myMojo.setCurrentBranch("master");
        myMojo.setCommitId("cafebabe");
        myMojo.setGitUrl("gitUrl");
        myMojo.setFetchTags(false);
        myMojo.setGroupIdPrefix("ch.bit.admin.test");

        myMojo.execute();

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
