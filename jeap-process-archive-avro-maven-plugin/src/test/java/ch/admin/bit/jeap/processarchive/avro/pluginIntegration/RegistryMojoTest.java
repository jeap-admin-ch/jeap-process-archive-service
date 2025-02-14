package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.RegistryMojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertFalse;

public class RegistryMojoTest extends AbstractAvroMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @BeforeClass
    public static void setUp() {
        String filePath = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader().getResource("truststore.jks")).getFile();
        System.setProperty("javax.net.ssl.trustStore", filePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @Test
    public void execute() throws Exception {
        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-registry");
        final RegistryMojo myMojo = (RegistryMojo) mojoRule.lookupConfiguredMojo(testDirectory, "registry");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
    }
}
