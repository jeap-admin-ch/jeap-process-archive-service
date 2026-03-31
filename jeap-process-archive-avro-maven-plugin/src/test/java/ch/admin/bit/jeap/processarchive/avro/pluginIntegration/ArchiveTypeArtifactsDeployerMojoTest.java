package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchiveTypeArtifactsDeployerMojoTest {

    @RegisterExtension
    AvroMojoTestSupport mojoSupport = new AvroMojoTestSupport();

    @TempDir
    Path tempDir;

    @Test
    void execute_noSourcesDirectory_nothingDeployed() throws Exception {
        File testDirectory = AvroMojoTestSupport.copyToTempDir("src/test/resources/sample-archive-type-artifacts-deploy", tempDir);
        File targetDirectory = new File(testDirectory, "target/generated-sources");
        Mojo myMojo = mojoSupport.lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");

        myMojo.execute();

        assertFalse(Files.exists(Path.of(targetDirectory.getAbsolutePath())));
    }

    @Test
    void execute_sourcesDirectoryExists_projectsDeployed() throws Exception {
        final File testDirectory = AvroMojoTestSupport.copyToTempDir("src/test/resources/sample-archive-type-artifacts-deploy", tempDir);
        final File targetDirectory = new File(testDirectory, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), Paths.get(targetDirectory.getAbsolutePath()).toFile());
        ArchiveTypeArtifactsDeployerMojo myMojo = (ArchiveTypeArtifactsDeployerMojo) mojoSupport.lookupConfiguredMojo(testDirectory, "deploy-archive-type-artifacts");
        Invoker invoker = mock(Invoker.class);
        InvocationResult resultMock = mock(InvocationResult.class);
        when(invoker.execute(any())).thenReturn(resultMock);

        myMojo.setInvokerFactory(() -> invoker);

        myMojo.execute();

        verify(invoker).execute(any());
    }
}
