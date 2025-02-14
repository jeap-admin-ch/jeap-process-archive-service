package ch.admin.bit.jeap.processarchive.registry.verifier;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class FileNotChangedValidatorTest {
    private File oldCommon;
    private File newCommon;
    private File newSystem;
    private File oldArchiveType;
    private File newArchiveType;
    private File oldCommonSystem;
    private File newCommonSystem;
    private ValidationContext validationContext;

    @BeforeEach
    void setup(@TempDir File tmpDir) throws IOException {
        File oldDescriptor = new File(tmpDir, "old");
        FileUtils.forceMkdir(oldDescriptor);
        File newDescriptor = new File(tmpDir, "new");
        FileUtils.forceMkdir(newDescriptor);
        validationContext = ValidationContext.builder()
                .oldDescriptorDir(oldDescriptor)
                .descriptorDir(newDescriptor)
                .systemName("test")
                .archiveTypeName("decree")
                .build();
        oldCommon = new File(oldDescriptor, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        newCommon = new File(newDescriptor, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File oldSystem = new File(oldDescriptor, "test");
        FileUtils.forceMkdir(oldSystem);
        newSystem = new File(newDescriptor, "test");
        FileUtils.forceMkdir(newSystem);
        oldCommonSystem = new File(oldSystem, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        newCommonSystem = new File(newSystem, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        oldArchiveType = new File(oldSystem, "decree");
        FileUtils.forceMkdir(oldArchiveType);
        newArchiveType = new File(newSystem, "decree");
        FileUtils.forceMkdir(newArchiveType);
    }

    @Test
    void commonFolderNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFolderCreated() throws IOException {
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertFalse(validationResult.isValid(), "Common folder cannot be created");
        assertEquals("Common dir does not exist in master but does in this branch. You are not allowed to add a common dir on root level",
                validationResult.getErrors().get(0),
                msg(validationResult));
    }

    @Test
    void commonFolderDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertFalse(validationResult.isValid(), "Common folder cannot be deleted");
        assertEquals("Common dir does exist in master but does not in this branch. You are not allowed to delete common dir on root level",
                validationResult.getErrors().get(0));
    }

    @Test
    void commonFoldersEmpty() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(newCommon, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertFalse(validationResult.isValid(), "File cannot be crated in common folder");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "_common/test does not exist master but does  exist in this branch. You are not allowed to create this file"),
                msg(validationResult));
    }

    @Test
    void commonFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "_common/test exists in master but does not exist in this branch. You are not allowed to delete this file"),
                msg(validationResult));
    }

    @Test
    void commonFoldersExistingFile() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi", UTF_8);
        FileUtils.write(new File(newCommon, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi", UTF_8);
        FileUtils.write(new File(newCommon, "test"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "_common/test has changed compared to master. You are not allowed to change this file"),
                msg(validationResult));
    }

    @Test
    void commonSystemFoldersNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertFalse(validationResult.isValid(), "Cannot delete common system folder");
        assertEquals("Common dir for system test exist in master but does in this branch. You are not allowed to delete a common dir",
                validationResult.getErrors().get(0),
                msg(validationResult));
    }

    @Test
    void commonSystemFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(newCommonSystem, "test"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertFalse(validationResult.isValid(), "Cannot delete file in common system folder");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "_common/test exists in master but does not exist in this branch. You are not allowed to delete this file"),
                msg(validationResult));
    }

    @Test
    void commonSystemFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "hi", UTF_8);
        FileUtils.write(new File(newCommonSystem, "test"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        assertFalse(validationResult.isValid(), "Cannot change files in common system folder");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "test/_common/test has changed compared to master. You are not allowed to change this file"),
                msg(validationResult));
    }

    @Test
    void systemDeleted() throws IOException {
        FileUtils.deleteDirectory(newSystem);

        ValidationResult validationResult = FileNotChangedValidator.noSystemDeleted(validationContext);

        assertFalse(validationResult.isValid(), "Cannot delete system");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "test exists in master but does not exist in this branch. You are not allowed to delete this file"),
                msg(validationResult));
    }

    @Test
    void archiveTypeDeleted() throws IOException {
        FileUtils.deleteDirectory(newArchiveType);

        ValidationResult validationResult = FileNotChangedValidator.noMessageTypeDeleted(validationContext);

        assertFalse(validationResult.isValid(), "Cannot delete archive type");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "test/decree exists in master but does not exist in this branch. You are not allowed to delete this file"),
                msg(validationResult));
    }

    @Test
    void schemaDeleted() throws IOException {
        FileUtils.write(new File(oldArchiveType, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        assertFalse(validationResult.isValid(), "Cannot delete schema");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "test/decree/test exists in master but does not exist in this branch. You are not allowed to delete this file"),
                msg(validationResult));
    }

    @Test
    void schemaCreated() throws IOException {
        FileUtils.write(new File(newArchiveType, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void existingSchema() throws IOException {
        FileUtils.write(new File(newArchiveType, "test"), "hi", UTF_8);
        FileUtils.write(new File(oldArchiveType, "test"), "hi", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void schemaChanged() throws IOException {
        FileUtils.write(new File(newArchiveType, "test"), "hi", UTF_8);
        FileUtils.write(new File(oldArchiveType, "test"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        assertFalse(validationResult.isValid(), "Cannot change schema");
        assertTrue(validationResult.getErrors().get(0).endsWith(
                        "test/decree/test has changed compared to master. You are not allowed to change this file"),
                msg(validationResult));
    }

    @Test
    void descriptorChanged() throws IOException {
        FileUtils.write(new File(newArchiveType, "test.json"), "hi", UTF_8);
        FileUtils.write(new File(oldArchiveType, "test.json"), "other", UTF_8);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    private static Supplier<String> msg(ValidationResult validationResult) {
        return () -> validationResult.getErrors().toString();
    }
}
