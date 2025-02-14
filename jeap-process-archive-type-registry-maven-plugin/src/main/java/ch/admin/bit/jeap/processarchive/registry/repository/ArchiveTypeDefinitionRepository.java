package ch.admin.bit.jeap.processarchive.registry.repository;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Builder
public class ArchiveTypeDefinitionRepository {

    @NonNull
    private final File outputDirectory;
    @NonNull
    private final String repoUrl;
    @NonNull
    private final GitReference gitReference;
    @NonNull
    private final Log log;

    public void copyArchiveTypeDefinitions(List<String> systemNames) throws IOException {
        File rootDir = cloneRegistryToDirectory();
        File archiveTypesDir = new File(rootDir, "archive-types");

        copySystemTypeDefinitions(archiveTypesDir, systemNames);
        copyGlobalCommonTypeDefinitions(archiveTypesDir);
    }

    private void copySystemTypeDefinitions(File archiveTypesDir, List<String> systemNames) throws IOException {
        for (String systemName : systemNames) {
            File systemDir = new File(archiveTypesDir, systemName.toLowerCase());
            copyArchiveTypeDir(systemDir);
        }
    }

    private void copyGlobalCommonTypeDefinitions(File archiveTypesDir) throws IOException {
        File globalCommonDir = new File(archiveTypesDir, "_common");
        if (globalCommonDir.exists()) {
            copyArchiveTypeDir(globalCommonDir);
        } else {
            log.info("Not copying global common archive types, no global _common ype dir found at " + globalCommonDir);
        }
    }

    private void copyArchiveTypeDir(File systemDir) throws IOException {
        var systemDestinationDir = new File(outputDirectory, systemDir.getName());
        log.info(String.format("Copy %s to %s", systemDir, systemDestinationDir));
        FileUtils.copyDirectory(systemDir, systemDestinationDir);
    }

    private File cloneRegistryToDirectory() {
        log.info(String.format("Checkout %s at %s to %s", repoUrl, gitReference, outputDirectory));

        try {
            File tempDir = Files.createTempDirectory("archivetyperegistry").toFile();
            FileUtils.forceDeleteOnExit(tempDir);

            Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir)
                    .call();
            String checkoutAt = gitReference.isCommit() ? gitReference.getCommit() :
                    Constants.DEFAULT_REMOTE_NAME + "/" + gitReference.getBranch();
            git.checkout()
                    .setName(checkoutAt)
                    .call();
            return tempDir;
        } catch (IOException | GitAPIException e) {
            throw RepositoryException.unableToClone(e);
        }
    }
}
