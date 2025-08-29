package ch.admin.bit.jeap.processarchive.registry.verifier;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class FileNotChangedValidator {
    static ValidationResult noSystemDeleted(ValidationContext validationContext) {
        File newDescriptorFolder = descriptorDir(validationContext, false);
        File oldDescriptorFolder = descriptorDir(validationContext, true);

        return Arrays.stream(Objects.requireNonNullElse(oldDescriptorFolder.list(), new String[0]))
                .map(filename -> fileNotDeleted(newDescriptorFolder, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    public static ValidationResult noMessageTypeDeleted(ValidationContext validationContext) {
        File newSystemDir = systemDir(validationContext, false);
        File oldSystemDir = systemDir(validationContext, true);

        if (!oldSystemDir.exists()) {
            //This is a new system, definitely no archive type deleted
            return ValidationResult.ok();
        }

        return Arrays.stream(Objects.requireNonNullElse(oldSystemDir.list(), new String[0]))
                .map(filename -> fileNotDeleted(newSystemDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    static ValidationResult commonRootDirNotChanged(ValidationContext validationContext) {
        File newCommonRootDir = new File(descriptorDir(validationContext, false), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File oldCommonRootDir = new File(descriptorDir(validationContext, true), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);

        if (!oldCommonRootDir.exists() && !newCommonRootDir.exists()) {
            //There is no common dir, this is fine
            return ValidationResult.ok();
        }

        if (!oldCommonRootDir.exists()) {
            String message = "Common dir does not exist in master but does in this branch. You are not allowed to add a common dir on root level";
            return ValidationResult.fail(message);
        }

        if (!newCommonRootDir.exists()) {
            String message = "Common dir does exist in master but does not in this branch. You are not allowed to delete common dir on root level";
            return ValidationResult.fail(message);
        }

        ValidationResult filesCreated = Arrays.stream(Objects.requireNonNullElse(newCommonRootDir.list(), new String[0]))
                .map(filename -> fileExistedBefore(oldCommonRootDir, newCommonRootDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);

        ValidationResult filesChanged = Arrays.stream(Objects.requireNonNullElse(oldCommonRootDir.list(), new String[0]))
                .map(filename -> fileNotChanged(oldCommonRootDir, newCommonRootDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);

        return ValidationResult.merge(filesCreated, filesChanged);
    }

    public static ValidationResult commonSystemDirNoFilesChanged(ValidationContext validationContext) {
        File newCommonSystemDir = new File(systemDir(validationContext, false), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        File oldCommonSystemDir = new File(systemDir(validationContext, true), ArchiveTypeRegistryConstants.COMMON_DIR_NAME);

        if (!oldCommonSystemDir.exists()) {
            //This is a new common dir, definitely no schema deleted
            return ValidationResult.ok();
        }

        if (!newCommonSystemDir.exists()) {
            String message = String.format("""
                    Common dir for system %s exist in master but does in this branch.\
                     You are not allowed to delete a common dir\
                    """, validationContext.getSystemName());
            return ValidationResult.fail(message);
        }

        return Arrays.stream(Objects.requireNonNullElse(oldCommonSystemDir.list(), new String[0]))
                .map(filename -> fileNotChanged(oldCommonSystemDir, newCommonSystemDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    public static ValidationResult noExistingSchemasChanged(ValidationContext validationContext) {
        File newArchiveTypeDir = archiveTypeDir(validationContext, false);
        File oldArchiveTypeDir = archiveTypeDir(validationContext, true);

        if (!oldArchiveTypeDir.exists()) {
            //This is a new archive type, definitely no schema deleted
            return ValidationResult.ok();
        }

        return Arrays.stream(Objects.requireNonNullElse(oldArchiveTypeDir.list(), new String[0]))
                //Archive type descriptors can change
                .filter(filename -> !filename.endsWith("json"))
                .map(filename -> fileNotChanged(oldArchiveTypeDir, newArchiveTypeDir, filename))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private static ValidationResult fileExistedBefore(File oldFolder, File newFolder, String filename) {
        File newFile = new File(newFolder, filename);
        File oldFile = new File(oldFolder, filename);
        if (!oldFile.exists()) {
            String message = String.format("""
                    File %s does not exist master but does  exist in this branch. \
                    You are not allowed to create this file\
                    """, newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private static ValidationResult fileNotChanged(File oldFolder, File newFolder, String filename) {
        File newFile = new File(newFolder, filename);
        File oldFile = new File(oldFolder, filename);

        if (!newFile.exists()) {
            String message = String.format("""
                    File %s exists in master but does not exist in this branch. \
                    You are not allowed to delete this file\
                    """, newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        try {
            if (FileUtils.checksumCRC32(newFile) != FileUtils.checksumCRC32(oldFile)) {
                String message = String.format("""
                        File %s has changed compared to master. \
                        You are not allowed to change this file\
                        """, newFile.getAbsolutePath());
                return ValidationResult.fail(message);
            }
        } catch (IOException e) {
            String message = String.format("Could not compute checksum: %s", e.getMessage());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private static ValidationResult fileNotDeleted(File newFolder, String filename) {
        File newFile = new File(newFolder, filename);

        if (!newFile.exists()) {
            String message = String.format("""
                    File %s exists in master but does not exist in this branch. \
                    You are not allowed to delete this file\
                    """, newFile.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private static File descriptorDir(ValidationContext validationContext, boolean old) {
        return old ? validationContext.getOldDescriptorDir() : validationContext.getDescriptorDir();
    }

    private static File systemDir(ValidationContext validationContext, boolean old) {
        return new File(descriptorDir(validationContext, old), validationContext.getSystemName());
    }

    private static File archiveTypeDir(ValidationContext validationContext, boolean old) {
        File systemDir = systemDir(validationContext, old);
        return new File(systemDir, validationContext.getArchiveTypeName().toLowerCase());
    }
}
