package ch.admin.bit.jeap.processarchive.registry.verifier;

import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class DescriptorDirectoryValidatorTest {

    @Test
    void descriptorDirectoryNotExists(@TempDir File tmpDir) {
        File descriptorDir = new File(tmpDir, "archive-types");
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(descriptorDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        assertFalse(result.isValid(), "Must fail if no descriptor directory");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("/archive-types' does not exist"),
                () -> result.getErrors().toString());
    }

    @Test
    void descriptorDirectoryEmpty(@TempDir File tmpDir) {
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void fileInDir(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.touch(file);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        assertFalse(result.isValid(), "File in descriptor dir not allowed");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("/test' is not a directory, but a system has to be"),
                () -> result.getErrors().toString());

    }

    @Test
    void uppercaseDir(@TempDir File tmpDir) throws IOException {
        File dir = new File(tmpDir, "Test");
        FileUtils.forceMkdir(dir);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        assertFalse(result.isValid(), "Uppercase not allowed in system name");
        assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
        assertTrue(result.getErrors().get(0).endsWith("/Test' must be lowercase but is not"),
                () -> result.getErrors().toString());
    }

    @Test
    void valid(@TempDir File tmpDir) throws IOException {
        File dir = new File(tmpDir, "test");
        FileUtils.forceMkdir(dir);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptorDir(tmpDir)
                .oldDescriptorDir(tmpDir)
                .build();

        ValidationResult result = DescriptorDirectoryValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
