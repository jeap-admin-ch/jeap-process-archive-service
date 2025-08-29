package ch.admin.bit.jeap.processarchive.avro.plugin.mojo;

import ch.admin.bit.jeap.processarchive.avro.plugin.helper.MavenDeployer;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeRegistryConstants;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Mojo(name = "deploy-archive-type-artifacts", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class ArchiveTypeArtifactsDeployerMojo extends AbstractMojo {

    public static final String POM_XML_FILE_NAME = "pom.xml";

    @Parameter(name = "sourcesDirectory", defaultValue = "${project.build.directory}/generated-sources")
    @SuppressWarnings("unused")
    private File sourcesDirectory;

    @Parameter(name = "mavenDeployGoal", defaultValue = "install")
    @SuppressWarnings("unused")
    private String mavenDeployGoal;

    @Parameter(name = "mavenExecutable")
    @SuppressWarnings("unused")
    private String mavenExecutable;

    @Parameter(name = "mavenGlobalSettingsFile")
    @SuppressWarnings("unused")
    private String mavenGlobalSettingsFile;

    @Parameter(name = "currentBranch", defaultValue = "${git.branch}", required = true)
    @Setter
    private String currentBranch;

    @Parameter(name = "trunkBranchName", defaultValue = "master", required = true)
    @Setter
    private String trunkBranchName;

    @Parameter(name = "trunkMavenProfile")
    @Setter
    private String trunkMavenProfile;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    public void execute() throws MojoExecutionException {
        if (!sourcesDirectory.exists()) {
            return;
        }
        String profile = isBuildOnTrunk() ? trunkMavenProfile : null;
        MavenDeployer deployer = new MavenDeployer(getLog(), mavenDeployGoal, mavenExecutable, mavenGlobalSettingsFile, profile, executorService);
        deployCommonLibraries(deployer);
        deployLibraries(deployer);
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private void deployCommonLibraries(MavenDeployer deployer) throws MojoExecutionException {
        try (Stream<Path> walk = Files.walk(Paths.get(sourcesDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            List<Path> poms = walk.filter(path -> isCommonLibrary(path) && path.toString().endsWith(POM_XML_FILE_NAME)).collect(toList());
            getLog().info("Deploying " + poms.size() + " common maven projects.");
            deployer.deployProjects(poms);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot parse the output directory for the pom.xml: " + e.getMessage(), e);
        }

    }

    private void deployLibraries(MavenDeployer deployer) throws MojoExecutionException {
        try (Stream<Path> walk = Files.walk(Paths.get(sourcesDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            List<Path> poms = walk.filter(path -> !isCommonLibrary(path) && path.toString().endsWith(POM_XML_FILE_NAME)).collect(toList());
            getLog().info("Deploying " + poms.size() + " maven projects.");
            deployer.deployProjects(poms);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot parse the output directory for the pom.xml: " + e.getMessage(), e);
        }

    }

    private boolean isCommonLibrary(Path path) {
        return path.toString().contains(ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
    }

    private boolean isBuildOnTrunk() {
        return trunkBranchName.equals(currentBranch);
    }
}
