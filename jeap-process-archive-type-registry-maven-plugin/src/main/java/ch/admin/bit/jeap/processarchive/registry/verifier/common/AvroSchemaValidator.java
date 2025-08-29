package ch.admin.bit.jeap.processarchive.registry.verifier.common;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Builder
@AllArgsConstructor
public class AvroSchemaValidator {
    private final IdlFileParser idlFileParser;
    private final JsonNode archiveTypeDescriptor;
    private final ValidationContext validationContext;
    private ValidationResult schemaValidationResult;

    public static ValidationResult validate(ValidationContext validationContext, JsonNode archiveTypeDescriptor) {

        try (ImportClassLoader importClassLoader = generateImportClassLoader(validationContext)) {
            return AvroSchemaValidator.builder()
                    .validationContext(validationContext)
                    .archiveTypeDescriptor(archiveTypeDescriptor)
                    .idlFileParser(new IdlFileParser(importClassLoader))
                    .build()
                    .validate();
        } catch (IOException e) {
            return ValidationResult.fail("Cannot import files: " + e.getMessage());
        }
    }

    private static ImportClassLoader generateImportClassLoader(ValidationContext validationContext) {
        File commonRootDir = new File(validationContext.getDescriptorDir(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File commonSystemDir = new File(validationContext.getSystemDir(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        return new ImportClassLoader(validationContext.getImportClassLoader(), commonRootDir, commonSystemDir);

    }

    private ValidationResult validate() {
        schemaValidationResult = ValidationResult.ok();
        String archiveTypeName = archiveTypeDescriptor.get("archiveType").textValue();
        List<ArchiveTypeVersion> archiveTypeVersions = getVersions()
                .flatMap(versionNode -> loadSchema(
                        archiveTypeName,
                        versionNode.get("version").asInt(),
                        textNode(versionNode.get("compatibilityMode")),
                        intNode(versionNode.get("compatibleVersion")),
                        versionNode.get("schema").asText()).stream())
                .sorted()
                .collect(toList());

        validateUniqueNamespacePerVersion(archiveTypeVersions);
        validateSchemaCompatibility(archiveTypeVersions);

        return schemaValidationResult;
    }

    private String textNode(JsonNode node) {
        return node == null ? null : node.asText();
    }

    private int intNode(JsonNode node) {
        return node == null ? 0 : node.asInt(0);
    }

    private void validateUniqueNamespacePerVersion(List<ArchiveTypeVersion> archiveTypeVersions) {
        List<String> namespaces = archiveTypeVersions.stream()
                .map(atv -> atv.avroSchema().getNamespace())
                .filter(Objects::nonNull)
                .toList();

        Set<String> uniqueNamespaces = new HashSet<>(namespaces);
        if (namespaces.size() != uniqueNamespaces.size()) {
            String message = format(
                    """
                            Schema versions in descriptor %s do not use a separate namespace per version (i.e. .v1, .v2, ...) -\
                             generated classes would collide.
                            Namespaces: %s\
                            """,
                    validationContext.getDescriptor(), namespaces);
            schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(message));
        }
    }

    private void validateSchemaCompatibility(List<ArchiveTypeVersion> archiveTypeVersions) {
        // Skip index 0, the first version cannot be validated to be compatible with anything
        for (int i = 1; i < archiveTypeVersions.size(); i++) {
            ArchiveTypeVersion archiveTypeVersion = archiveTypeVersions.get(i);
            int compatibleVersionNumber;
            if (archiveTypeVersion.compatibleVersion() > 0) {
                compatibleVersionNumber = archiveTypeVersion.compatibleVersion();
            } else {
                compatibleVersionNumber = archiveTypeVersions.get(i - 1).version();
            }
            ArchiveTypeVersion compatibleVersion = archiveTypeVersions.stream()
                    .filter(v -> v.version() == compatibleVersionNumber)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "%s version %d not found - cannot verify compatibility".formatted(validationContext.getArchiveTypeName(), compatibleVersionNumber)));

            validateSchemaCompatibility(archiveTypeVersion, compatibleVersion);
        }
    }

    private void validateSchemaCompatibility(ArchiveTypeVersion archiveTypeVersion, ArchiveTypeVersion compatibleVersion) {
        Schema currentSchema = archiveTypeVersion.avroSchema().getType(validationContext.getArchiveTypeName());
        Schema compatibleSchema = compatibleVersion.avroSchema().getType(validationContext.getArchiveTypeName());
        ValidationResult result = ValidationResult.ok();
        if (archiveTypeVersion.compatibilityMode().isBackwardOrFull()) {
            result = SchemaCompatibility.checkReaderWriterCompatibility(currentSchema, compatibleSchema)
                    .getResult()
                    .getIncompatibilities()
                    .stream()
                    .map(s -> "Schemas %s version %s and %s are not backward compatible: %s".formatted(
                            validationContext.getArchiveTypeName(), archiveTypeVersion.version(), compatibleVersion.version(), s))
                    .map(ValidationResult::fail)
                    .reduce(result, ValidationResult::merge);
        }

        if (archiveTypeVersion.compatibilityMode().isForwardOrFull()) {
            result = SchemaCompatibility.checkReaderWriterCompatibility(compatibleSchema, currentSchema)
                    .getResult()
                    .getIncompatibilities()
                    .stream()
                    .map(s -> "Schemas %s version %s and %s are not forward compatible: %s".formatted(
                            validationContext.getArchiveTypeName(), archiveTypeVersion.version(), compatibleVersion.version(), s))
                    .map(ValidationResult::fail)
                    .reduce(result, ValidationResult::merge);
        }
        schemaValidationResult = ValidationResult.merge(schemaValidationResult, result);
    }

    private Optional<ArchiveTypeVersion> loadSchema(String archiveTypeName,
                                                    int version,
                                                    String compatibilityMode,
                                                    int compatibleVersion,
                                                    String filename) {
        File schemaFile = getSchemaFile(filename);
        if (schemaFile == null) {
            String archiveTypeDir = validationContext.getArchiveTypeDir().getAbsolutePath();
            String message = format("Cannot find schema file '%s' in archive type '%s'", filename, archiveTypeDir);
            schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(message));
            return Optional.empty();
        }

        try {
            Protocol protocol = validateSchemaParsesSuccessfully(schemaFile);
            String fqn = protocol.getNamespace() + "." + archiveTypeName;
            if (protocol.getType(fqn) == null) {
                String typeNames = protocol.getTypes().stream()
                        .map(Schema::getFullName)
                        .collect(Collectors.joining(","));
                String msg = format("""
                        Protocol in schema file %s does not contain a schema for a type that matches the \
                        archive type name %s. Contained types: %s\
                        """, schemaFile, fqn, typeNames);
                schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(msg));
            }
            return Optional.of(new ArchiveTypeVersion(version, CompatibilityMode.valueOfNullSafe(compatibilityMode), compatibleVersion, protocol));
        } catch (Exception e) {
            String message = format("Cannot compile file '%s' (%s): %s", schemaFile.getAbsolutePath(), e.getClass(), e.getMessage());
            schemaValidationResult = ValidationResult.merge(schemaValidationResult, ValidationResult.fail(message));
            return Optional.empty();
        }
    }

    private File getSchemaFile(String filename) {
        File inArchiveTypeDir = new File(validationContext.getArchiveTypeDir(), filename);
        if (inArchiveTypeDir.exists()) {
            return inArchiveTypeDir;
        }
        File commonSystemDir = new File(validationContext.getSystemDir(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File inCommonSystemDir = new File(commonSystemDir, filename);
        if (inCommonSystemDir.exists()) {
            return inCommonSystemDir;
        }
        File commonRootDir = new File(validationContext.getDescriptorDir(), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File inCommonRootDir = new File(commonRootDir, filename);
        if (inCommonRootDir.exists()) {
            return inCommonRootDir;
        }
        return null;
    }

    private Protocol validateSchemaParsesSuccessfully(File file) throws Exception {
        if (file.getName().endsWith("avdl")) {
            return idlFileParser.parseIdlFile(file);
        } else {
            String message = format("File '%s' has an invalid ending", file.getAbsolutePath());
            throw new RuntimeException(message);
        }
    }

    private Stream<JsonNode> getVersions() {
        JsonNode versionsNode = archiveTypeDescriptor.get("versions");
        if (versionsNode == null) {
            return Stream.of();
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(versionsNode.elements(), 0), false);
    }
}
