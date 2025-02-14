package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import ch.admin.bit.jeap.processarchive.avro.plugin.mojo.RegistryMojo;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;

public class RegistryMojoTest extends AbstractAvroMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(12324);

    @Test
    public void execute() throws Exception {
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/decree/Decree.json?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/decree/Decree.json"))));
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/decree/Decree_v1.avdl?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/decree/Decree_v1.avdl"))));
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/_common?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("1\tch.admin.bit.jeap.processarchive.test.DecreeReference.avdl")));
        stubFor(get(urlEqualTo("/raw/archive-types/jeap/_common/ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody(loadResource("/test-registry/jeap/_common/ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"))));
        stubFor(get(urlEqualTo("/raw/archive-types/_common?at=refs%2Fheads%2Fmaster"))
                .willReturn(aResponse()
                        .withStatus(404)));

        // arrange
        final File testDirectory = syncWithNewTempDirectory("src/test/resources/sample-registry");

        final RegistryMojo myMojo = (RegistryMojo) mojoRule.lookupConfiguredMojo(testDirectory, "registry");
        // act
        myMojo.execute();
        // assert
        final List<String> filenames = readAllFiles(new File(testDirectory, "target/generated-sources"));
        assertFalse(filenames.isEmpty());
    }

    private static String loadResource(String path) {
        try {
            return new String(RegistryMojoTest.class.getResourceAsStream(path).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
