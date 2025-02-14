package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeVersion;

import java.io.File;
import java.util.stream.Collectors;

public class RegistryException extends RuntimeException {
    private RegistryException(String message) {
        super(message);
    }

    private RegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    static RegistryException cannotReadSchemaFile(Throwable cause) {
        String message = String.format("Cannot read reference schema file '%s'", RegistryService.REFERENCES_SCHEMA_FILE);
        return new RegistryException(message, cause);
    }

    static RegistryException cannotReadFile(File archiveTypeReferencesFile, Throwable cause) {
        String message = String.format("Cannot read reference file '%s'", archiveTypeReferencesFile.getAbsolutePath());
        return new RegistryException(message, cause);
    }

    static RegistryException cannotDownloadDescriptor(ArchiveTypeReference archiveTypeReference, Throwable cause) {
        String message = String.format("Cannot download type descriptor for archive type '%s'", archiveTypeReference.getName());
        return new RegistryException(message, cause);
    }

    static RegistryException cannotDownloadSchema(ArchiveTypeReference archiveTypeReference, String filename, Throwable cause) {
        String message = String.format("Cannot download schema '%s' for archive type '%s'", filename, archiveTypeReference.getName());
        return new RegistryException(message, cause);
    }

    static RegistryException invalidVersion(ArchiveTypeReference archiveTypeReference, ArchiveTypeDescriptor typeDescriptor) {
        String versions = typeDescriptor.getVersions().stream()
                .map(ArchiveTypeVersion::getVersion)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        String message = String.format("Version '%s' of type '%s' not found. Present are {'%s'}",
                archiveTypeReference.getVersion(), archiveTypeReference.getName(), versions);
        return new RegistryException(message);
    }

    static RegistryException schemaValidationFailed(File archiveTypeReferencesFile, String errorMessage) {
        String message = String.format("File '%s' is not a valid references file: %s",
                archiveTypeReferencesFile.getAbsolutePath(), errorMessage);
        return new RegistryException(message);
    }

    static RegistryException missingGitHistoryReference() {
        return new RegistryException("Missing git history reference in schema file");
    }

    static RegistryException ambiguousGitHistoryReference() {
        return new RegistryException("Ambiguous git history reference in schema file, both branch and commit are specified");
    }
}
