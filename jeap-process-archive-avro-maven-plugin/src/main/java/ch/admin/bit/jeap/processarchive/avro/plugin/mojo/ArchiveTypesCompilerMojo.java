package ch.admin.bit.jeap.processarchive.avro.plugin.mojo;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.AvroCompiler;
import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.git.GitClient;
import ch.admin.bit.jeap.processarchive.avro.plugin.git.GitDiffDto;
import ch.admin.bit.jeap.processarchive.avro.plugin.git.NewArchiveTypeVersionDto;
import ch.admin.bit.jeap.processarchive.avro.plugin.helper.GeneratedSourcesCleaner;
import ch.admin.bit.jeap.processarchive.avro.plugin.helper.PomFileGenerator;
import ch.admin.bit.jeap.processarchive.avro.plugin.helper.RegistryHelper;
import ch.admin.bit.jeap.processarchive.avro.plugin.helper.TypeDescriptorFactory;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.ArchiveTypeSchema;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.TypeReference;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.metadata.ArchiveTypeMetadata;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.metadata.ArchiveTypeMetadataProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Mojo(name = "compile-archive-types", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ArchiveTypesCompilerMojo extends AbstractMojo {

    private final String commonLibVersion;
    private final GeneratedSourcesCleaner generatedSourcesCleaner = new GeneratedSourcesCleaner();
    @Parameter(name = "sourceDirectory", defaultValue = "${basedir}/archive-types")
    @SuppressWarnings("unused")
    private File sourceDirectory;
    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources")
    @SuppressWarnings("unused")
    private File outputDirectory;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @SuppressWarnings("unused")
    private MavenProject project;
    @Parameter(name = "generateAllArchiveTypes", defaultValue = "false")
    @Setter
    private boolean generateAllArchiveTypes;
    @Parameter(name = "groupIdPrefix", required = true)
    @Setter
    private String groupIdPrefix;
    @Parameter(name = "currentBranch", defaultValue = "${git.branch}", required = true)
    @Setter
    private String currentBranch;
    @Parameter(name = "commitId", defaultValue = "${git.commit.id}", required = true)
    @Setter
    private String commitId;
    @Parameter(name = "gitUrl", required = true)
    @Setter
    private String gitUrl;
    @Parameter(name = "trunkBranchName", defaultValue = "master", required = true)
    @Setter
    private String trunkBranchName;
    @Parameter(name = "avroVersion", defaultValue = "${avro.version}", required = true)
    @Setter
    private String avroVersion;
    @Parameter(name = "pomTemplateFile")
    @Setter
    private File pomTemplateFile;
    @Parameter(name = "skip", defaultValue = "false", required = true)
    @Setter
    private boolean skip;
    @Parameter(name = "fetchTags", defaultValue = "true")
    @Setter
    private boolean fetchTags;
    @Setter
    @Parameter(name = "archiveTypeRepoGitTokenEnvVariableName", defaultValue = "ARCHIVE_TYPE_REPO_GIT_TOKEN")
    private String archiveTypeRepoGitTokenEnvVariableName;
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "false")
    @SuppressWarnings("unused")
    private boolean enableDecimalLogicalType;
    private Map<String, List<Path>> commonDefinitionsPerSystem = new HashMap<>();

    public ArchiveTypesCompilerMojo() {
        this.commonLibVersion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss"));
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Archive type compilation is skipped");
            return;
        }

        getLog().info("Current Branch: " + this.currentBranch);
        String sourceEncoding = project.getProperties().getProperty("project.build.sourceEncoding");
        AvroCompiler avroCompiler = AvroCompiler.builder()
                .sourceEncoding(sourceEncoding)
                .outputDirectory(outputDirectory)
                .enableDecimalLogicalType(enableDecimalLogicalType)
                .build();

        GitClient gitClient = new GitClient(this.project.getBasedir().getAbsolutePath(), this.gitUrl, this.trunkBranchName, getLog());

        if (fetchTags) {
            getLog().info("Fetching tags from remote Git repository.");
            gitClient.gitFetchTags(getGitToken());
        }

        if (generateAllArchiveTypes) {
            compile(avroCompiler);
        } else {
            final GitDiffDto gitDiff = gitClient.getGitDiff(this.currentBranch);
            if (gitDiff.hasChanges()) {
                getLog().info("New archive types since compared commit: " + newTypes(gitDiff.newArchiveTypeVersions()));
                compile(avroCompiler, gitDiff);
            } else {
                getLog().info("No changes from compared commit. Skipping compile...");
            }
        }

        generatedSourcesCleaner.cleanupDuplicatedCommonFiles(outputDirectory, commonDefinitionsPerSystem);
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    private String getGitToken() {
        getLog().info("The archive type repo git token env variable name configured is: " + archiveTypeRepoGitTokenEnvVariableName);
        String token = Optional.ofNullable(System.getenv(archiveTypeRepoGitTokenEnvVariableName))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        getLog().info("The env variable " + archiveTypeRepoGitTokenEnvVariableName + " is " +
                (token != null ? "set." : "not set."));
        return token;
    }

    private String newTypes(Set<NewArchiveTypeVersionDto> newArchiveTypeVersionDtos) {
        return newArchiveTypeVersionDtos.stream()
                .map(Record::toString)
                .collect(joining("\n", "\n", ""));
    }

    /**
     * Compile all archive types of the registry based on the descriptors
     *
     * @param avroCompiler the avro compiler used to compile the schemas
     * @throws MojoExecutionException if the process failed
     */
    private void compile(AvroCompiler avroCompiler) throws MojoExecutionException {
        this.commonDefinitionsPerSystem = retrieveCommonDefinitionsGroupedBySystem();
        compileCommonSchemas(avroCompiler, Set.of());
        compileSchemas(avroCompiler, retrieveSchemas(retrieveDescriptors()));
    }

    /**
     * Compile only changed files based on a git diff
     *
     * @param avroCompiler the avro compiler used to compile the schemas
     * @param gitDiff      Git differences relevant for the compiler
     * @throws MojoExecutionException if the process failed
     */
    private void compile(AvroCompiler avroCompiler, GitDiffDto gitDiff) throws MojoExecutionException {
        this.commonDefinitionsPerSystem = retrieveCommonDefinitionsGroupedBySystem();
        compileCommonSchemas(avroCompiler, gitDiff.systems());
        compileSchemas(avroCompiler, retrieveSchemasForNewArchiveTypeVersions(gitDiff.newArchiveTypeVersions()));
    }

    private List<ArchiveTypeSchema> retrieveSchemasForNewArchiveTypeVersions(Set<NewArchiveTypeVersionDto> newArchiveTypeVersionDtos) {
        List<ArchiveTypeSchema> list = new ArrayList<>();
        for (NewArchiveTypeVersionDto dto : newArchiveTypeVersionDtos) {
            Optional<ArchiveTypeSchema> schema = createSchema(dto.descriptorPath(), dto.typeDescriptor(), dto.version());
            schema.ifPresent(list::add);
        }
        return list;
    }

    private Optional<ArchiveTypeSchema> createSchema(Path descriptorPath, ArchiveTypeDescriptor typeDescriptor, Integer newVersion) {
        Optional<ArchiveTypeVersion> typeVersionSchema = typeDescriptor.getVersions().stream()
                .filter(v -> v.getVersion().equals(newVersion)).findFirst();
        return typeVersionSchema.map(version ->
                descriptorToArchiveTypeSchema(schemaFile(descriptorPath, version.getSchema()), typeDescriptor, version));
    }

    private static File schemaFile(Path descriptorPath, String schemaFile) {
        return new File(descriptorPath.getParent().toFile(), schemaFile);
    }

    /**
     * Retrieve all common files ending with avdl for all systems and save the files grouped by system in a map
     *
     * @return a map with all common files grouped by systems
     * @throws MojoExecutionException if the source directory is not available
     */
    private Map<String, List<Path>> retrieveCommonDefinitionsGroupedBySystem() throws MojoExecutionException {
        try (Stream<Path> stream = Files.walk(Paths.get(sourceDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            return stream
                    .filter(path -> path.toString().contains(ArchiveTypeRegistryConstants.COMMON_DIR_NAME) && FilenameUtils.getExtension(path.getFileName().toString()).equals("avdl"))
                    .collect(Collectors.groupingBy(file -> file.getParent().getParent().getFileName().toString(), Collectors.mapping(file -> file, Collectors.toList())));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot walk through the source directory at " + sourceDirectory, e);
        }
    }

    private void compileCommonSchemas(AvroCompiler avroCompiler, Set<String> changedSystemsToCompile) throws MojoExecutionException {
        getLog().info("Compile common schemas for " + commonDefinitionsPerSystem.size() + " systems");
        for (Map.Entry<String, List<Path>> entry : commonDefinitionsPerSystem.entrySet()) {

            if (!changedSystemsToCompile.isEmpty() && !changedSystemsToCompile.contains(entry.getKey())) {
                getLog().info("... System " + entry.getKey() + " not changed. Skipping...");
            } else {

                List<ArchiveTypeSchema> schemas = entry.getValue().stream()
                        .map(commonFile -> commonSchemaToArchiveTypeSchema(commonFile.toFile(), entry.getKey(), commonFile.getFileName().toString(), RegistryHelper.retrieveVersionFromCommonDefinition(commonFile.toString())))
                        .toList();

                for (ArchiveTypeSchema schema : schemas) {
                    getLog().debug("Compile common schema: " + schema.getSchema().getAbsolutePath());
                    final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), retrieveTypeReference(schema).getSystem(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
                    compileSchema(outputPath, avroCompiler, schema);
                }

                final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), entry.getKey(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
                PomFileGenerator pomFileGenerator = new PomFileGenerator(outputPath, pomTemplateFile, avroVersion, getLog());
                pomFileGenerator.generatePomFile(getGroupIdPrefixWithTrailingDot() + entry.getKey().toLowerCase(Locale.ROOT), entry.getKey() + "-archivetype-common", "", getCommonLibVersionAsProjectVersion());

                getLog().info("Compiled " + entry.getValue().size() + " common schemas for system " + entry.getKey());
            }
        }
    }

    private String getGroupIdPrefixWithTrailingDot() {
        return groupIdPrefix.endsWith(".") ? groupIdPrefix : groupIdPrefix + ".";
    }

    private TypeReference retrieveTypeReference(ArchiveTypeSchema schema) throws MojoExecutionException {
        final Optional<TypeReference> typeReference = schema.getTypeReference();
        if (typeReference.isPresent()) {
            return typeReference.get();
        } else {
            throw new MojoExecutionException("TypeReference is empty but must be present");
        }
    }

    private Map<String, File> retrieveCommonFilesForSystem(String systemName) {
        final Path systemCommonDirectoryPath = Paths.get(sourceDirectory.getAbsolutePath(), systemName.toLowerCase(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);

        if (!Files.exists(systemCommonDirectoryPath)) {
            return Collections.emptyMap();
        }

        try (Stream<Path> stream = Files.walk(systemCommonDirectoryPath, Integer.MAX_VALUE)) {
            return stream
                    .filter(file -> FilenameUtils.getExtension(file.getFileName().toString()).equals("avdl"))
                    .collect(toMap(filepath -> filepath.getFileName().toString(), Path::toFile));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot get the common files: " + e.getMessage(), e);
        }
    }

    private List<Path> retrieveDescriptors() throws MojoExecutionException {
        try (Stream<Path> stream = Files.walk(Paths.get(sourceDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            return stream
                    .filter(f -> FilenameUtils.getExtension(f.getFileName().toString()).equals("json"))
                    .toList();
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot walk through the sourceDirectory: " + e.getMessage(), e);
        }
    }

    private ArchiveTypeSchema commonSchemaToArchiveTypeSchema(File schemaFile, String system, String archiveTypeName, Integer version) {
        return ArchiveTypeSchema.builder()
                .schema(schemaFile)
                .typeReference(new TypeReference(system, archiveTypeName, version))
                .importPath(Map.of())
                .build();
    }

    private ArchiveTypeSchema descriptorToArchiveTypeSchema(File schemaFile, ArchiveTypeDescriptor typeDescriptor, ArchiveTypeVersion version) {
        ArchiveTypeMetadata metadata = ArchiveTypeMetadataProvider
                .createMetadata(typeDescriptor, version, currentBranch, commitId, gitUrl);
        return ArchiveTypeSchema.builder()
                .schema(schemaFile)
                .archiveTypeMetadata(metadata)
                .typeReference(new TypeReference(typeDescriptor.getSystem(), typeDescriptor.getArchiveType(), version.getVersion()))
                .importPath(retrieveCommonFilesForSystem(typeDescriptor.getSystem()))
                .build();
    }

    private List<ArchiveTypeSchema> retrieveSchemas(List<Path> descriptors) throws MojoExecutionException {
        List<ArchiveTypeSchema> schemas = new ArrayList<>();
        for (Path descriptor : descriptors) {
            schemas.addAll(retrieveSchemasForDescriptor(descriptor, TypeDescriptorFactory.getTypeDescriptor(descriptor)));
        }
        return schemas;
    }

    private List<ArchiveTypeSchema> retrieveSchemasForDescriptor(Path descriptor, ArchiveTypeDescriptor typeDescriptor) {
        List<ArchiveTypeSchema> schemas = new ArrayList<>();
        for (ArchiveTypeVersion version : typeDescriptor.getVersions()) {
            schemas.add(descriptorToArchiveTypeSchema(schemaFile(descriptor, version.getSchema()), typeDescriptor, version));
        }
        return schemas;
    }

    private void compileSchemas(AvroCompiler avroCompiler, List<ArchiveTypeSchema> schemas) throws MojoExecutionException {
        getLog().info("Compiling " + schemas.size() + " schemas");
        for (ArchiveTypeSchema schema : schemas) {
            getLog().debug("Compile schema " + schema.getSchema().getAbsolutePath());
            final TypeReference typeReference = retrieveTypeReference(schema);
            final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), typeReference.getSystem().toLowerCase(Locale.ROOT), typeReference.getName(), String.valueOf(typeReference.getVersion()));
            String groupId = getGroupIdPrefixWithTrailingDot() + typeReference.getSystem().toLowerCase(Locale.ROOT);
            PomFileGenerator pomFileGenerator = new PomFileGenerator(outputPath, pomTemplateFile, avroVersion, getLog());
            String artifactId = camelCase2Snake(typeReference.getName()) + "v" + typeReference.getVersion();
            pomFileGenerator.generatePomFile(groupId,
                    artifactId,
                    getDependencyDefinition(typeReference),
                    getArtifactVersion(typeReference.getVersion()));
            compileSchema(outputPath, avroCompiler, schema);
        }
        getLog().info("Compiled all schemas");
    }

    private String getDependencyDefinition(TypeReference typeReference) {
        String dependency = "";
        if (commonLibAvailableForSystem(typeReference.getSystem())) {
            String groupId = getGroupIdPrefixWithTrailingDot() + typeReference.getSystem().toLowerCase(Locale.ROOT);
            dependency = PomFileGenerator.getCommonDependency(
                    groupId,
                    typeReference.getSystem().toLowerCase(Locale.ROOT),
                    getCommonLibVersionAsDependencyVersion());
        }

        return dependency;
    }

    private void compileSchema(Path outputPath, AvroCompiler avroCompiler, ArchiveTypeSchema archiveTypeSchema) throws MojoExecutionException {
        File outputDirectory = Paths.get(outputPath.toString(), "src", "main", "java").toFile();
        AvroCompiler.AvroCompilerBuilder avroCompilerBuilder = avroCompiler.toBuilder().outputDirectory(outputDirectory);
        archiveTypeSchema.getArchiveTypeMetadata().ifPresent(avroCompilerBuilder::additionalTool);

        avroCompiler = avroCompilerBuilder.build();

        getLog().info("Compiling to " + outputDirectory.getAbsolutePath());

        try (ImportClassLoader importClassLoader = getImportClassLoader(archiveTypeSchema)) {
            compileIdl(avroCompiler, archiveTypeSchema.getSchema(), importClassLoader);
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot create classpath loader: " + e.getMessage(), e);
        }
    }

    private String getArtifactVersion(Integer typeReferenceVersion) {
        if (isBuildOnTrunk()) {
            return String.valueOf(typeReferenceVersion);
        } else {
            return String.format("%s-%s-SNAPSHOT", typeReferenceVersion, getSanitizedCurrentBranchName());
        }
    }

    private String getCommonLibVersionAsDependencyVersion() {
        if (isBuildOnTrunk()) {
            return "[0,)";
        } else {
            return getCommonLibSnapshotVersion();
        }
    }

    private String getCommonLibVersionAsProjectVersion() {
        if (isBuildOnTrunk()) {
            return commonLibVersion;
        } else {
            return getCommonLibSnapshotVersion();
        }
    }

    private boolean isBuildOnTrunk() {
        return trunkBranchName.equals(currentBranch);
    }

    private String getCommonLibSnapshotVersion() {
        return String.format("%s-%s-SNAPSHOT", commonLibVersion, getSanitizedCurrentBranchName());
    }

    private String getSanitizedCurrentBranchName() {
        return currentBranch.replaceAll("[^A-Za-z0-9-_]", "_");
    }

    private boolean commonLibAvailableForSystem(String definingSystem) {
        return commonDefinitionsPerSystem.containsKey(definingSystem.toLowerCase(Locale.ROOT));
    }

    private ImportClassLoader getImportClassLoader(ArchiveTypeSchema archiveTypeSchema) throws IOException, DependencyResolutionRequiredException {
        ImportClassLoader importClassLoader = new ImportClassLoader(sourceDirectory, project.getRuntimeClasspathElements());
        for (String filename : archiveTypeSchema.getImportPath().keySet()) {
            importClassLoader.addImportFile(filename, archiveTypeSchema.getImportPath().get(filename));
        }
        return importClassLoader;
    }

    private void compileIdl(AvroCompiler avroCompiler, File file, ImportClassLoader importClassLoader) throws MojoExecutionException {
        try {
            IdlFileParser idlFileParser = new IdlFileParser(importClassLoader);
            Protocol protocol = idlFileParser.parseIdlFile(file);
            //We cannot skip compile when the file has not changed
            //as there might include which have changed
            getLog().debug("Compile protocol " + protocol.getName() + " from IDL file " + file.getAbsolutePath());
            protocol.getTypes().forEach(t -> getLog().debug("Type " + t.getName() + " is in this record"));
            avroCompiler.compileProtocol(protocol, null);
        } catch (IOException | ParseException e) {
            throw compileException(e, file);
        }
    }

    private MojoExecutionException compileException(Exception e, File file) {
        return new MojoExecutionException("Could not compile file " + file.getAbsolutePath(), e);
    }

    private String camelCase2Snake(String input) {
        if (isBlank(input)) {
            return input;
        }
        return input.replaceAll("([A-Z])", "-$1").replaceFirst("^-", "").toLowerCase();
    }
}
