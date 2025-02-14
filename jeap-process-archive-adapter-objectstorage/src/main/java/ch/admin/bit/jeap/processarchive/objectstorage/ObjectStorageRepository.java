package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;

import java.util.Map;
import java.util.Optional;

public interface ObjectStorageRepository {

    /**
     * Put the given object payload in the given bucket under the given key with the given metadata.
     * A lifecycle configuration will be created based on the given lifecycle policy, to delete data when it expires.
     *
     * @return The version id assigned to the object by the object store.
     */
    String putObjectWithLifecyclePolicy(String bucketName,
                                        String objectKey,
                                        byte[] objectPayload,
                                        Map<String, String> metadata,
                                        LifecyclePolicy lifecyclePolicy,
                                        ArchiveDataEncryption encryption);

    /**
     * Put the given object payload in the given bucket under the given key with the given metadata.
     *
     * @return The version id assigned to the object by the object store.
     */
    String putObject(String bucketName, String objectKey, byte[] objectPayload, Map<String, String> metadata);

    /**
     * @return StorageObjectProperties if the object exists
     */
    Optional<StorageObjectProperties> getObjectProperties(String bucketName, String objectKey);
}
