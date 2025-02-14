package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReference;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.crypto.vault.keymanagement.VaultKeyLocation;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationFactory;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationInitializer;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.Md5Utils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Testcontainers
class S3ObjectStorageRepositoryIT {

    private static final String MINIO_IMAGE = "quay.io/minio/minio:RELEASE.2022-09-07T22-25-02Z";
    private static final Integer MINIO_PORT = 9000;
    private static final String MINIO_ACCESS_KEY = "dev";
    private static final String MINIO_SECRET_KEY = "devsecret";
    private static final String TEST_BUCKET_NAME = "test-bucket";

    private static final LifecyclePolicy LIFECYCLE_POLICY = LifecyclePolicy.builder()
            .systemName("jme")
            .archiveTypeName("test-archive-type")
            .currentVersionExpirationDays(30)
            .previousVersionExpirationDays(30)
            .retainDays(30)
            .build();

    private TimedS3Client s3Client;

    private S3ObjectStorageRepository s3ObjectStorageRepository;

    private KeyIdCryptoService keyIdCryptoService;
    private KeyReferenceCryptoService keyReferenceCryptoService;

    @Container
    private final GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
                    .withExposedPorts(MINIO_PORT)
                    .withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
                    .withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
                    .withCommand("server", "/data");

    @SneakyThrows
    @BeforeEach
    void setUp() {
        final String minioHost = minioContainer.getHost();
        final int minioPort = minioContainer.getFirstMappedPort();
        final String minioUrl = new URL("http", minioHost, minioPort, "").toString();

        initS3Client(minioUrl);
        setupStorage();
        checkStorage();

        ObjectStorageProperties objectStorageProperties = new ObjectStorageProperties();
        objectStorageProperties.setObjectLockEnabled(true);
        s3ObjectStorageRepository = createS3ObjectStorageRepository(objectStorageProperties);
    }

    private S3ObjectStorageRepository createS3ObjectStorageRepository(ObjectStorageProperties objectStorageProperties) {
        LifecyclePolicyService lifecyclePolicyServiceMock = Mockito.mock(LifecyclePolicyService.class);
        when(lifecyclePolicyServiceMock.getLifecyclePolicy(any())).thenReturn(LIFECYCLE_POLICY);
        when(lifecyclePolicyServiceMock.getAllLifecyclePolicies()).thenReturn(List.of(LIFECYCLE_POLICY));
        S3LifecycleConfigurationFactory lifecycleConfigurationFactory = new S3LifecycleConfigurationFactory(lifecyclePolicyServiceMock);
        S3LifecycleConfigurationInitializer lifecycleConfigurationInitializer = new S3LifecycleConfigurationInitializer(lifecycleConfigurationFactory, s3Client);

        keyReferenceCryptoService = mock(KeyReferenceCryptoService.class);
        keyIdCryptoService = mock(KeyIdCryptoService.class);
        ArchiveCryptoService cryptoService = new ArchiveCryptoService(Optional.of(keyReferenceCryptoService), Optional.of(keyIdCryptoService));

        return new S3ObjectStorageRepository(objectStorageProperties, lifecycleConfigurationInitializer, cryptoService, s3Client);
    }

    @SneakyThrows
    @Test
    void testPutObject() {
        final String objectKey = "test-object-key";
        final byte[] objectContent = "test-object-content".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("test-meta-data-key", "test-meta-data-value");

        s3ObjectStorageRepository.putObject(TEST_BUCKET_NAME, objectKey, objectContent, metadata);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray()).isEqualTo(objectContent);
        assertThat(headObjectResponse.metadata()).containsAllEntriesOf(metadata);
        assertThat(headObjectResponse.expiration()).isNull();
        assertThat(headObjectResponse.expires()).isNull();
        assertThat(headObjectResponse.objectLockMode()).isNull();
        assertThat(headObjectResponse.objectLockRetainUntilDate()).isNull();
    }

    @SneakyThrows
    @Test
    void testPutObjectWithLifecyclePolicy() {
        final String objectKey = "test-object-lifecycled-key";
        final byte[] objectContent = "test-object-lifecycled-content".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("test-lifecycled-meta-data-key", "test-lifecycled-meta-data-value");

        final Date beforePutObject = new Date();
        final Date expectedRetentionMin = addTime(beforePutObject, LIFECYCLE_POLICY.getRetainDays(), Calendar.DATE);
        final Date expectedRetentionMax = addTime(expectedRetentionMin, 5, Calendar.MINUTE);

        s3ObjectStorageRepository.putObjectWithLifecyclePolicy(TEST_BUCKET_NAME, objectKey, objectContent, metadata, LIFECYCLE_POLICY, null);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray()).isEqualTo(objectContent);
        assertThat(headObjectResponse.metadata()).containsAllEntriesOf(metadata);
        String expirationNoLeadingZeroDayOfMonth = headObjectResponse.expiration().replace(", 0", ", ");
        assertThat(expirationNoLeadingZeroDayOfMonth).isEqualTo(policyAsExpirationString(LIFECYCLE_POLICY));
        assertThat(headObjectResponse.objectLockMode()).isEqualTo(ObjectLockMode.COMPLIANCE);
        assertThat(headObjectResponse.objectLockRetainUntilDate()).isBetween(expectedRetentionMin.toInstant(), expectedRetentionMax.toInstant());
    }

    @SneakyThrows
    @Test
    void testPutObjectWithLifecyclePolicy_encryptedWithKeyId() {
        ArchiveDataEncryption encryption = ArchiveDataEncryption.builder()
                .encryptionKeyId(EncryptionKeyId.builder()
                        .keyId("test-key-id")
                        .build())
                .build();

        String objectKey = "test-object-encrypted-key";
        byte[] objectContent = "test-object-content".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedContent = "test-object-encrypted-content".getBytes(StandardCharsets.UTF_8);
        when(keyIdCryptoService.encrypt(objectContent, KeyId.of("test-key-id")))
                .thenReturn(encryptedContent);

        s3ObjectStorageRepository.putObjectWithLifecyclePolicy(TEST_BUCKET_NAME, objectKey, objectContent, Map.of(), LIFECYCLE_POLICY, encryption);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray())
                .isEqualTo(encryptedContent);
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(headObjectResponse.metadata())
                .containsEntry("is_encrypted", "true");
    }

    @SneakyThrows
    @Test
    void testPutObjectWithLifecyclePolicy_encryptedWithKeyReference() {
        ArchiveDataEncryption encryption = ArchiveDataEncryption.builder()
                .encryptionKeyReference(EncryptionKeyReference.builder()
                        .secretEnginePath("path")
                        .keyName("keyname")
                        .build())
                .build();

        String objectKey = "test-object-encrypted-key";
        byte[] objectContent = "test-object-content".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedContent = "test-object-encrypted-content".getBytes(StandardCharsets.UTF_8);
        KeyReference keyReference = VaultKeyLocation.asKeyReference("path", "keyname");
        when(keyReferenceCryptoService.encrypt(objectContent, keyReference))
                .thenReturn(encryptedContent);

        s3ObjectStorageRepository.putObjectWithLifecyclePolicy(TEST_BUCKET_NAME, objectKey, objectContent, Map.of(), LIFECYCLE_POLICY, encryption);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray())
                .isEqualTo(encryptedContent);
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(headObjectResponse.metadata())
                .containsEntry("is_encrypted", "true");
    }

    @SneakyThrows
    @Test
    void testPutObjectWithLifecyclePolicyCustomObjectLockMode() {
        ObjectStorageProperties objectStorageProperties = new ObjectStorageProperties();
        objectStorageProperties.setObjectLockEnabled(true);
        objectStorageProperties.setObjectLockMode("GOVERNANCE");

        S3ObjectStorageRepository s3ObjectStorageRepository = createS3ObjectStorageRepository(objectStorageProperties);
        final String objectKey = "test-object-lifecycled-key";
        final byte[] objectContent = "test-object-lifecycled-content".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("test-lifecycled-meta-data-key", "test-lifecycled-meta-data-value");

        final Date beforePutObject = new Date();
        final Date expectedRetentionMin = addTime(beforePutObject, LIFECYCLE_POLICY.getRetainDays(), Calendar.DATE);
        final Date expectedRetentionMax = addTime(expectedRetentionMin, 5, Calendar.MINUTE);

        s3ObjectStorageRepository.putObjectWithLifecyclePolicy(TEST_BUCKET_NAME, objectKey, objectContent, metadata, LIFECYCLE_POLICY, null);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray()).isEqualTo(objectContent);
        assertThat(headObjectResponse.metadata()).containsAllEntriesOf(metadata);
        String expirationNoLeadingZeroDayOfMonth = headObjectResponse.expiration().replace(", 0", ", ");
        assertThat(expirationNoLeadingZeroDayOfMonth).isEqualTo(policyAsExpirationString(LIFECYCLE_POLICY));
        assertThat(headObjectResponse.objectLockMode()).isEqualTo(ObjectLockMode.GOVERNANCE);
        assertThat(headObjectResponse.objectLockRetainUntilDate()).isBetween(expectedRetentionMin.toInstant(), expectedRetentionMax.toInstant());
    }

    @SneakyThrows
    @Test
    void testPutObjectWithLifecyclePolicyCustomAndDisabledObjectLockMode() {
        ObjectStorageProperties objectStorageProperties = new ObjectStorageProperties();
        objectStorageProperties.setObjectLockEnabled(false);

        S3ObjectStorageRepository s3ObjectStorageRepository = createS3ObjectStorageRepository(objectStorageProperties);
        final String objectKey = "test-object-lifecycled-key";
        final byte[] objectContent = "test-object-lifecycled-content".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("test-lifecycled-meta-data-key", "test-lifecycled-meta-data-value");

        s3ObjectStorageRepository.putObjectWithLifecyclePolicy(TEST_BUCKET_NAME, objectKey, objectContent, metadata, LIFECYCLE_POLICY, null);

        ResponseBytes<GetObjectResponse> objectAsBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        assertThat(objectAsBytes.asByteArray()).isEqualTo(objectContent);
        assertThat(headObjectResponse.metadata()).containsAllEntriesOf(metadata);
        String expirationNoLeadingZeroDayOfMonth = headObjectResponse.expiration().replace(", 0", ", ");
        assertThat(expirationNoLeadingZeroDayOfMonth).isEqualTo(policyAsExpirationString(LIFECYCLE_POLICY));
        assertThat(headObjectResponse.objectLockMode()).isNull();
        assertThat(headObjectResponse.objectLockRetainUntilDate()).isNull();
    }

    private Date addTime(Date date, int amount, int calendarUnit) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(calendarUnit, amount);
        return cal.getTime();
    }

    private String policyAsExpirationString(LifecyclePolicy policy) {
        return String.format("expiry-date=\"%s\", rule-id=\"%s_%s_%s_%s\"", ZonedDateTime.now().plusDays(1).toLocalDate().atStartOfDay(ZoneId.of("GMT"))
                .plusDays(policy.getRetainDays()).format(DateTimeFormatter.RFC_1123_DATE_TIME), policy.getSystemName(), policy.getArchiveTypeName(), policy.getRetainDays(), policy.getRetainDays());
    }

    @Test
    void testGetObjectProperties() {
        final String objectKey = "test-object-props-key";
        final byte[] objectContent = "test-object-props-content".getBytes(StandardCharsets.UTF_8);
        Map<String, String> metadata = Map.of("test-props-meta-data-key", "test-props-meta-data-value");
        s3ObjectStorageRepository.putObject(TEST_BUCKET_NAME, objectKey, objectContent, metadata);

        Optional<StorageObjectProperties> propertiesNonExistingKey = s3ObjectStorageRepository.getObjectProperties(TEST_BUCKET_NAME, "does-not-exits");
        assertThat(propertiesNonExistingKey).isNotPresent();

        Optional<StorageObjectProperties> propertiesNonExistingBucket = s3ObjectStorageRepository.getObjectProperties("does-not-exist", objectKey);
        assertThat(propertiesNonExistingBucket).isNotPresent();

        StorageObjectProperties storageObjectProperties = s3ObjectStorageRepository.getObjectProperties(TEST_BUCKET_NAME, objectKey).get();
        assertThat(storageObjectProperties.getVersionId()).isNotNull();
        assertThat(storageObjectProperties.getMetadata()).containsAllEntriesOf(metadata);
    }

    private void initS3Client(String url) {
        S3ObjectStorageConnectionProperties connectionProperties = new S3ObjectStorageConnectionProperties();
        connectionProperties.setAccessKey(MINIO_ACCESS_KEY);
        connectionProperties.setSecretKey(MINIO_SECRET_KEY);
        connectionProperties.setAccessUrl(url);
        connectionProperties.setRegion("aws-global");

        s3Client = new TimedS3Client(connectionProperties, StaticCredentialsProvider.create(AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)));
    }

    private void setupStorage() {
        CreateBucketRequest cbr = CreateBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .objectLockEnabledForBucket(true)
                .build();
        s3Client.createBucket(cbr);
    }

    @SneakyThrows
    private void checkStorage() {
        final String objectKey = "check-object";
        final byte[] content = "check-content".getBytes(StandardCharsets.UTF_8);
        final String tagName = "check-tag";
        final String tagValue = "check-tag-value";

        // Check creating an object
        Tag tag = Tag.builder().key(tagName).value(tagValue).build();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(objectKey)
                .contentLength((long) content.length)
                .contentMD5(computeMD5HashBase64(content))
                .objectLockMode(ObjectLockMode.COMPLIANCE)
                .objectLockRetainUntilDate(ZonedDateTime.now().plusDays(30).toInstant())
                .tagging(Tagging.builder().tagSet(tag).build())
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content));

        // Check reading created object
        try {
            s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(TEST_BUCKET_NAME).key(objectKey).build());
        } catch (Exception e) {
            log.error("Reading check object failed.", e);
            throw e;
        }
    }

    private String computeMD5HashBase64(byte[] object) {
        return Md5Utils.md5AsBase64(object);
    }

}
