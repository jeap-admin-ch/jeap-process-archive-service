package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MojoTest
class IdlMojoTest extends AbstractAvroMojoTest {

    @Inject
    private MavenProject project;

    @Test
    @InjectMojo(goal = "idl", pom = "src/test/resources/sample-idl/pom.xml")
    void execute(IDLProtocolMojo mojo, @TempDir Path tempDir) throws Exception {
        File testDirectory = syncToTempDirectory("src/test/resources/sample-idl", tempDir);
        setVariableValueToObject(project, "basedir", testDirectory);
        project.getBuild().setDirectory(new File(testDirectory, "target").getAbsolutePath());
        setVariableValueToObject(mojo, "sourceDirectory", new File(testDirectory, "processarchive"));
        setVariableValueToObject(mojo, "outputDirectory", new File(testDirectory, "target/generated-sources"));

        mojo.execute();

        File outputDir = new File(testDirectory, "target/generated-sources");
        List<String> filenames = readAllFiles(outputDir);
        assertFalse(filenames.isEmpty());
    }
}
