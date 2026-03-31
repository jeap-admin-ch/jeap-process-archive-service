package ch.admin.bit.jeap.processarchive.avro.pluginIntegration;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAvroMojoTest {
    @TempDir
    Path temporaryFolder;

    private PlexusContainer container;
    private ComponentConfigurator configurator;
    private Map<String, MojoDescriptor> mojoDescriptors;

    @BeforeEach
    void initContainer() throws Exception {
        ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
        var cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");
        container = new DefaultPlexusContainer(cc);

        configurator = container.lookup(ComponentConfigurator.class, "basic");

        Map<Object, Object> map = container.getContext().getContextData();
        try (InputStream is = getClass().getResourceAsStream("/META-INF/maven/plugin.xml");
             Reader reader = new BufferedReader(new XmlStreamReader(is));
             InterpolationFilterReader interpolationReader = new InterpolationFilterReader(reader, map, "${", "}")) {

            PluginDescriptor pluginDescriptor = new PluginDescriptorBuilder().build(interpolationReader);

            var artifact = new DefaultArtifact(
                    pluginDescriptor.getGroupId(),
                    pluginDescriptor.getArtifactId(),
                    pluginDescriptor.getVersion(),
                    null, "jar", null,
                    new DefaultArtifactHandler("jar"));
            artifact.setFile(new File(System.getProperty("basedir", "")).getCanonicalFile());
            pluginDescriptor.setPluginArtifact(artifact);
            pluginDescriptor.setArtifacts(List.of(artifact));

            for (ComponentDescriptor<?> desc : pluginDescriptor.getComponents()) {
                container.addComponentDescriptor(desc);
            }

            mojoDescriptors = new HashMap<>();
            for (MojoDescriptor md : pluginDescriptor.getMojos()) {
                mojoDescriptors.put(md.getGoal(), md);
            }
        }
    }

    @AfterEach
    void disposeContainer() {
        if (container != null) {
            container.dispose();
        }
    }

    protected Mojo lookupConfiguredMojo(File basedir, String goal) throws Exception {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest buildingRequest = request.getProjectBuildingRequest();
        buildingRequest.setRepositorySession(new DefaultRepositorySystemSession());

        File pom = new File(basedir, "pom.xml");
        MavenProject project = container.lookup(ProjectBuilder.class).build(pom, buildingRequest).getProject();

        MavenSession session = new MavenSession(
                container, MavenRepositorySystemUtils.newSession(), request, new DefaultMavenExecutionResult());
        session.setCurrentProject(project);
        session.setProjects(List.of(project));

        MojoDescriptor mojoDescriptor = mojoDescriptors.get(goal);
        Objects.requireNonNull(mojoDescriptor, "No MojoDescriptor found for goal: " + goal);
        MojoExecution execution = new MojoExecution(mojoDescriptor);
        finalizeMojoConfiguration(execution);

        Mojo mojo = (Mojo) container.lookup(mojoDescriptor.getRole(), mojoDescriptor.getRoleHint());

        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, execution);

        Xpp3Dom configuration = Optional.ofNullable(project.getPlugin(mojoDescriptor.getPluginDescriptor().getPluginLookupKey()))
                .map(plugin -> (Xpp3Dom) plugin.getConfiguration())
                .orElseGet(() -> new Xpp3Dom("configuration"));
        configuration = Xpp3Dom.mergeXpp3Dom(configuration, execution.getConfiguration());

        configurator.configureComponent(
                mojo, new XmlPlexusConfiguration(configuration), evaluator, container.getContainerRealm());

        return mojo;
    }

    private void finalizeMojoConfiguration(MojoExecution mojoExecution) {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        Xpp3Dom executionConfiguration = mojoExecution.getConfiguration();
        if (executionConfiguration == null) {
            executionConfiguration = new Xpp3Dom("configuration");
        }

        Xpp3Dom defaultConfiguration = new Xpp3Dom(MojoDescriptorCreator.convert(mojoDescriptor));
        Xpp3Dom finalConfiguration = new Xpp3Dom("configuration");

        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                Xpp3Dom parameterConfiguration = executionConfiguration.getChild(parameter.getName());
                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }
                Xpp3Dom parameterDefaults = defaultConfiguration.getChild(parameter.getName());
                parameterConfiguration = Xpp3Dom.mergeXpp3Dom(parameterConfiguration, parameterDefaults, Boolean.TRUE);
                if (parameterConfiguration != null) {
                    parameterConfiguration = new Xpp3Dom(parameterConfiguration, parameter.getName());
                    if (StringUtils.isEmpty(parameterConfiguration.getAttribute("implementation"))
                            && StringUtils.isNotEmpty(parameter.getImplementation())) {
                        parameterConfiguration.setAttribute("implementation", parameter.getImplementation());
                    }
                    finalConfiguration.addChild(parameterConfiguration);
                }
            }
        }

        mojoExecution.setConfiguration(finalConfiguration);
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
