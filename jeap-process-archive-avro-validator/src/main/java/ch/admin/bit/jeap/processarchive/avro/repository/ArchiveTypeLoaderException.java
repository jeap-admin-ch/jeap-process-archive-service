package ch.admin.bit.jeap.processarchive.avro.repository;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.String.format;

public class ArchiveTypeLoaderException extends RuntimeException {

    private ArchiveTypeLoaderException(String message) {
        super(message);
    }

    private ArchiveTypeLoaderException(String message, Exception exception) {
        super(message, exception);
    }

    static ArchiveTypeLoaderException jsonParsingFailed(Path descriptorPath, IOException e) {
        return new ArchiveTypeLoaderException("Failed to load descriptor at " + descriptorPath, e);
    }

    static ArchiveTypeLoaderException schemaFileNotFound(Path descriptorDir, String filename) {
        return new ArchiveTypeLoaderException(format("Schema file %s referenced in descriptor %s not found",
                filename, descriptorDir));
    }

    static ArchiveTypeLoaderException avroSchemaParsingFailed(Path schemaFile, Exception exception) {
        return new ArchiveTypeLoaderException("Failed to parse schema file " + schemaFile, exception);
    }

    static ArchiveTypeLoaderException archiveTypeNotFound(ArchiveTypeId archiveTypeId) {
        return new ArchiveTypeLoaderException("Archive type unknown: " + archiveTypeId);
    }
}
