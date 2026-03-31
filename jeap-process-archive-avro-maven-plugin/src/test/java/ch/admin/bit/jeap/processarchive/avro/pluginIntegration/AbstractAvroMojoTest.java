package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractAvroMojoTest {

    File syncToTempDirectory(String srcTestDirectory, Path tempDir) throws IOException {
        Path tmpTestDir = Files.createDirectory(tempDir.resolve("test"));
        FileUtils.copyDirectory(new File(srcTestDirectory), tmpTestDir.toFile());
        return tmpTestDir.toFile();
    }

    List<String> readAllFiles(File dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir.toPath(), Integer.MAX_VALUE)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}
