package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.ArchiveTypeArtifactsDeployerMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MojoTest
class ArchiveTypeArtifactsDeployerMojoTest extends AbstractAvroMojoTest {

    @Inject
    private MavenProject project;

    private void pointToTempDir(Mojo mojo, File testDirectory) throws IllegalAccessException {
        setVariableValueToObject(project, "basedir", testDirectory);
        project.getBuild().setDirectory(new File(testDirectory, "target").getAbsolutePath());
        setVariableValueToObject(mojo, "sourcesDirectory", new File(testDirectory, "target/generated-sources"));
    }

    @Test
    @InjectMojo(goal = "deploy-archive-type-artifacts", pom = "src/test/resources/sample-archive-type-artifacts-deploy/pom.xml")
    void execute_noSourcesDirectory_nothingDeployed(ArchiveTypeArtifactsDeployerMojo myMojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = syncToTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy", tempDir);
        pointToTempDir(myMojo, testDirectory);

        myMojo.execute();

        assertFalse(Files.exists(new File(testDirectory, "target/generated-sources").toPath()));
    }

    @Test
    @InjectMojo(goal = "deploy-archive-type-artifacts", pom = "src/test/resources/sample-archive-type-artifacts-deploy/pom.xml")
    void execute_sourcesDirectoryExists_projectsDeployed(ArchiveTypeArtifactsDeployerMojo myMojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = syncToTempDirectory("src/test/resources/sample-archive-type-artifacts-deploy", tempDir);
        File targetDirectory = new File(testDirectory, "target/generated-sources");
        Files.createDirectories(targetDirectory.toPath());
        FileUtils.copyDirectory(Paths.get("src/test/resources/sample-project").toFile(), targetDirectory);
        pointToTempDir(myMojo, testDirectory);

        Invoker invoker = mock(Invoker.class);
        InvocationResult resultMock = mock(InvocationResult.class);
        when(invoker.execute(any())).thenReturn(resultMock);
        myMojo.setInvokerFactory(() -> invoker);

        myMojo.execute();

        verify(invoker).execute(any());
    }
}
