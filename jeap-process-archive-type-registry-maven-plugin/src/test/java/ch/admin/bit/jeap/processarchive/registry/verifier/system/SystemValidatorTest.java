package ch.admin.bit.jeap.processarchive.registry.verifier.system;

import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class SystemValidatorTest {
    private final String systemName = "test";
    private final String archiveTypeFolder = "decree";
    private final String archiveTypeName = "Decree";
    private final String archiveTypeDescriptorContent = """
            { "archiveType": "Decree",\
             "system": "Test",\
             "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
             "description": "archive type example for a decree",\
             "documentationUrl": "http://foo/bar",\
             "expirationDays": 1,\
             "versions": [] }\
            """;

    @Test
    void valid(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, archiveTypeFolder);
        FileUtils.forceMkdir(archiveTypeDir);
        File archiveTypeDescriptor = new File(archiveTypeDir, archiveTypeName + ".json");
        FileUtils.write(archiveTypeDescriptor, archiveTypeDescriptorContent, UTF_8);
        File file = new File(archiveTypeDir, archiveTypeName + "_v1.avdl");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void uppercaseName(@TempDir File tmpDir) {
        String systemName = "Test";
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Uppercase not allowed in system name");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("must be lowercase but is not"), () -> result.getErrors().toString());
    }

    @Test
    void wrongFileInSystem(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "wrongFile.txt");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Wrong file in dir");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("wrongFile.txt' was expected to be an archive type directory but is a file"),
                () -> result.getErrors().toString());
    }

    @Test
    void subDirWithoutArchiveTypeDescriptor(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, archiveTypeFolder);
        FileUtils.forceMkdir(archiveTypeDir);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Sub dir without archive type descriptor");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("does not contain an archive type descriptor with the correct name"),
                () -> result.getErrors().toString());
    }

    @Test
    void descriptorButNoSchemas(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, archiveTypeFolder);
        FileUtils.forceMkdir(archiveTypeDir);
        File archiveTypeDescriptor = new File(archiveTypeDir, archiveTypeName + ".json");
        FileUtils.write(archiveTypeDescriptor, archiveTypeDescriptorContent, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void wrongFile(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, archiveTypeFolder);
        FileUtils.forceMkdir(archiveTypeDir);
        File archiveTypeDescriptor = new File(archiveTypeDir, archiveTypeName + ".json");
        FileUtils.write(archiveTypeDescriptor, archiveTypeDescriptorContent, UTF_8);
        File file = new File(archiveTypeDir, "wrongFile.txt");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Wrong file in dir");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("decree/wrongFile.txt' is not allowed, only one descriptor and some schemas"),
                () -> result.getErrors().toString());
    }

    @Test
    void wrongSchemaName(@TempDir File tmpDir) throws IOException {
        File archiveTypeDir = new File(tmpDir, archiveTypeFolder);
        FileUtils.forceMkdir(archiveTypeDir);
        File file = new File(archiveTypeDir, "schema_v1.avdl");
        FileUtils.touch(file);
        File archiveTypeDescriptor = new File(archiveTypeDir, archiveTypeName + ".json");
        FileUtils.write(archiveTypeDescriptor, archiveTypeDescriptorContent, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .systemName(systemName)
                .archiveTypeName(archiveTypeName)
                .systemDir(tmpDir)
                .build();

        ValidationResult result = SystemValidator.validate(validationContext);

        assertFalse(result.isValid(), "Invalid schema descriptor");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("decree/schema_v1.avdl' is not allowed, only one descriptor and some schemas"),
                () -> result.getErrors().toString());
    }
}
