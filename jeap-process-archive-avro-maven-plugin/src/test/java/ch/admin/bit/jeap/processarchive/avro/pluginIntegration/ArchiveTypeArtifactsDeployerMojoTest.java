package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchiveTypeArtifactsDeployerMojoTest extends AbstractAvroMojoTest {

    @Test
    void execute_noSourcesDirectory_nothingDeployed() throws Exception {
        // arrange
        File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy");
        File targetDirectory = new File(testDirectory, "target/generated-sources");
        Mojo myMojo = lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");

        // act
        myMojo.execute();

        // assert
        Assertions.assertFalse(Files.exists(Path.of(targetDirectory.getAbsolutePath())));
    }

    @Test
    void execute_sourcesDirectoryExists_projectsDeployed() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy");
        final File targetDirectory = new File(testDirectory, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), Paths.get(targetDirectory.getAbsolutePath()).toFile());
        ArchiveTypeArtifactsDeployerMojo myMojo = (ArchiveTypeArtifactsDeployerMojo) lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");
        Invoker invoker = mock(Invoker.class);
        InvocationResult resultMock = mock(InvocationResult.class);
        when(invoker.execute(any())).thenReturn(resultMock);

        myMojo.setInvokerFactory(() -> invoker);

        // act
        myMojo.execute();

        // assert
        verify(invoker).execute(any());
    }
}
