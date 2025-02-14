package ch.admin.bit.jeap.processarchive.avro.plugin.mojo;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.AvroCompiler;
import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.DownloadResult;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.DownloadedSchema;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.RegistryException;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.RegistryService;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Generate Java classes and interfaces from Archive Type Registry References
 */
@Mojo(name = "registry", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class RegistryMojo extends AbstractAvroMojo {
    @Parameter(name = "descriptorDirectory", defaultValue = "${project.build.directory}/classes")
    @SuppressWarnings("unused")
    private File descriptorDirectory;

    @Override
    protected String[] getIncludedFileNames() {
        return new String[]{"archive-types.json"};
    }

    @Override
    protected void compile(AvroCompiler avroCompiler, File descriptorFile) throws MojoExecutionException {
        try {
            DownloadResult downloadResult = downloadFromRegistry(descriptorFile);
            compileDownloadedFiles(avroCompiler, downloadResult.getSchemas());
        } catch (RegistryException e) {
            throw new MojoExecutionException("Cannot download type: " + e.getMessage(), e);
        }
    }

    private DownloadResult downloadFromRegistry(File descriptorFile) {
        getLog().info("Download references file " + descriptorFile.getAbsolutePath());
        RegistryService referencesFileHandler = new RegistryService(descriptorFile);
        DownloadResult result = referencesFileHandler.download();
        getLog().debug("Download " + result.getArchiveTypeDescriptors().size() + " types");
        return result;
    }

    private void compileDownloadedFiles(AvroCompiler avroCompiler, List<DownloadedSchema> schemas) throws MojoExecutionException {
        for (DownloadedSchema schema : schemas) {
            getLog().debug("Compile downloaded schema " + schema.getSchema().getAbsolutePath());
            compileDownloadedFile(avroCompiler, schema);
        }
        getLog().debug("Compiled all downloaded schemas");
    }

    private void compileDownloadedFile(AvroCompiler avroCompiler, DownloadedSchema downloadedSchema) throws MojoExecutionException {
        File file = downloadedSchema.getSchema();
        if (file.getName().endsWith("avdl")) {
            try (ImportClassLoader importClassLoader = getImportClassLoader(downloadedSchema)) {
                compileIdl(avroCompiler, file, importClassLoader);
            } catch (IOException | DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("Cannot create classpath loader", e);
            }
        } else if (file.getName().endsWith("avpr")) {
            compileProtocol(avroCompiler, file);
        } else if (file.getName().endsWith("avsc")) {
            compileSchema(avroCompiler, file);
        } else {
            throw new MojoExecutionException("Downloaded file " + file.getName() + " has an invalid file ending");
        }
    }

    private ImportClassLoader getImportClassLoader(DownloadedSchema downloadedSchema) throws IOException, DependencyResolutionRequiredException {
        ImportClassLoader importClassLoader = new ImportClassLoader(getSourceDirectory(), getProject().getRuntimeClasspathElements());
        for (String filename : downloadedSchema.getImportPath().keySet()) {
            importClassLoader.addImportFile(filename, downloadedSchema.getImportPath().get(filename));
        }
        return importClassLoader;
    }
}
