package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ArchiveTypeArtifactsDeployerMojoTest extends AbstractAvroMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute_noSourcesDirectory_nothingDeployed() throws Exception {
        // arrange
        File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy");
        File targetDirectory = new File(testDirectory, "target/generated-sources");
        Mojo myMojo = mojoRule.lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");

        // act
        myMojo.execute();

        // assert
        assertFalse(Files.exists(Path.of(targetDirectory.getAbsolutePath())));
    }

    @Test
    public void execute_sourcesDirectoryExists_projectsDeployed() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy");
        final File targetDirectory = new File(testDirectory, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), Paths.get(targetDirectory.getAbsolutePath()).toFile());
        ArchiveTypeArtifactsDeployerMojo myMojo = (ArchiveTypeArtifactsDeployerMojo) mojoRule.lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");

        ExecutorService executorService = mock(ExecutorService.class);
        Future<Object> future = mock(Future.class);
        myMojo.setExecutorService(executorService);

        when(executorService.submit((Callable<Object>) any())).thenReturn(future);

        // act
        myMojo.execute();

        verify(executorService).submit((Callable<?>) any());
    }
}