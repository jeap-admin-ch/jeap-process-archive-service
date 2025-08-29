package ch.admin.bit.jeap.processarchive.avro.repository;

import org.apache.avro.specific.SpecificRecordBase;

import java.nio.file.Path;

import static java.lang.String.format;

public class ArchiveTypeLoaderException extends RuntimeException {

    private ArchiveTypeLoaderException(String message) {
        super(message);
    }

    private ArchiveTypeLoaderException(String message, Exception exception) {
        super(message, exception);
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

    public static ArchiveTypeLoaderException missingMetadataField(Class<? extends SpecificRecordBase> archiveTypeClass, NoSuchFieldException e) {
        return new ArchiveTypeLoaderException("Missing metadata field in archive type class " + archiveTypeClass.getName(), e);
    }
}
