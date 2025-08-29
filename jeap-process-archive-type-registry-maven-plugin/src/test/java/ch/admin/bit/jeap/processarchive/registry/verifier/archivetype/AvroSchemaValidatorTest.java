package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.AvroSchemaValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class AvroSchemaValidatorTest {
    private static final JsonNodeFactory factory = JsonNodeFactory.instance;
    private final static File ARCHIVETYPE_RESOURCES_DIR = new File("src/test/resources/valid/archive-types/test/decree/");
    private final static File COMMON_RESOURCES_DIR = new File("src/test/resources/valid/archive-types/test/_common/");

    @Test
    void noVersions() {
        JsonNode jsonNode = new ObjectNode(factory, Map.of(
                "archiveType", new TextNode("Decree")));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void emptyVersions() {
        JsonNode jsonNode = createDescriptor(createVersionsNode());
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void missingSchema(@TempDir File tmpDir) {
        JsonNode jsonNode = createDescriptor(createVersionsNode(1));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .archiveTypeDir(tmpDir)
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertFalse(result.isValid(), "schema_v1.avdl is not present");
    }

    @Test
    void invalidValueSchema(@TempDir File tmpDir) throws IOException {
        FileUtils.write(new File(tmpDir, "schema_v1.avdl"), "garbage", UTF_8);
        JsonNode jsonNode = createDescriptor(createVersionsNode(1));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .archiveTypeDir(tmpDir)
                .importClassLoader(new ImportClassLoader())
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertFalse(result.isValid(), "schema_v1.avdl is not valid");
    }

    @Test
    void validSchemas(@TempDir File tmpDir) throws IOException {
        FileUtils.copyFile(new File(ARCHIVETYPE_RESOURCES_DIR, "Decree_v1.avdl"), new File(tmpDir, "schema_v1.avdl"));
        JsonNode jsonNode = createDescriptor(createVersionsNode(1));
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .archiveTypeDir(tmpDir)
                .importClassLoader(new ImportClassLoader())
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void imports(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.copyFile(
                new File(COMMON_RESOURCES_DIR, "ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"),
                new File(commonSystemDir, "ch.admin.bit.jeap.processarchive.test.DecreeReference.avdl"));

        File archiveTypeDir = new File(tmpDir, "decree");
        FileUtils.forceMkdir(archiveTypeDir);
        FileUtils.copyFile(
                new File(ARCHIVETYPE_RESOURCES_DIR, "Decree_v2.avdl"),
                new File(archiveTypeDir, "Decree_v2.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(createVersionNode(2, "Decree_v2.avdl"));
        JsonNode jsonNode = createDescriptor(versions);

        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .archiveTypeName("Decree")
                .archiveTypeDir(archiveTypeDir)
                .importClassLoader(new ImportClassLoader(new ImportClassLoader(), commonSystemDir, new File("nonexisting")))
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void importNonExisting(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(archiveTypeDir);
        FileUtils.copyFile(new File(ARCHIVETYPE_RESOURCES_DIR, "Decree_v1.avdl"), new File(archiveTypeDir, "Decree_v1.avdl"));

        ArrayNode versions = new ArrayNode(factory);
        versions.add(createVersionNode(1, "Decree_v1.avdl"));
        JsonNode jsonNode = createDescriptor(versions);

        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(new File("test"))
                .archiveTypeName("Decree")
                .archiveTypeDir(tmpDir)
                .importClassLoader(new ImportClassLoader())
                .build();

        ValidationResult result = AvroSchemaValidator.validate(validationContext, jsonNode);

        assertFalse(result.isValid(), "Importing a file that does not exist. This should fail");
        assertEquals(1, result.getErrors().size(),
                () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).startsWith("Cannot find schema file 'Decree_v1.avdl' in archive type"),
                () -> result.getErrors().toString());
    }

    private static JsonNode createVersionsNode(Integer... versions) {
        return Arrays.stream(versions)
                .map(i -> createVersionNode(i, "schema_v" + i + ".avdl"))
                .reduce(new ArrayNode(factory), ArrayNode::add, ArrayNode::addAll);
    }


    private static ObjectNode createVersionNode(int version, String schema) {
        return new ObjectNode(factory, Map.of(
                "version", new IntNode(version),
                "schema", new TextNode(schema)));
    }

    private static ObjectNode createDescriptor(JsonNode versionNode) {
        return new ObjectNode(factory, Map.of(
                "archiveType", new TextNode("Decree"),
                "versions", versionNode));
    }
}
