package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;

abstract class AbstractAvroMojoTest {

    MavenProject ensureProjectOnMojo(Object mojo, File baseDir) throws Exception {
        MavenProject project = (MavenProject) getFieldValue(mojo, "project");
        if (project == null) {
            project = new MavenProject();
            setVariableValueToObject(mojo, "project", project);
        }
        if (project.getBuild() == null) {
            project.setBuild(new Build());
        }
        setVariableValueToObject(project, "basedir", baseDir);
        project.getBuild().setDirectory(new File(baseDir, "target").getAbsolutePath());
        return project;
    }

    private Object getFieldValue(Object target, String fieldName) throws IllegalAccessException {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                var field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    File syncToTempDirectory(String srcTestDirectory, Path tempDir) throws IOException {
        Path tmpTestDir = Files.createDirectory(tempDir.resolve("test"));
        FileUtils.copyDirectory(resolveTestDirectory(srcTestDirectory), tmpTestDir.toFile());
        return tmpTestDir.toFile();
    }

    private File resolveTestDirectory(String srcTestDirectory) {
        File directory = new File(srcTestDirectory);
        if (directory.isDirectory()) {
            return directory;
        }

        directory = new File("jeap-process-archive-avro-maven-plugin", srcTestDirectory);
        if (directory.isDirectory()) {
            return directory;
        }

        return new File(srcTestDirectory);
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
