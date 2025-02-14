package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationInitializer;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.Md5Utils;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class S3ObjectStorageRepository implements ObjectStorageRepository {

    private final ObjectStorageProperties objectStorageProperties;

    private final S3LifecycleConfigurationInitializer lifecycleConfigurationInitializer;

    private final ArchiveCryptoService archiveCryptoService;

    private final TimedS3Client s3Client;

    private static final String METADATA_KEY_IS_ENCRYPTED = "is_encrypted";

    @Timed(value = "jeap_pas_put_object_with_lifecycle_policy", description = "Store object in s3 with lifecycle policy")
    @Override
    public String putObjectWithLifecyclePolicy(String bucketName, String objectKey, byte[] objectPayload,
                                               Map<String, String> metadata, LifecyclePolicy lifecyclePolicy,
                                               ArchiveDataEncryption encryption) {
        log.info("Putting object with lifecycle with key '{}' and size '{}' into bucket '{}'.", objectKey, objectPayload.length, bucketName);
        ZonedDateTime retainUntil = objectStorageProperties.isObjectLockEnabled() ? computeRetainUntil(lifecyclePolicy) : null;
        lifecycleConfigurationInitializer.ensureLifecyclePolicyPresent(bucketName, lifecyclePolicy);
        Tag tag = S3LifecycleConfigurationInitializer.lifecyclePolicyTag(lifecyclePolicy);
        log.info("Parameters: Tag '{}', retainUntil '{}', encryption '{}'.", tag, retainUntil, encryption);
        return internalPutObject(bucketName, objectKey, objectPayload, metadata, tag, retainUntil, encryption);
    }

    private ZonedDateTime computeRetainUntil(LifecyclePolicy lifecyclePolicy) {
        return ZonedDateTime.now().plusDays(lifecyclePolicy.getRetainDays());
    }

    @Timed(value = "jeap_pas_put_object", description = "Store object in s3")
    @Override
    public String putObject(String bucketName, String objectKey, byte[] objectPayload, Map<String, String> metadata) {
        log.info("Putting object with key '{}' and size '{}' into bucket '{}'.", objectKey, objectPayload.length, bucketName);
        return internalPutObject(bucketName, objectKey, objectPayload, metadata, null, null, null);
    }

    private String internalPutObject(String bucketName, String objectKey, byte[] objectPayload,
                                     Map<String, String> metadata, @Nullable Tag tag, @Nullable ZonedDateTime retainUntil,
                                     ArchiveDataEncryption encryption) {

        byte[] payloadToArchive;

        Map<String, String> objectMetadata = new HashMap<>();

        if (encryption != null) {
            payloadToArchive = archiveCryptoService.encrypt(objectPayload, encryption);
            objectMetadata.put(METADATA_KEY_IS_ENCRYPTED, Boolean.TRUE.toString());
        } else {
            payloadToArchive = objectPayload;
        }

        if (metadata != null) {
            objectMetadata.putAll(metadata);
        }

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength((long) payloadToArchive.length)
                .contentMD5(computeMD5HashBase64(payloadToArchive))
                .metadata(objectMetadata);

        if (retainUntil != null) {
            log.info("Setting retain-until-date to '{}' on object with key '{}' in bucket '{}'", retainUntil, objectKey, bucketName);
            Date retainUntilDate = Date.from(retainUntil.toInstant());
            requestBuilder.objectLockRetainUntilDate(retainUntilDate.toInstant());
            requestBuilder.objectLockMode(objectStorageProperties.getObjectLockMode());
        }
        if (tag != null) {
            requestBuilder.tagging(Tagging.builder().tagSet(tag).build());
        }

        PutObjectRequest putObjectRequest = requestBuilder.build();
        PutObjectResponse putObjectResult = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(payloadToArchive));
        log.info("Successfully put object with key '{}' and size '{}' into bucket '{}' with payload having md5 hash '{}' and version id '{}'.",
                objectKey,
                objectPayload.length,
                bucketName,
                putObjectRequest.contentMD5(),
                putObjectResult.versionId());
        return putObjectResult.versionId();
    }

    /**
     * Returns object properties (version ID, metadata) for the current version of the object
     */
    @Timed(value = "jeap_pas_get_object_properties", description = "Get object properties in s3")
    @Override
    public Optional<StorageObjectProperties> getObjectProperties(String bucketName, String objectKey) {
        if (!s3Client.doesObjectExist(bucketName, objectKey)) {
            return Optional.empty();
        }
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .key(objectKey)
                .bucket(bucketName)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

        StorageObjectProperties properties = StorageObjectProperties.builder()
                .metadata(headObjectResponse.metadata())
                .versionId(headObjectResponse.versionId())
                .build();
        return Optional.of(properties);
    }

    private String computeMD5HashBase64(byte[] object) {
        return Md5Utils.md5AsBase64(object);
    }

}
