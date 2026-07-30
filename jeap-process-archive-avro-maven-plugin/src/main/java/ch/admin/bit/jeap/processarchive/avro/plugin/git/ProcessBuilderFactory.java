package ch.admin.bit.jeap.processarchive.avro.plugin.git;

import java.io.File;

/**
 * Factory class to create instances of {@link ProcessBuilder}.
 * This allows for easier testing and mocking of process execution.
 */
public class ProcessBuilderFactory {

    /**
     * Creates a process builder for the given command, to be run in the given working directory.
     * <p>
     * The working directory is mandatory on purpose: a process inheriting the working directory of the JVM would
     * make a git command operate on whatever repository happens to enclose it instead of on the archive type
     * registry (for example on the repository of the build itself when running in a test).
     */
    ProcessBuilder createProcessBuilder(File workingDirectory, String... command) {
        return new ProcessBuilder(command)
                .directory(workingDirectory);
    }

}
