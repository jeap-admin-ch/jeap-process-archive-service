package ch.admin.bit.jeap.processarchive.registry.repository;

import java.io.File;

class RepositoryException extends RuntimeException {
    private RepositoryException(String message) {
        super(message);
    }

    private RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    static RepositoryException schemaValidationFailed(File definitionFile, String errors) {
        return new RepositoryException(
                "Archive type definition reference file " + definitionFile + " does not conform to schema: " + errors);
    }

    static RepositoryException cannotReadFile(File definitionFile, Exception exception) {
        return new RepositoryException(
                "Unable to read archive type definition reference file at " + definitionFile,
                exception);
    }

    static RepositoryException unableToClone(Exception cause) {
        return new RepositoryException("Unable to clone archive type registry repository", cause);
    }

    static RepositoryException missingGitHistoryReference() {
        return new RepositoryException("Missing git history reference in schema file");
    }

    static RepositoryException ambiguousGitHistoryReference() {
        return new RepositoryException("Ambiguous git history reference in schema file, both branch and commit are specified");
    }
}
