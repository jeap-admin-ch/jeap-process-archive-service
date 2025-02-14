package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.map.LinkedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of the {@link ObjectStorageRepository} interface. Use for test purposes only.
 */
public class InMemoryObjectStorageRepository implements ObjectStorageRepository {

    private final Map<String, LinkedMap<String, StorageObject>> STORAGE = new ConcurrentHashMap<>();

    @Override
    public String putObjectWithLifecyclePolicy(String bucketName,
                                               String objectKey,
                                               byte[] objectPayload,
                                               Map<String, String> metadata,
                                               LifecyclePolicy lifecyclePolicy,
                                               ArchiveDataEncryption encryption) {
        return putObject(bucketName, objectKey, objectPayload, metadata);
    }

    boolean doesObjectExist(String bucketName, String objectKey) {
        return !getAllVersions(bucketName, objectKey).isEmpty();
    }

    StorageObject getStorageObject(String bucketName, String objectKey, String objectVersion) {
        return getAllVersions(bucketName, objectKey).get(objectVersion);
    }

    public Map<String, StorageObject> getAllVersions(String bucketName, String objectKey) {
        return STORAGE.getOrDefault(createKey(bucketName, objectKey), new LinkedMap<>());
    }

    @Override
    public String putObject(String bucketName, String objectKey, byte[] objectPayload, Map<String, String> metadata) {

        final String objectVersionId = UUID.randomUUID().toString();
        final StorageObject storageObject = StorageObject.builder()
                .payload(objectPayload.clone())
                .metadata(Optional.ofNullable(metadata).map(HashMap::new).orElse(new HashMap<>()))
                .build();
        STORAGE.computeIfAbsent(createKey(bucketName, objectKey), k -> new LinkedMap<>())
                .put(objectVersionId, storageObject);

        return objectVersionId;
    }

    @Override
    public Optional<StorageObjectProperties> getObjectProperties(String bucketName, String objectKey) {
        LinkedMap<String, StorageObject> versions = STORAGE.getOrDefault(createKey(bucketName, objectKey), new LinkedMap<>());
        if (versions.isEmpty()) {
            return Optional.empty();
        }

        String currentVersionId = versions.lastKey();
        StorageObject currentVersion = versions.get(currentVersionId);
        StorageObjectProperties properties = StorageObjectProperties.builder()
                .versionId(currentVersionId)
                .metadata(currentVersion.getMetadata())
                .build();
        return Optional.of(properties);
    }

    @Value
    @Builder
    public static class StorageObject {
        byte[] payload;
        Map<String, String> metadata;
    }

    private String createKey(String bucketName, String objectKey) {
        return bucketName + objectKey;
    }
}
