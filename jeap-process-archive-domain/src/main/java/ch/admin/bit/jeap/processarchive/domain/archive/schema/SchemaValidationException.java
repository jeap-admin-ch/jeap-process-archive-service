package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

import static java.lang.String.format;

public class SchemaValidationException extends RuntimeException {

    private SchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    private SchemaValidationException(String message) {
        super(message);
    }

    public static SchemaValidationException schemaValidationFailed(ArchiveData archiveData, Exception ex) {
        String msg = format("Failed to validate archive data %s against schema %s version %d",
                archiveData.getReferenceId(), archiveData.getSchema(), archiveData.getSchemaVersion());
        return new SchemaValidationException(msg, ex);
    }

    public static SchemaValidationException schemaValidationFailed(ArchiveData archiveData, String message) {
        String msg = format("Failed to validate archive data %s against schema %s version %d: %s",
                archiveData.getReferenceId(), archiveData.getSchema(), archiveData.getSchemaVersion(), message);
        return new SchemaValidationException(msg);
    }

    public static SchemaValidationException noValidatorForContentType(ArchiveData archiveData, String contentType) {
        String msg = format("""
                        Failed to validate archive data %s against a schema: Unsupported content type %s, \
                        no validator is available for this content type\
                        """,
                archiveData.getReferenceId(), contentType);
        return new SchemaValidationException(msg);
    }
}
