package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageTarget;

public final class ArchiveDataObjectStoreStorageException extends RuntimeException {

    public ArchiveDataObjectStoreStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchiveDataObjectStoreStorageException(String message) {
        this(message, null);
    }

    public static ArchiveDataObjectStoreStorageException storingFailed(ArchiveData archiveData, String reason, Throwable cause) {
        String msg = String.format("Storing archive data '%s' failed. Reason: %s.", archiveData.getReferenceId(), reason);
        return new ArchiveDataObjectStoreStorageException(msg, cause);
    }

    public static ArchiveDataObjectStoreStorageException storingSchemaFailed(ArchiveDataSchema archiveDataSchema, String reason, Throwable cause) {
        String msg = String.format("Storing archive data schema '%s' failed. Reason: %s.", archiveDataSchema, reason);
        return new ArchiveDataObjectStoreStorageException(msg, cause);
    }

    public static ArchiveDataObjectStoreStorageException existenceCheckingFailed(ObjectStorageTarget target, Throwable cause) {
        String msg = String.format("Checking if object exists at storage target '%s' failed.", target);
        return new ArchiveDataObjectStoreStorageException(msg, cause);
    }

}
