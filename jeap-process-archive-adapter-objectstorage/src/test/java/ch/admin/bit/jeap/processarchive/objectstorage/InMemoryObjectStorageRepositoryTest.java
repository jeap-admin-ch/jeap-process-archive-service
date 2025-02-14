package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.objectstorage.InMemoryObjectStorageRepository.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryObjectStorageRepositoryTest {

    private static final byte[] PAYLOAD = "This is a test object.".getBytes(StandardCharsets.UTF_8);
    private static final String BUCKET = "testbucket";
    private static final String OBJECT_KEY = "1234567890";
    private static final Map<String, String> METADATA = Map.of("meta1", "data1", "meta2", "data2");

    private InMemoryObjectStorageRepository inMemoryObjectStorageRepository;

    @BeforeEach
    void setUp() {
        inMemoryObjectStorageRepository = new InMemoryObjectStorageRepository();
    }

    @Test
    void testPutAndGet() {
        assertThat(inMemoryObjectStorageRepository.doesObjectExist(BUCKET, OBJECT_KEY)).isFalse();

        final String objectVersionId = inMemoryObjectStorageRepository.putObjectWithLifecyclePolicy(BUCKET, OBJECT_KEY, PAYLOAD, METADATA, null, null);
        final byte[] payload = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionId).getPayload();
        final StorageObject storageObject = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionId);

        assertThat(payload).isEqualTo(PAYLOAD);
        assertThat(storageObject.getPayload()).isEqualTo(PAYLOAD);
        assertThat(storageObject.getMetadata()).isEqualTo(METADATA);
    }

    @Test
    void testPutAndGetWithTwoVersions() {
        final byte[] secondVersionPayload = "This is the second version of the test object.".getBytes(StandardCharsets.UTF_8);
        final Map<String, String> secondVersionMetadata = Map.of("meta3", "data3", "meta2", "data2");
        assertThat(inMemoryObjectStorageRepository.doesObjectExist(BUCKET, OBJECT_KEY)).isFalse();

        // Put two versions of the object
        final String objectVersionIdFirstVersion = inMemoryObjectStorageRepository.putObjectWithLifecyclePolicy(BUCKET, OBJECT_KEY, PAYLOAD, METADATA, null, null);
        final String objectVersionIdSecondVersion = inMemoryObjectStorageRepository.putObjectWithLifecyclePolicy(BUCKET, OBJECT_KEY, secondVersionPayload, secondVersionMetadata, null, null);

        // Get both versions of the object
        final byte[] payloadFirstVersion = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionIdFirstVersion).getPayload();
        final StorageObject storageObjectFirstVersion = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionIdFirstVersion);
        final byte[] payloadSecondVersion = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionIdSecondVersion).getPayload();
        final StorageObject storageObjectSecondVersion = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, objectVersionIdSecondVersion);

        assertThat(payloadFirstVersion).isEqualTo(PAYLOAD);
        assertThat(payloadSecondVersion).isEqualTo(secondVersionPayload);
        assertThat(storageObjectFirstVersion.getPayload()).isEqualTo(PAYLOAD);
        assertThat(storageObjectSecondVersion.getPayload()).isEqualTo(secondVersionPayload);
        assertThat(storageObjectFirstVersion.getMetadata()).isEqualTo(METADATA);
        assertThat(storageObjectSecondVersion.getMetadata()).isEqualTo(secondVersionMetadata);
    }

    @Test
    void testGetAllVersions() {
        assertThat(inMemoryObjectStorageRepository.doesObjectExist(BUCKET, OBJECT_KEY)).isFalse();

        assertThat(inMemoryObjectStorageRepository.getAllVersions(BUCKET, OBJECT_KEY)).isEmpty();

        String versionId1 = inMemoryObjectStorageRepository.putObjectWithLifecyclePolicy(BUCKET, OBJECT_KEY, PAYLOAD, METADATA, null, null);
        StorageObject storageObjectVersion1 = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, versionId1);

        assertThat(inMemoryObjectStorageRepository.getAllVersions(BUCKET, OBJECT_KEY)).hasSize(1);
        assertThat(inMemoryObjectStorageRepository.getAllVersions(BUCKET, OBJECT_KEY)).containsValues(storageObjectVersion1);

        String versionId2 = inMemoryObjectStorageRepository.putObjectWithLifecyclePolicy(BUCKET, OBJECT_KEY, PAYLOAD, METADATA, null, null);
        StorageObject storageObjectVersion2 = inMemoryObjectStorageRepository.getStorageObject(BUCKET, OBJECT_KEY, versionId2);

        assertThat(inMemoryObjectStorageRepository.getAllVersions(BUCKET, OBJECT_KEY)).hasSize(2);
        assertThat(inMemoryObjectStorageRepository.getAllVersions(BUCKET, OBJECT_KEY)).containsValues(storageObjectVersion1, storageObjectVersion2);
    }
}
