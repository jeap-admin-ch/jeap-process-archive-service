package ch.admin.bit.jeap.processarchive.adapter.opensearch;

import ch.admin.bit.jeap.opensearch.searchitem.api.SearchItemsProvider;
import ch.admin.bit.jeap.opensearch.searchitem.api.exception.SearchItemsBadInputException;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.objectsstorage.StorageObjectProperties;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchivedArtifactSearchItemsProvider implements SearchItemsProvider {

    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final IndexTypeConfigurationRepository configurationRepository;

    @Override
    public Optional<SearchItemContainer> findSearchItem(String indexType, String originId, String originVersion) throws SearchItemsBadInputException {
        log.info("Received request to search item with indexType '{}', originId '{}', originVersion '{}'", indexType, originId, originVersion);

        String[] split = originId.split(":");
        if (split.length != 2) {
            log.error("Invalid origin id '{}'. Required format 'objectBucket:objectKey'", originId);
            throw new SearchItemsBadInputException("Invalid origin id. Required format 'objectBucket:objectKey' : " + originId);
        }
        String objectBucket = split[0];
        String objectKey = split[1];

        Optional<IndexTypeConfiguration> indexTypeConfigurationOptional = configurationRepository.findByName(indexType);

        if (indexTypeConfigurationOptional.isEmpty()) {
            log.warn("Configuration for the IndexType '{}' not found", indexType);
            return Optional.empty();
        }

        IndexTypeConfiguration indexTypeConfiguration = indexTypeConfigurationOptional.get();

        Optional<StorageObjectProperties> objectPropertiesOptional = archiveDataObjectStore.getObjectProperties(objectBucket, objectKey);
        if (objectPropertiesOptional.isEmpty()) {
            log.warn("Archived artifact properties with object bucket '{}' and object key '{}' not found in object storage", objectBucket, objectKey);
            return Optional.empty();
        }

        Optional<Object> archivedArtifactOptional = archiveDataObjectStore.retrieveObject(indexTypeConfiguration.archiveType(), objectBucket, objectKey, originVersion);
        if (archivedArtifactOptional.isEmpty()) {
            log.warn("Archived artifact with object bucket '{}' and object key '{}' not found in object storage", objectBucket, objectKey);
            return Optional.empty();
        }

        return Optional.of(indexTypeConfiguration.archiveTypeToSearchItemConverter().convert(
                archivedArtifactOptional.get(),
                originId,
                objectPropertiesOptional.get().getVersionId(),
                objectPropertiesOptional.get().getMetadata()));
    }

}
