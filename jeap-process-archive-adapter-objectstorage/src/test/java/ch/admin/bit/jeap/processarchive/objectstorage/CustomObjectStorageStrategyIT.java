package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStoreStorageException;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ActiveProfiles(JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = {CustomObjectStorageStrategyConfig.class, ObjectStorageConfiguration.class, HashProviderTestConfig.class},
        properties = {"spring.cloud.vault.enabled=false"})
class CustomObjectStorageStrategyIT {

    @Autowired
    ArchiveDataObjectStoreAdapter archiveDataObjectStoreAdapter;

    @Autowired
    ObjectStorageProperties objectStorageProperties;

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

    private final static ArchiveDataSchema OTHER_ARCHIVE_DATA_SCHEMA = ArchiveDataSchema.builder()
            .schemaDefinition("different-schema-test".getBytes(StandardCharsets.UTF_8))
            .system("test-system")
            .name("schemaname")
            .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
            .version(1)
            .fileExtension("avpr")
            .build();

    @Test
    void testStore() {
        ArchiveData archiveData = createArchiveData();

        ArchiveDataStorageInfo storageInfo = archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(storageInfo.getBucket()).isEqualTo(CustomObjectStorageStrategy.CUSTOM_BUCKET);
        assertThat(storageInfo.getKey()).startsWith(CustomObjectStorageStrategy.CUSTOM_PREFIX);
    }

    @Test
    void testStoreTwice_sameSchema_shouldNotThrowException() {
        ArchiveData archiveData = createArchiveData();

        archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);
        assertDoesNotThrow(() ->
                archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA));
    }

    @Test
    void testStoreTwice_differentSchema_overwriteAllowed_shouldNotThrowException() {
        ArchiveData archiveData = createArchiveData();

        archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);
        assertDoesNotThrow(() ->
                archiveDataObjectStoreAdapter.store(archiveData, OTHER_ARCHIVE_DATA_SCHEMA));
    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @Test
    void testStoreTwice_differentSchema_overwriteNotAllowed_shouldThrowException() {
        objectStorageProperties.setSchemaOverwriteAllowed(false);

        ArchiveData archiveData = createArchiveData();

        archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);
        assertThatThrownBy(() ->
                archiveDataObjectStoreAdapter.store(archiveData, OTHER_ARCHIVE_DATA_SCHEMA))
                .isInstanceOf(ArchiveDataObjectStoreStorageException.class)
                .hasCauseInstanceOf(S3ObjectStorageException.class)
                .hasMessageContaining("Schema overwrite not allowed");
    }

    private ArchiveData createArchiveData() {
        return ArchiveData.builder()
                .contentType("application/json")
                .system("test-system")
                .schema("some schema")
                .schemaVersion(1)
                .referenceId(UUID.randomUUID().toString())
                .payload("{ \"data\" = \"test\"}".getBytes(StandardCharsets.UTF_8))
                .metadata(emptyList())
                .build();
    }
}
