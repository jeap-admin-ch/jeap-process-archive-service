package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeRegistryConstants.COMMON_DIR_NAME;

@Component
@Slf4j
public class ArchiveTypeLoader {

    private static final String RESOURCE_PATH = "/archive-types";
    private static final int MAX_DESCRIPTOR_SEARCH_DEPTH = 5;

    private final ObjectMapper objectMapper;
    private final Map<Path, ImportClassLoader> importClassLoadersPerSystem = new HashMap<>();

    ArchiveTypeLoader() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    Map<ArchiveTypeId, ArchiveType> loadArchiveTypes() throws IOException {
        Path targetRoot = Files.createTempDirectory(getClass().getSimpleName());
        try {
            // The avro IDl parser supports imports only for file-based schemas, only the constructor
            // Idl(File inputFile, ClassLoader resourceLoader) accepts a resourceLoader that resolves imported files.
            // The schema registry is thus extracted to the file system, and then loaded from there.
            ArchiveResourceUtils.copyFromClassPath(RESOURCE_PATH, targetRoot);

            Map<ArchiveTypeId, ArchiveType> archiveTypes = new HashMap<>();
            Files.find(targetRoot, MAX_DESCRIPTOR_SEARCH_DEPTH,
                            (path, attrs) -> path.getFileName().toString().endsWith(".json") && Files.isRegularFile(path))
                    .forEach(descriptorPath -> loadArchiveTypeFromDescriptor(descriptorPath, archiveTypes));

            return archiveTypes;
        } finally {
            FileUtils.deleteQuietly(targetRoot.toFile());
        }
    }

    private void loadArchiveTypeFromDescriptor(Path descriptorPath, Map<ArchiveTypeId, ArchiveType> archiveTypes) {
        log.info("Reading archive type descriptor {}", descriptorPath);
        ArchiveTypeDescriptor archiveTypeDescriptor = parseDescriptor(descriptorPath);
        Path systemDir = descriptorPath.getParent();
        archiveTypeDescriptor.getVersions()
                .forEach(v -> loadArchiveTypeVersion(v, archiveTypeDescriptor, systemDir, archiveTypes));
    }

    private ArchiveTypeDescriptor parseDescriptor(Path descriptorPath) {
        try {
            return objectMapper.readValue(descriptorPath.toFile(), ArchiveTypeDescriptor.class);
        } catch (IOException e) {
            throw ArchiveTypeLoaderException.jsonParsingFailed(descriptorPath, e);
        }
    }

    private void loadArchiveTypeVersion(ArchiveTypeVersion version, ArchiveTypeDescriptor archiveTypeDescriptor,
                                        Path descriptorDir, Map<ArchiveTypeId, ArchiveType> schemas) {
        String archiveTypeName = archiveTypeDescriptor.getArchiveType();
        ArchiveTypeId archiveTypeId = ArchiveTypeId.builder()
                .system(archiveTypeDescriptor.getSystem())
                .name(archiveTypeName)
                .version(version.getVersion())
                .build();
        Path schemaFile = getSchemaFile(descriptorDir, version.getSchema());
        log.info("Loading schema for archive type {} version {} for system {} from schema file {}",
                archiveTypeId.getName(), archiveTypeId.getVersion(), archiveTypeId.getSystem(), schemaFile);
        Schema schema = loadSchema(descriptorDir, archiveTypeName, schemaFile);
        ArchiveType archiveType = ArchiveType.builder()
                .name(archiveTypeName)
                .system(archiveTypeDescriptor.getSystem())
                .referenceIdType(archiveTypeDescriptor.getReferenceIdType())
                .version(version.getVersion())
                .schema(schema)
                .expirationDays(archiveTypeDescriptor.getExpirationDays())
                .encryption(getArchiveTypeEncryptionFromDescriptor(archiveTypeDescriptor))
                .encryptionKey(getArchiveTypeEncryptionKeyFromDescriptor(archiveTypeDescriptor))
                .build();
        schemas.put(archiveTypeId, archiveType);
    }

    private ArchiveTypeEncryption getArchiveTypeEncryptionFromDescriptor(ArchiveTypeDescriptor archiveTypeDescriptor) {
        if (archiveTypeDescriptor.getEncryption() != null) {
            return ArchiveTypeEncryption.builder()
                    .secretEnginePath(archiveTypeDescriptor.getEncryption().getSecretEnginePath())
                    .keyName(archiveTypeDescriptor.getEncryption().getKeyName())
                    .build();
        }
        return null;
    }

    private ArchiveTypeEncryptionKey getArchiveTypeEncryptionKeyFromDescriptor(ArchiveTypeDescriptor archiveTypeDescriptor) {
        if (archiveTypeDescriptor.getEncryptionKey() != null) {
            return ArchiveTypeEncryptionKey.builder()
                    .keyId(archiveTypeDescriptor.getEncryptionKey().getKeyId())
                    .build();
        }
        return null;
    }

    private Schema loadSchema(Path descriptorDir, String archiveType, Path schemaFile) {
        Path systemDir = descriptorDir.getParent();
        ImportClassLoader importClassLoader = getOrCreateSystemImportClassLoader(systemDir);
        IdlFileParser idlFileParser = new IdlFileParser(importClassLoader);
        try {
            Protocol protocol = idlFileParser.parseIdlFile(schemaFile.toFile());
            return protocol.getType(protocol.getNamespace() + "." + archiveType);
        } catch (IOException | ParseException e) {
            throw ArchiveTypeLoaderException.avroSchemaParsingFailed(schemaFile, e);
        }
    }

    private ImportClassLoader getOrCreateSystemImportClassLoader(Path systemDir) {
        return importClassLoadersPerSystem.computeIfAbsent(systemDir,
                ArchiveTypeLoader::createImportClassLoader);
    }

    private static ImportClassLoader createImportClassLoader(Path systemDir) {
        Path commonRootDir = systemDir.getParent().resolve(COMMON_DIR_NAME);
        Path commonSystemDir = systemDir.resolve(COMMON_DIR_NAME);
        return new ImportClassLoader(Thread.currentThread().getContextClassLoader(),
                commonRootDir.toFile(), commonSystemDir.toFile());
    }

    private Path getSchemaFile(Path descriptorDir, String filename) {
        Path inArchiveTypeDir = descriptorDir.resolve(filename);
        if (Files.isRegularFile(inArchiveTypeDir)) {
            return inArchiveTypeDir;
        }
        Path systemDir = descriptorDir.getParent();
        Path inCommonSystemDir = systemDir.resolve(COMMON_DIR_NAME).resolve(filename);
        if (Files.isRegularFile(inCommonSystemDir)) {
            return inCommonSystemDir;
        }
        Path inCommonRootDir = systemDir.getParent().resolve(COMMON_DIR_NAME).resolve(filename);
        if (Files.isRegularFile(inCommonRootDir)) {
            return inCommonRootDir;
        }

        throw ArchiveTypeLoaderException.schemaFileNotFound(descriptorDir, filename);
    }
}
