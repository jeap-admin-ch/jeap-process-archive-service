package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.objectstorage.InMemoryObjectStorageRepository.StorageObject;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(
        classes = {ObjectStorageConfiguration.class, HashProviderTestConfig.class},
        properties = {"jeap.processarchive.objectstorage.storage.bucket=testbucket",
                "logging.level.ch.admin.bit.jeap.processarchive.objectstorage=DEBUG",
                "spring.cloud.vault.enabled=false"})
class ObjectStorageInMemoryIT {

    @Autowired
    ArchiveDataObjectStoreAdapter archiveDataObjectStoreAdapter;

    @Autowired
    ObjectStorageRepository objectStorageRepository;

    @Autowired
    HashProvider hashProvider;

    @MockitoBean
    LifecyclePolicyService lifecyclePolicyService;

    private final static ArchiveDataSchema ARCHIVE_DATA_SCHEMA = ArchiveDataSchema.builder()
            .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
            .system("test-system")
            .name("schemaname")
            .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
            .version(1)
            .fileExtension("avpr")
            .build();

    @Test
    void testStore_WhenObjectDoesNotExistAndArchiveDataVersionSet_ThenFirstVersionCreated() {
        testStore_WhenObjectDoesNotExist_ThenFirstVersionCreated(1);
    }

    @Test
    void testStore_WhenObjectDoesNotExistAndArchiveDataVersionNull_ThenFirstVersionCreated() {
        testStore_WhenObjectDoesNotExist_ThenFirstVersionCreated(null);
    }

    void testStore_WhenObjectDoesNotExist_ThenFirstVersionCreated(Integer archiveDataVersion) {
        final String referenceId = UUID.randomUUID().toString();
        ArchiveData archiveData = createArchiveData(referenceId, archiveDataVersion);
        InMemoryObjectStorageRepository inMemoryObjectStorageRepository = (InMemoryObjectStorageRepository) objectStorageRepository;
        assertThat(inMemoryObjectStorageRepository.getAllVersions("testbucket", referenceId)).size().isEqualTo(0);

        ArchiveDataStorageInfo storageInfo = archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);

        String today = formatToday();
        assertThat(storageInfo.getBucket()).isEqualTo("testbucket");
        assertThat(storageInfo.getKey()).isEqualTo(today + "/hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
        assertThat(storageInfo.getVersionId()).isNotBlank();
        assertThat(inMemoryObjectStorageRepository.getAllVersions(storageInfo.getBucket(), storageInfo.getKey())).size().isEqualTo(1);
        StorageObject storedObject = inMemoryObjectStorageRepository.getStorageObject(storageInfo.getBucket(), storageInfo.getKey(), storageInfo.getVersionId());
        assertThat(storedObject.getPayload()).isEqualTo(archiveData.getPayload());
        assertThat(storedObject.getMetadata()).isEqualTo(createExpectedMetadata(archiveData));
    }

    @Test
    void testStore_WhenObjectVersionAlreadyExists_ThenAdditionalVersionCreated() {
        final String referenceId = UUID.randomUUID().toString();
        final ArchiveData archiveDataExistingVersion = createArchiveData(referenceId, 1);
        final ArchiveDataStorageInfo storageInfoExistingVersion = archiveDataObjectStoreAdapter.store(archiveDataExistingVersion, ARCHIVE_DATA_SCHEMA);
        InMemoryObjectStorageRepository inMemoryObjectStorageRepository = (InMemoryObjectStorageRepository) objectStorageRepository;
        assertThat(inMemoryObjectStorageRepository.getAllVersions(storageInfoExistingVersion.getBucket(), storageInfoExistingVersion.getKey())).size().isEqualTo(1);
        final ArchiveData archiveDataNewVersion = createArchiveData(referenceId, 2);
        assertThat(archiveDataNewVersion.getReferenceId()).isEqualTo(archiveDataExistingVersion.getReferenceId());
        assertThat(archiveDataNewVersion.getVersion()).isNotEqualTo(archiveDataExistingVersion.getVersion());

        ArchiveDataStorageInfo storageInfoNewVersion = archiveDataObjectStoreAdapter.store(archiveDataNewVersion, ARCHIVE_DATA_SCHEMA);

        String today = formatToday();
        assertThat(storageInfoNewVersion.getBucket()).isEqualTo("testbucket");
        assertThat(storageInfoNewVersion.getBucket()).isEqualTo(storageInfoExistingVersion.getBucket());
        assertThat(storageInfoNewVersion.getKey()).isEqualTo(today + "/hash_" + archiveDataNewVersion.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
        assertThat(storageInfoNewVersion.getVersionId()).isNotBlank();
        assertThat(storageInfoNewVersion.getVersionId()).isNotEqualTo(storageInfoExistingVersion.getVersionId());
        assertThat(inMemoryObjectStorageRepository.getAllVersions(storageInfoNewVersion.getBucket(), storageInfoNewVersion.getKey())).size().isEqualTo(2);
        StorageObject storedObject = inMemoryObjectStorageRepository.getStorageObject(storageInfoNewVersion.getBucket(), storageInfoNewVersion.getKey(), storageInfoNewVersion.getVersionId());
        assertThat(storedObject.getPayload()).isEqualTo(archiveDataNewVersion.getPayload());
        assertThat(storedObject.getMetadata()).isEqualTo(createExpectedMetadata(archiveDataNewVersion));
    }

    private String formatToday() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private Map<String, String> createExpectedMetadata(ArchiveData archiveData) {
        Map<String, String> metadata = new HashMap<>(
                archiveData.getMetadata().stream().collect(Collectors.toMap(Metadata::getName, Metadata::getValue)));
        metadata.put(ArchiveDataObjectStoreAdapter.REFERENCE_ID_METADATA_NAME, archiveData.getReferenceId());
        if (archiveData.getVersion() != null) {
            metadata.put(ArchiveDataObjectStoreAdapter.VERSION_METADATA_NAME, Integer.toString(archiveData.getVersion()));
        }
        metadata.put(ArchiveDataObjectStoreAdapter.CONTENT_TYPE_METADATA_NAME, archiveData.getContentType());
        metadata.put(ArchiveDataObjectStoreAdapter.SYSTEM_NAME, archiveData.getSystem());
        metadata.put(ArchiveDataObjectStoreAdapter.SCHEMA_METADATA_NAME, archiveData.getSchema());
        metadata.put(ArchiveDataObjectStoreAdapter.SCHEMA_VERSION_METADATA_NAME, String.valueOf(archiveData.getSchemaVersion()));
        metadata.put(ArchiveDataObjectStoreAdapter.HASH_METADATA_NAME, hashProvider.hashPayload(archiveData.getPayload()));
        String schemaFileKey = "archive-data-schema/test-system_schemaname_1.avpr";
        metadata.put(ArchiveDataObjectStoreAdapter.SCHEMAFILE_KEY_METADATA_NAME, schemaFileKey);
        String schemaVersionId = objectStorageRepository.getObjectProperties("testbucket", schemaFileKey)
                .orElseThrow().getVersionId();
        metadata.put(ArchiveDataObjectStoreAdapter.SCHEMAFILE_VERSION_ID_METADATA_NAME, schemaVersionId);
        return metadata;
    }

    private ArchiveData createArchiveData(String referenceId, Integer version) {
        return ArchiveData.builder()
                .contentType("application/json")
                .system("test-system")
                .schema("some schema")
                .schemaVersion(1)
                .referenceId(referenceId)
                .payload(("{ \"data\" = \"test" + version + "\"}").getBytes(StandardCharsets.UTF_8))
                .version(version)
                .metadata(List.of(Metadata.of("meta", "data"), Metadata.of("meta" + version, "data" + version)))
                .build();
    }

}
