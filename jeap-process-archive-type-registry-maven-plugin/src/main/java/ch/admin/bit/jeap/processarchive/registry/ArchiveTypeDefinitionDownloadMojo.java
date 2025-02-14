package ch.admin.bit.jeap.processarchive.registry;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import ch.admin.bit.jeap.processarchive.registry.repository.ArchiveTypeDefinitionReferences;
import ch.admin.bit.jeap.processarchive.registry.repository.ArchiveTypeDefinitionReferencesReader;
import ch.admin.bit.jeap.processarchive.registry.repository.ArchiveTypeDefinitionRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

@Mojo(name = "download-archive-type-definitions", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class ArchiveTypeDefinitionDownloadMojo extends AbstractMojo {

    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/classes/archive-types")
    private File outputDirectory;

    @Parameter(name = "referencesFile", defaultValue = "${basedir}/src/main/processarchive/archive-type-definition-references.json")
    private File referencesFile;

    @Override
    public void execute() throws MojoFailureException {
        try {
            ArchiveTypeDefinitionReferences references = ArchiveTypeDefinitionReferencesReader.read(referencesFile);

            GitReference gitReference = references.getGitReference();

            ArchiveTypeDefinitionRepository.builder()
                    .outputDirectory(outputDirectory)
                    .repoUrl(references.getRepoUrl())
                    .gitReference(gitReference)
                    .log(getLog())
                    .build()
                    .copyArchiveTypeDefinitions(references.getSystems());
        } catch (Exception e) {
            throw new MojoFailureException("Failed to copy archive type definitions: " + e.getMessage(), e);
        }
    }
}
