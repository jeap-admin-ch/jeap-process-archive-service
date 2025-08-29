package ch.admin.bit.jeap.processarchive.registry.verifier.system;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.FileNotChangedValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.archivetype.ArchiveTypeValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemValidator {
    private final static List<String> allowedSchemaSuffix = List.of("avdl");
    private final File systemDirectory;
    private final String systemName;
    private final List<String> archiveTypes = new LinkedList<>();

    private final ValidationContext validationContext;

    public static ValidationResult validate(ValidationContext validationContext) {
        SystemValidator systemValidator = new SystemValidator(
                validationContext.getSystemDir(),
                validationContext.getSystemName(),
                validationContext);

        ValidationResult result = ValidationResult.merge(
                systemValidator.checkDirectory(),
                systemValidator.checkSystemName(),
                systemValidator.checkSubDirs(),
                systemValidator.checkCommonSystemDir(),
                FileNotChangedValidator.noMessageTypeDeleted(validationContext),
                FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext)
        );

        //If dir is not valid itself, it makes no sense to check its content
        if (!result.isValid()) {
            return result;
        }

        return systemValidator.checkArchiveTypeDescriptors();
    }

    private ValidationResult checkDirectory() {
        String absolutePath = systemDirectory.getAbsolutePath();
        if (!systemDirectory.isDirectory()) {
            String message = String.format("File '%s' is not a directory, but a system has to be", absolutePath);
            return ValidationResult.fail(message);
        }
        if (!systemDirectory.canRead()) {
            String message = String.format("File '%s' is not readable", absolutePath);
            return ValidationResult.fail(message);
        }

        return ValidationResult.ok();
    }

    private ValidationResult checkSystemName() {
        String absolutePath = systemDirectory.getAbsolutePath();
        if (!systemName.toLowerCase().equals(systemName)) {
            String message = String.format("System name '%s' in directory '%s' must be lowercase but is not",
                    systemName, absolutePath);
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private ValidationResult checkSubDirs() {
        archiveTypes.clear();

        String[] subDirs = systemDirectory.list();
        if (subDirs == null) {
            return ValidationResult.ok();
        }
        return Arrays.stream(subDirs)
                .filter(f -> !ArchiveTypeRegistryConstants.COMMON_DIR_NAME.equals(f))
                .map(f -> new File(systemDirectory, f))
                .map(this::checkArchiveTypeSubDir)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkArchiveTypeSubDir(File dir) {
        ValidationResult result = checkSubDirIsReadable(dir);
        return result.isValid() ? checkArchiveTypeSubdirContent(dir) : result;
    }

    private ValidationResult checkSubDirIsReadable(File dir) {
        if (!dir.isDirectory()) {
            String message = String.format("File '%s' was expected to be an archive type directory but is a file",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        if (!dir.canRead()) {
            String message = String.format("File '%s' is not readable",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        if (!dir.getName().equals(dir.getName().toLowerCase())) {
            String message = String.format("Archive type directory '%s' must be all lower case",
                    dir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    private ValidationResult checkArchiveTypeSubdirContent(File subDir) {
        //Get the archive type descriptor and its name
        Optional<String> nameOpt = getArchiveTypeName(subDir);
        if (nameOpt.isEmpty()) {
            String message = String.format("Directory '%s' does not contain an archive type descriptor with the correct name",
                    subDir.getAbsolutePath());
            return ValidationResult.fail(message);
        }
        String descriptorName = nameOpt.get();
        archiveTypes.add(descriptorName);

        //Check that there are no illegal files
        IOFileFilter notAllowedFileFilter = getNotAllowedFilesFilter(descriptorName);
        String[] notAllowedFiles = Objects.requireNonNull(subDir.list(notAllowedFileFilter));
        return Arrays.stream(notAllowedFiles)
                .map(f -> new File(subDir, f))
                .map(f -> String.format("File '%s' is not allowed, only one descriptor and some schemas",
                        f.getAbsolutePath()))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private Optional<String> getArchiveTypeName(File subDir) {
        //Check if it has a single descriptor
        FilenameFilter jsonFilter = new SuffixFileFilter(".json");
        String[] jsonFileNames = subDir.list(jsonFilter);
        if (jsonFileNames == null) {
            return Optional.empty();
        }
        return Arrays.stream(jsonFileNames)
                .map(FilenameUtils::removeExtension)
                .filter(f -> f.toLowerCase().equals(subDir.getName()))
                .findAny();
    }

    private IOFileFilter getNotAllowedFilesFilter(String descriptorName) {
        IOFileFilter allowedFilesFilter = new OrFileFilter(List.of(new RegexFileFilter(descriptorName + ".json"),
                schemaFilter(descriptorName)));
        return new NotFileFilter(allowedFilesFilter);
    }

    private IOFileFilter schemaFilter(String descriptorName) {
        IOFileFilter version = allowedSchemaSuffix.stream()
                .map(s -> descriptorName + "_v\\d+." + s)
                .map(RegexFileFilter::new)
                .map(f -> (IOFileFilter) f)
                .reduce(new OrFileFilter(), OrFileFilter::new);
        return new OrFileFilter(List.of(version));
    }

    private ValidationResult checkArchiveTypeDescriptors() {
        return archiveTypes.stream()
                .map(this::checkArchiveTypeDescriptor)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult checkArchiveTypeDescriptor(String descriptorName) {
        File archiveTypeDir = new File(systemDirectory, descriptorName.toLowerCase());
        ValidationContext subValidationContext = validationContext.toBuilder()
                .archiveTypeDir(archiveTypeDir)
                .descriptor(new File(archiveTypeDir, descriptorName + ".json"))
                .archiveTypeName(descriptorName)
                .build();
        return ArchiveTypeValidator.validate(subValidationContext);
    }

    private ValidationResult checkCommonSystemDir() {
        return SystemCommonDirValidator.builder()
                .validationContext(validationContext)
                .validate();
    }
}
