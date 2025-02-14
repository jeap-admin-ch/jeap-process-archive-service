package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAvroMojoTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    File syncWithNewTempDirectory(final String srcTestDirectory) throws IOException {
        File tmpTestDir = temporaryFolder.newFolder("test");
        final File testPomDir = new File(srcTestDirectory);
        FileUtils.copyDirectory(testPomDir, tmpTestDir);
        return tmpTestDir;
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
