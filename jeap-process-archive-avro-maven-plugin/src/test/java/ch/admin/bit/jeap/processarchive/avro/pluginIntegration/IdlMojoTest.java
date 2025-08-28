package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class IdlMojoTest extends AbstractAvroMojoTest {
    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Test
    public void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) mojoRule.lookupConfiguredMojo(testDirectory, "idl");
        // act
        myMojo.execute();
        // assert
        File outputDir = new File(testDirectory, "target/generated-sources");
        final List<String> filenames = readAllFiles(outputDir);

        for (String filename : filenames) {
            System.out.println("File: " + filename);
            System.out.println(Files.readString(outputDir.toPath().resolve(filename)));
        }

        assertFalse(filenames.isEmpty());
    }
}
