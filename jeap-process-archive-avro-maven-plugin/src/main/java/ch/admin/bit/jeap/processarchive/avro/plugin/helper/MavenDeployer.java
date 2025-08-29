package ch.admin.bit.jeap.processarchive.avro.plugin.helper;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class MavenDeployer {

    public interface InvokerFactory {
        Invoker createInvoker();
    }

    private final Log log;
    private final String mavenDeployGoal;
    private final String mavenExecutable;
    private final String mavenGlobalSettingsFile;
    private final String profile;
    private final InvokerFactory invokerFactory;

    public MavenDeployer(Log log, String mavenDeployGoal, String mavenExecutable, String mavenGlobalSettingsFile, String profile, InvokerFactory invokerFactory) {
        this.log = log;
        this.mavenDeployGoal = mavenDeployGoal;
        this.mavenExecutable = mavenExecutable;
        this.mavenGlobalSettingsFile = mavenGlobalSettingsFile;
        this.profile = profile;
        this.invokerFactory = invokerFactory;
    }

    public void deployProjects(List<Path> poms) {
        poms.stream()
                .map(this::toInvocationRequest)
                .forEach(this::runMavenInvoker);
    }

    private void runMavenInvoker(InvocationRequest request) {
        log.info("Executing maven request for pom %s: mnv=%s args=%s profiles=%s goals=%s props=%s".formatted(
                request.getPomFile(), getMavenExecutable(), request.getArgs(),
                request.getProfiles() == null ? "" : request.getProfiles(), request.getGoals(), request.getProperties()));
        executeRequest(request);
    }

    @SneakyThrows
    private void executeRequest(InvocationRequest request) {
        Invoker invoker = invokerFactory.createInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Build failed with exitCode " + result.getExitCode());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Error during Maven Invocation: " + e.getMessage(), e);
        }
    }

    private InvocationRequest toInvocationRequest(Path pomPath) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setPomFile(pomPath.toFile());
        request.setMavenExecutable(getMavenExecutable());
        request.setGlobalSettingsFile(getMavenGlobalSettingsFile());
        if (profile != null) {
            request.setProfiles(List.of(profile));
        }
        request.addArg(mavenDeployGoal);
        Properties properties = new Properties();
        // No tests in generated archive types, skip
        properties.setProperty("maven.test.skip", "true");
        // Colored output
        properties.setProperty("style.color", "always");
        properties.setProperty("jansi.force", "true");
        // Pass proxy properties to invoked maven instance
        Map<String, String> proxyProperties = System.getProperties().entrySet().stream()
                .filter(e -> e.getKey().toString().matches("^http.*[pP]roxy.*$")) // NOSONAR
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        properties.putAll(proxyProperties);
        request.setProperties(properties);
        return request;
    }

    private File getMavenExecutable() {
        if (!StringUtils.isBlank(this.mavenExecutable) && Files.exists(Path.of(this.mavenExecutable))) {
            return Path.of(this.mavenExecutable).toFile();
        }
        return Paths.get(Paths.get("").toAbsolutePath().toString(), "mvnw").toFile();
    }

    private File getMavenGlobalSettingsFile() {
        if (!StringUtils.isBlank(this.mavenGlobalSettingsFile) && Files.exists(Path.of(this.mavenGlobalSettingsFile))) {
            return Path.of(this.mavenGlobalSettingsFile).toFile();
        }
        return Paths.get(Paths.get("").toAbsolutePath().toString(), "settings.xml").toFile();
    }
}
