package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageTarget;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultObjectStorageStrategyTest {

    private static final String BUCKET = "testbucket";
    private final static ArchiveDataSchema ARCHIVE_DATA_SCHEMA = ArchiveDataSchema.builder()
            .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
            .system("test-system")
            .name("schemaname")
            .referenceIdType("ch.admin.bit.jeap.audit.type.SchemaNameArchive")
            .version(1)
            .fileExtension("avpr")
            .build();

    @Test
    void testGetObjectStorageTarget_WhenBucketAndPrefixNotGiven_ThenUseConfiguredBucketAndPrefixWithReferenceId() {
        final ArchiveData archiveData = createArchiveData(null, null);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.YEAR);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).isNotBlank();
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenBucketAndPrefixBlank_ThenUseUseConfiguredBucketAndPrefixWithReferenceId() {
        final String blankBucket = " ";
        final String blankPrefix = "";
        final ArchiveData archiveData = createArchiveData(blankBucket, blankPrefix);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.YEAR);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).isNotBlank();
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenBucketAndPrefixGiven_ThenUseGivenBucketAndPrefixWithReferenceId() {
        final String customBucket = "custom-bucket";
        final String customPrefix = "custom-prefix/";
        final ArchiveData archiveData = createArchiveData(customBucket, customPrefix);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.YEAR);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(customBucket);
        assertThat(target.getPrefix()).isEqualTo(customPrefix);
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenPrefixModeNone_ThenEmptyStringPrefix() {
        final ArchiveData archiveData = createArchiveData(null, null);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.NONE);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).isEmpty();
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenPrefixModeMonth_ThenPrefixStartsWithYear() {
        final ArchiveData archiveData = createArchiveData(null, null);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.YEAR);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).startsWith(String.format("%1$tY", LocalDate.now()));
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenPrefixModeMonth_ThenPrefixStartsWithYearMonth() {
        final ArchiveData archiveData = createArchiveData(null, null);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.MONTH);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).startsWith(String.format("%1$tY%1$tm", LocalDate.now()));
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    @Test
    void testGetObjectStorageTarget_WhenPrefixModeDay_ThenPrefixStartsWithYearMonthDay() {
        final ArchiveData archiveData = createArchiveData(null, null);
        final DefaultObjectStorageStrategy defaultObjectStorageStrategy = createDefaultObjectStorageStrategy(BUCKET, PrefixMode.DAY);

        ObjectStorageTarget target = defaultObjectStorageStrategy.getObjectStorageTarget(archiveData, ARCHIVE_DATA_SCHEMA);

        assertThat(target.getBucket()).isEqualTo(BUCKET);
        assertThat(target.getPrefix()).startsWith(String.format("%1$tY%1$tm%1$td", LocalDate.now()));
        assertThat(target.getName()).isEqualTo("hash_" + archiveData.getReferenceId() + "_" + ARCHIVE_DATA_SCHEMA.getReferenceIdType());
    }

    private ArchiveData createArchiveData(String bucket, String prefix) {
        return ArchiveData.builder()
                .contentType("application/json")
                .system("test-system")
                .schema("some schema")
                .schemaVersion(1)
                .referenceId(UUID.randomUUID().toString())
                .payload("{ \"data\" = \"test\"}".getBytes(StandardCharsets.UTF_8))
                .storageBucket(Optional.ofNullable(bucket))
                .storagePrefix(Optional.ofNullable(prefix))
                .metadata(Collections.emptyList())
                .build();
    }

    private DefaultObjectStorageStrategy createDefaultObjectStorageStrategy(String bucket, PrefixMode prefixMode) {
        DefaultObjectStorageStrategyProperties storageStrategyProperties = new DefaultObjectStorageStrategyProperties();
        storageStrategyProperties.setBucket(bucket);
        storageStrategyProperties.setPrefixMode(prefixMode);
        HashProvider hashProvider = new HashProviderTestConfig().hashProvider();
        return new DefaultObjectStorageStrategy(storageStrategyProperties, hashProvider);
    }

}
