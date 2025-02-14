package ch.admin.bit.jeap.processarchive.plugin.api.storage;

/**
 * Implementation used to create a hash of the archive data payload for adding it as metadata in the object storage.
 */
public interface HashProvider {

    String hashPayload(byte[] payload);

    String hashStorageObjectId(String referenceId, String referenceIdType);

    default String hashReferenceId(String referenceId, String referenceIdType) {
        return hashStorageObjectId(referenceId, referenceIdType);
    }

}
