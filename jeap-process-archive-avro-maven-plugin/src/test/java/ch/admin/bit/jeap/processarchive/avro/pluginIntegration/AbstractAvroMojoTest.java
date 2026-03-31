package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAvroMojoTest extends AbstractMojoTestCase {
    @TempDir
    Path temporaryFolder;

    @BeforeEach
    void initMojoTestCase() throws Exception {
        setUp();
    }

    protected Mojo lookupConfiguredMojo(File basedir, String goal) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = getContainer().lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        return lookupConfiguredMojo(project, goal);
    }

    File syncWithNewTempDirectory(final String srcTestDirectory) throws IOException {
        Path tmpTestDir = Files.createDirectory(temporaryFolder.resolve("test"));
        final File testPomDir = new File(srcTestDirectory);
        FileUtils.copyDirectory(testPomDir, tmpTestDir.toFile());
        return tmpTestDir.toFile();
    }

    List<String> readAllFiles(File testPomDir) throws IOException {
        try (Stream<Path> stream = Files.walk(testPomDir.toPath(), Integer.MAX_VALUE)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
