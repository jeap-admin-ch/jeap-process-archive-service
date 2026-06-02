package ch.admin.bit.jeap.processarchive.adapter.opensearch;

import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.objectsstorage.StorageObjectProperties;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfigurationRepository;
import ch.admin.bit.jeap.processarchive.plugin.api.indextype.SearchItemContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchItemsProvider {

    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final IndexTypeConfigurationRepository configurationRepository;


    public Optional<SearchItemContainer> searchItem(String originId, String indexType, String objectBucket, String objectKey, String originVersion) {
        log.info("Searching for item with indexType '{}', originId '{}', objectBucket '{}', objectKey '{}', originVersion '{}'", indexType, originId, objectBucket, objectKey, originVersion);

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

        SearchItemContainer itemContainer = indexTypeConfiguration.archiveTypeToSearchItemConverter().convert(
                archivedArtifactOptional.get(),
                originId,
                objectPropertiesOptional.get().getVersionId(),
                objectPropertiesOptional.get().getMetadata());

        return Optional.of(itemContainer);
    }

}
