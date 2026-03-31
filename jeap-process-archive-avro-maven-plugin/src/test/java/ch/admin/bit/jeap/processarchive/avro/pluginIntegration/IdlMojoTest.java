package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.IDLProtocolMojo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class IdlMojoTest extends AbstractAvroMojoTest {

    @Test
    void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-idl");
        final IDLProtocolMojo myMojo = (IDLProtocolMojo) lookupConfiguredMojo(testDirectory, "idl");
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
