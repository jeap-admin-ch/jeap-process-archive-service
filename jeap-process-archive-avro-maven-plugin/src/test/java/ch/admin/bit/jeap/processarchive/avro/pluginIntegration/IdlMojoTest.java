package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.IDLProtocolMojo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@MojoTest
class IdlMojoTest {

    @AfterEach
    void cleanup() throws IOException {
        File targetDir = new File("src/test/resources/sample-idl/target");
        if (targetDir.exists()) {
            FileUtils.deleteDirectory(targetDir);
        }
    }

    @Test
    @InjectMojo(goal = "idl")
    @Basedir("src/test/resources/sample-idl")
    void execute(IDLProtocolMojo mojo) throws Exception {
        mojo.execute();

        File outputDir = new File("src/test/resources/sample-idl/target/generated-sources");
        List<String> filenames = AvroMojoTestSupport.readAllFiles(outputDir);
        assertFalse(filenames.isEmpty());
    }
}
