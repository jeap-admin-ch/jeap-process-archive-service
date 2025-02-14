package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStoreStorageException;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageStrategy;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ArchiveDataObjectStoreAdapter implements ArchiveDataObjectStore {

    public static final String CONTENT_TYPE_METADATA_NAME = "content-type";
    public static final String SYSTEM_NAME = "system";
    public static final String SCHEMA_METADATA_NAME = "schema";
    public static final String SCHEMA_VERSION_METADATA_NAME = "schema-version";
    public static final String REFERENCE_ID_METADATA_NAME = "reference-id";
    public static final String VERSION_METADATA_NAME = "version";
    public static final String HASH_METADATA_NAME = "hash";
    public static final String SCHEMA_LOCATION = "archive-data-schema";
    public static final String SCHEMAFILE_VERSION_ID_METADATA_NAME = "schema-file-version-id";
    public static final String SCHEMAFILE_KEY_METADATA_NAME = "schema-file-key";

    private final ObjectStorageRepository objectStorageRepository;
    private final ObjectStorageStrategy objectStorageStrategy;
    private final LifecyclePolicyService lifecyclePolicyService;
    private final HashProvider hashProvider;

    @Override
    public ArchiveDataStorageInfo store(ArchiveData archiveData, ArchiveDataSchema schema) {

        final ObjectStorageTarget target = objectStorageStrategy.getObjectStorageTarget(archiveData, schema);
        final LifecyclePolicy lifecyclePolicy = lifecyclePolicyService.getLifecyclePolicy(archiveData);
        log.debug("Storage target for archive data '{}' is '{}'.", archiveData.getReferenceId(), target);

        String schemaLocation = String.format("%s/%s_%s_%d.%s",
                SCHEMA_LOCATION, schema.getSystem(), schema.getName(), schema.getVersion(), schema.getFileExtension());
        String schemaVersionId = storeSchema(schema, target.getBucket(), schemaLocation);
        final Map<String, String> metadata = createMetadata(archiveData, schemaVersionId, schemaLocation);
        String versionId = storeObject(archiveData, target, lifecyclePolicy, metadata,
                ArchiveDataEncryption.from(schema));

        return ArchiveDataStorageInfo.builder()
                .bucket(target.getBucket())
                .key(target.getFullObjectName())
                .versionId(versionId)
                .name(target.getName())
                .build();
    }

    private String storeObject(ArchiveData archiveData, ObjectStorageTarget target, LifecyclePolicy lifecyclePolicy,
                               Map<String, String> metadata,
                               ArchiveDataEncryption encryption) {
        try {
            return objectStorageRepository.putObjectWithLifecyclePolicy(target.getBucket(),
                    target.getFullObjectName(),
                    archiveData.getPayload(),
                    metadata,
                    lifecyclePolicy,
                    encryption);
        } catch (Exception e) {
            final String msg = String.format("Error while storing archive data to storage target '%s'.", target);
            throw ArchiveDataObjectStoreStorageException.storingFailed(archiveData, msg, e);
        }
    }

    private String storeSchema(ArchiveDataSchema schema, String bucket, String schemaLocation) {
        try {
            Optional<StorageObjectProperties> objectProperties = objectStorageRepository.getObjectProperties(bucket, schemaLocation);
            if (objectProperties.isEmpty() || shouldStoreSchema(schema, objectProperties.get())) {
                final Map<String, String> metadata = createSchemaMetadata(schema);
                return objectStorageRepository.putObject(bucket,
                        schemaLocation,
                        schema.getSchemaDefinition(),
                        metadata);
            } else {
                return objectProperties.get().getVersionId();
            }
        } catch (Exception e) {
            final String msg = String.format("Error while storing archive data schema in bucket %s at %s", bucket, schemaLocation);
            throw ArchiveDataObjectStoreStorageException.storingSchemaFailed(schema, msg, e);
        }
    }

    private boolean shouldStoreSchema(ArchiveDataSchema schema, StorageObjectProperties objectProperties) {
        return !hashMatches(objectProperties, schema);
    }

    private Map<String, String> createSchemaMetadata(ArchiveDataSchema schema) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(SYSTEM_NAME, schema.getSystem());
        metadata.put(SCHEMA_METADATA_NAME, schema.getName());
        metadata.put(SCHEMA_VERSION_METADATA_NAME, String.valueOf(schema.getVersion()));
        metadata.put(HASH_METADATA_NAME, hashProvider.hashPayload(schema.getSchemaDefinition()));
        return metadata;
    }

    private boolean hashMatches(StorageObjectProperties properties, ArchiveDataSchema schema) {
        String hash = hashProvider.hashPayload(schema.getSchemaDefinition());
        String existingHash = properties.getMetadata().get(HASH_METADATA_NAME);
        return existingHash != null && existingHash.equals(hash);
    }

    private Map<String, String> createMetadata(ArchiveData archiveData, String schemaVersionId, String schemaLocation) {
        Map<String, String> metadata = new HashMap<>(toMap(archiveData.getMetadata()));
        metadata.put(CONTENT_TYPE_METADATA_NAME, archiveData.getContentType());
        metadata.put(SYSTEM_NAME, archiveData.getSystem());
        metadata.put(SCHEMA_METADATA_NAME, archiveData.getSchema());
        metadata.put(SCHEMA_VERSION_METADATA_NAME, String.valueOf(archiveData.getSchemaVersion()));
        metadata.put(SCHEMAFILE_VERSION_ID_METADATA_NAME, schemaVersionId);
        metadata.put(SCHEMAFILE_KEY_METADATA_NAME, schemaLocation);
        metadata.put(REFERENCE_ID_METADATA_NAME, archiveData.getReferenceId());
        if (archiveData.getVersion() != null) {
            metadata.put(VERSION_METADATA_NAME, Integer.toString(archiveData.getVersion()));
        }
        metadata.put(HASH_METADATA_NAME, hashProvider.hashPayload(archiveData.getPayload()));
        return metadata;
    }

    private Map<String, String> toMap(Collection<Metadata> metadata) {
        return metadata.stream().collect(Collectors.toMap(Metadata::getName, Metadata::getValue));
    }
}
