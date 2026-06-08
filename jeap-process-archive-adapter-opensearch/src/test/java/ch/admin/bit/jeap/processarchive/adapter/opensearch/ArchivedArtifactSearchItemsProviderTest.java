package ch.admin.bit.jeap.processarchive.adapter.opensearch;

import ch.admin.bit.jeap.opensearch.indextype.Origin;
import ch.admin.bit.jeap.opensearch.indextype.SearchItem;
import ch.admin.bit.jeap.opensearch.searchitem.api.exception.SearchItemsBadInputException;
import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.objectsstorage.StorageObjectProperties;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfigurationRepository;
import ch.admin.bit.jeap.processarchive.plugin.api.indextype.ArchiveTypeToSearchItemConverter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivedArtifactSearchItemsProviderTest {

    private static final String INDEX_TYPE = "test-index-type";
    private static final String OBJECT_BUCKET = "my-bucket";
    private static final String OBJECT_KEY = "my-key";
    private static final String ORIGIN_VERSION = "1.0.0";
    private static final String ORIGIN_ID = OBJECT_BUCKET + ":" + OBJECT_KEY;

    @Mock
    private ArchiveDataObjectStore archiveDataObjectStore;

    @Mock
    private IndexTypeConfigurationRepository configurationRepository;

    @InjectMocks
    private ArchivedArtifactSearchItemsProvider archivedArtifactSearchItemsProvider;

    @Test
    @SneakyThrows
    void searchItem_whenArtifactFound_returnsOkWithSearchItemAndVersionHeaders() {
        SearchItem<String> searchItem = new SearchItem<>(
                new Origin(ORIGIN_ID, ORIGIN_VERSION, null, null, null, null, Map.of("url", "https://hello.ch")),
                "Hello World");
        SearchItemContainer container = new SearchItemContainer(2, 3, searchItem);

        @SuppressWarnings("unchecked")
        ArchiveTypeToSearchItemConverter<Object> converter = mock(ArchiveTypeToSearchItemConverter.class);
        when(converter.convert(any(), eq(ORIGIN_ID), eq(ORIGIN_VERSION), any())).thenReturn(container);

        IndexTypeConfiguration config = buildIndexTypeConfigurationWithConverter(converter);
        when(configurationRepository.findByName(INDEX_TYPE)).thenReturn(Optional.of(config));

        StorageObjectProperties objectProperties = StorageObjectProperties.builder().versionId(ORIGIN_VERSION).metadata(Map.of()).build();
        when(archiveDataObjectStore.getObjectProperties(OBJECT_BUCKET, OBJECT_KEY))
                .thenReturn(Optional.of(objectProperties));
        Object archivedPayload = new Object();
        when(archiveDataObjectStore.retrieveObject(any(), eq(OBJECT_BUCKET), eq(OBJECT_KEY), eq(null)))
                .thenReturn(Optional.of(archivedPayload));

        Optional<SearchItemContainer> searchItemContainer = archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, ORIGIN_ID, null);

        assertThat(searchItemContainer).isPresent();

        assertThat(searchItemContainer.get().searchItem()).isEqualTo(searchItem);
        assertThat(searchItemContainer.get().indexMajorVersion()).isEqualTo(2);
        assertThat(searchItemContainer.get().indexMinorVersion()).isEqualTo(3);
    }

    @Test
    @SneakyThrows
    void searchItem_withVersion_whenArtifactFound_returnsOkWithSearchItemAndVersionHeaders() {
        SearchItem<String> searchItem = new SearchItem<>(
                new Origin(ORIGIN_ID, ORIGIN_VERSION, null, null, null, null, Map.of("url", "https://hello.ch")),
                "Hello World");
        SearchItemContainer container = new SearchItemContainer(2, 3, searchItem);

        @SuppressWarnings("unchecked")
        ArchiveTypeToSearchItemConverter<Object> converter = mock(ArchiveTypeToSearchItemConverter.class);
        when(converter.convert(any(), eq(ORIGIN_ID), eq(ORIGIN_VERSION), any())).thenReturn(container);

        IndexTypeConfiguration config = buildIndexTypeConfigurationWithConverter(converter);
        when(configurationRepository.findByName(INDEX_TYPE)).thenReturn(Optional.of(config));

        Object archivedPayload = new Object();
        StorageObjectProperties objectProperties = StorageObjectProperties.builder().versionId(ORIGIN_VERSION).build();
        when(archiveDataObjectStore.getObjectProperties(OBJECT_BUCKET, OBJECT_KEY))
                .thenReturn(Optional.of(objectProperties));
        when(archiveDataObjectStore.retrieveObject(any(), eq(OBJECT_BUCKET), eq(OBJECT_KEY), eq(ORIGIN_VERSION)))
                .thenReturn(Optional.of(archivedPayload));


        Optional<SearchItemContainer> searchItemContainer = archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, ORIGIN_ID, ORIGIN_VERSION);

        assertThat(searchItemContainer).isPresent();

        assertThat(searchItemContainer.get().searchItem()).isEqualTo(searchItem);
        assertThat(searchItemContainer.get().indexMajorVersion()).isEqualTo(2);
        assertThat(searchItemContainer.get().indexMinorVersion()).isEqualTo(3);
    }

    @Test
    @SneakyThrows
    void searchItem_whenIndexTypeConfigurationNotFound_returnsNotFound() {
        when(configurationRepository.findByName(INDEX_TYPE)).thenReturn(Optional.empty());

        Optional<SearchItemContainer> searchItemContainer = archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, ORIGIN_ID, null);

        assertThat(searchItemContainer).isEmpty();

        verify(archiveDataObjectStore, never()).retrieveObject(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    void searchItem_whenArtifactNotFound_returnsNotFound() {

        @SuppressWarnings("unchecked")
        ArchiveTypeToSearchItemConverter<Object> converter = mock(ArchiveTypeToSearchItemConverter.class);

        IndexTypeConfiguration config = buildIndexTypeConfigurationWithConverter(converter);
        when(configurationRepository.findByName(INDEX_TYPE)).thenReturn(Optional.of(config));

        StorageObjectProperties objectProperties = StorageObjectProperties.builder().versionId(ORIGIN_VERSION).build();
        when(archiveDataObjectStore.getObjectProperties(OBJECT_BUCKET, OBJECT_KEY))
                .thenReturn(Optional.of(objectProperties));

        when(archiveDataObjectStore.retrieveObject(any(), eq(OBJECT_BUCKET), eq(OBJECT_KEY), eq(null)))
                .thenReturn(Optional.empty());

        Optional<SearchItemContainer> searchItemContainer = archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, ORIGIN_ID, null);

        assertThat(searchItemContainer).isEmpty();
    }

    @Test
    @SneakyThrows
    void searchItem_whenArtifactPropertiesNotFound_returnsNotFound() {

        @SuppressWarnings("unchecked")
        ArchiveTypeToSearchItemConverter<Object> converter = mock(ArchiveTypeToSearchItemConverter.class);

        IndexTypeConfiguration config = buildIndexTypeConfigurationWithConverter(converter);
        when(configurationRepository.findByName(INDEX_TYPE)).thenReturn(Optional.of(config));

        Optional<SearchItemContainer> searchItemContainer = archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, ORIGIN_ID, null);

        assertThat(searchItemContainer).isEmpty();

        verify(archiveDataObjectStore, never()).retrieveObject(any(), any(), any(), any());
    }

    @Test
    @SneakyThrows
    void searchItem_whenBadInput_throwsException() {
        assertThrows(SearchItemsBadInputException.class, () ->archivedArtifactSearchItemsProvider.findSearchItem(INDEX_TYPE, "foo", null));

        verify(archiveDataObjectStore, never()).retrieveObject(any(), any(), any(), any());
        verify(configurationRepository, never()).findByName(anyString());
    }

    private IndexTypeConfiguration buildIndexTypeConfiguration(ArchiveTypeToSearchItemConverter<Object> converter) {
        return new IndexTypeConfiguration(INDEX_TYPE, Object.class, converter);
    }

    private IndexTypeConfiguration buildIndexTypeConfigurationWithConverter(ArchiveTypeToSearchItemConverter<Object> converter) {
        return buildIndexTypeConfiguration(converter);
    }
}
