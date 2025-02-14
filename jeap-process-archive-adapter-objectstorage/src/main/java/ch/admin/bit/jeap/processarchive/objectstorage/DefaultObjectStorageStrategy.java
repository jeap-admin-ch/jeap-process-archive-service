package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageStrategy;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageTarget;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DefaultObjectStorageStrategy implements ObjectStorageStrategy {

    private static final String PREFIX_DELIMITER = "/";
    private static final DateTimeFormatter DAY_MODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd" + PREFIX_DELIMITER);
    private static final DateTimeFormatter MONTH_MODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM" + PREFIX_DELIMITER);
    private static final DateTimeFormatter YEAR_MODE_FORMATTER = DateTimeFormatter.ofPattern("yyyy" + PREFIX_DELIMITER);

    private final DefaultObjectStorageStrategyProperties strategyProperties;
    private final HashProvider hashProvider;

    public DefaultObjectStorageStrategy(DefaultObjectStorageStrategyProperties strategyProperties, HashProvider hashProvider) {
        Objects.requireNonNull(strategyProperties.getBucket(), "Object storage bucket name must be provided.");
        Objects.requireNonNull(strategyProperties.getPrefixMode(), "Object storage prefix mode must be provided.");
        this.strategyProperties = strategyProperties;
        this.hashProvider = hashProvider;
    }

    @Override
    public ObjectStorageTarget getObjectStorageTarget(ArchiveData archiveData, ArchiveDataSchema schema) {
        final String targetBucket = archiveData.getStorageBucket().filter(b -> !b.isBlank()).orElse(strategyProperties.getBucket());
        final String targetPrefix = archiveData.getStoragePrefix().filter(p -> !p.isBlank()).orElse(getPrefix());
        String referenceId = archiveData.getReferenceId();
        final String targetName = hashProvider.hashStorageObjectId(referenceId, schema.getReferenceIdType());

        return ObjectStorageTarget.builder()
                .bucket(targetBucket)
                .prefix(targetPrefix)
                .name(targetName)
                .build();
    }

    private String getPrefix() {
        final LocalDate now = LocalDate.now();
        final PrefixMode prefixMode = strategyProperties.getPrefixMode();
        switch (prefixMode) {
            case NONE: return "";
            case DAY: return now.format(DAY_MODE_FORMATTER);
            case MONTH: return now.format(MONTH_MODE_FORMATTER);
            case YEAR: return now.format(YEAR_MODE_FORMATTER);
            default: throw new IllegalArgumentException("Unsupported prefix mode " + prefixMode);
        }
    }

}
