package ch.admin.bit.jeap.processarchive.registry.repository;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveTypeDefinitionRepositoryTest {

    private File tempDir;

    @Test
    void copyArchiveTypeDefinitions_atCommitHash() throws Exception {
        String gitUrl = "https://bitbucket.bit.admin.ch/scm/bit_jme/jme-archive-type-registry.git";
        String commitReference = "380a6cf2637";
        File outputDir = new File(tempDir, "archive-types");

        ArchiveTypeDefinitionRepository.builder()
                .outputDirectory(outputDir)
                .repoUrl(gitUrl)
                .gitReference(GitReference.ofCommit(commitReference))
                .log(new DefaultLog(new ConsoleLogger()))
                .build()
                .copyArchiveTypeDefinitions(List.of("jme"));

        assertTrue(new File(outputDir, "jme").exists());
        assertTrue(new File(outputDir, "jme/_common").exists());
        assertTrue(new File(outputDir, "jme/decree/Decree.json").exists());
    }

    @Test
    void copyArchiveTypeDefinitions_atBranch() throws Exception {
        String gitUrl = "https://bitbucket.bit.admin.ch/scm/bit_jme/jme-archive-type-registry.git";
        String branch = "master";
        File outputDir = new File(tempDir, "archive-types");

        ArchiveTypeDefinitionRepository.builder()
                .outputDirectory(outputDir)
                .repoUrl(gitUrl)
                .gitReference(GitReference.ofBranch(branch))
                .log(new DefaultLog(new ConsoleLogger()))
                .build()
                .copyArchiveTypeDefinitions(List.of("jme"));

        assertTrue(new File(outputDir, "jme").exists());
        assertTrue(new File(outputDir, "jme/_common").exists());
        assertTrue(new File(outputDir, "jme/decree/Decree.json").exists());
    }

    @BeforeEach
    void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
    }

    @BeforeAll
    static void setupTruststore() {
        String filePath = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource("truststore.jks")).getFile();
        System.setProperty("javax.net.ssl.trustStore", filePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @AfterEach
    void deleteTempDir() throws IOException {
        FileUtils.forceDelete(tempDir);
    }
}
