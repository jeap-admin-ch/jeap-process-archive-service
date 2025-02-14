package ch.admin.bit.jeap.processarchive.registry;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.registry.verifier.DescriptorDirectoryValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Mojo(name = "registry", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ArchiveTypeRegistryVerifierMojo extends AbstractMojo {
    @SuppressWarnings("unused")
    @Getter(AccessLevel.PROTECTED)
    @Parameter(name = "descriptorDirectory", defaultValue = "${basedir}/archive-types")
    private File descriptorDirectory;
    @SuppressWarnings("unused")
    @Parameter(name = "gitUrl")
    private String gitUrl;
    @SuppressWarnings("unused")
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try (ImportClassLoader importClassLoader = new ImportClassLoader(getDescriptorDirectory(),
                getProject().getRuntimeClasspathElements())) {
            ValidationContext validationContext = ValidationContext.builder()
                    .descriptorDir(descriptorDirectory)
                    .oldDescriptorDir(getFolderToCompareTo())
                    .importClassLoader(importClassLoader)
                    .build();
            ValidationResult overallResult = DescriptorDirectoryValidator.validate(validationContext);
            if (!overallResult.isValid()) {
                overallResult.getErrors().forEach(getLog()::error);
                String msg = overallResult.getErrors().size() + " errors: " + String.join("\n", overallResult.getErrors());
                throw new MojoExecutionException("The archive type registry is not in a clean state, " + msg);
            }
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot get runtime classpath", e);
        }
    }

    private File getFolderToCompareTo() throws MojoExecutionException {
        if (gitUrl == null) {
            getLog().warn("No GIT repo set, cannot compare to old state");
            return descriptorDirectory;
        }
        try {
            File tempDir = Files.createTempDirectory("master").toFile();
            FileUtils.forceDeleteOnExit(tempDir);
            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setBranch("master")
                    .setDirectory(tempDir)
                    .call();
            return new File(tempDir, descriptorDirectory.getName());
        } catch (IOException | GitAPIException e) {
            throw new MojoExecutionException("Cannot checkout old repo", e);
        }
    }
}
