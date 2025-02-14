package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = {CustomObjectStorageStrategyConfig.class, ObjectStorageConfiguration.class, HashProviderTestConfig.class},
        properties = {"spring.cloud.vault.enabled=false"})
class CustomObjectStorageStrategyIT {

    @Autowired
    ArchiveDataObjectStoreAdapter archiveDataObjectStoreAdapter;

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
    void testStore() {
        ArchiveData archiveData = createArchiveData();

        ArchiveDataStorageInfo storageInfo = archiveDataObjectStoreAdapter.store(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(storageInfo.getBucket()).isEqualTo(CustomObjectStorageStrategy.CUSTOM_BUCKET);
        assertThat(storageInfo.getKey()).startsWith(CustomObjectStorageStrategy.CUSTOM_PREFIX);
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
