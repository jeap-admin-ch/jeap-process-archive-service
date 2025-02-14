package ch.admin.bit.jeap.processarchive.registry.repository;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveTypeDefinitionRepositoryTest {

    private static String commitRef;
    private File tempDir;

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
    void copyArchiveTypeDefinitions_atCommitHash() throws Exception {
        File outputDir = new File(tempDir, "archive-types");

        ArchiveTypeDefinitionRepository.builder()
                .outputDirectory(outputDir)
                .repoUrl(gitUrl)
                .gitReference(GitReference.ofCommit(commitRef))
                .log(new DefaultLog(new ConsoleLogger()))
                .build()
                .copyArchiveTypeDefinitions(List.of("test"));

        assertTrue(new File(outputDir, "test").exists());
        assertTrue(new File(outputDir, "test/_common").exists());
        assertTrue(new File(outputDir, "test/decree/Decree.json").exists());
    }

    @Test
    void copyArchiveTypeDefinitions_atBranch() throws Exception {
        String branch = "main";
        File outputDir = new File(tempDir, "archive-types");

        ArchiveTypeDefinitionRepository.builder()
                .outputDirectory(outputDir)
                .repoUrl(gitUrl)
                .gitReference(GitReference.ofBranch(branch))
                .log(new DefaultLog(new ConsoleLogger()))
                .build()
                .copyArchiveTypeDefinitions(List.of("test"));

        assertTrue(new File(outputDir, "test").exists());
        assertTrue(new File(outputDir, "test/_common").exists());
        assertTrue(new File(outputDir, "test/decree/Decree.json").exists());
    }

    @BeforeEach
    void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName()).toFile();
    }

    @AfterEach
    void deleteTempDir() throws IOException {
        FileUtils.forceDelete(tempDir);
    }
}
