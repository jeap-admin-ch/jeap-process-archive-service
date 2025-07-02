package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.DomainEventIdentity;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.togglz.spring.boot.actuate.autoconfigure.TogglzAutoConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = {DomainEventArchiveService.class, TogglzAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = "togglz.features.FEATURE_ACTIVE.enabled=true")
class DomainEventArchiveServiceIT {

    @Autowired
    private DomainEventArchiveService domainEventArchiveService;

    @MockitoBean
    private ArchiveDataObjectStore archiveDataObjectStore;

    @MockitoBean
    private ArchiveDataSchemaValidationService archiveDataSchemaValidationService;

    @MockitoBean
    private ArtifactArchivedListener artifactArchivedListener;

    private static final Integer DATA_VERSION = 1;
    private static final String REFERENCE_ID = "referenceId";
    private static final String BUCKET = "bucket";
    private static final String PREFIX = "prefix";
    private static final String KEY = PREFIX + REFERENCE_ID;
    private static final String VERSION_ID = "object-version-id";
    private static final String CONTENT_TYPE = "avro/binary";
    private static final ArchiveData ARCHIVE_DATA = ArchiveData.builder()
            .system("JME")
            .schema("MySchema")
            .schemaVersion(1)
            .referenceId(REFERENCE_ID)
            .version(DATA_VERSION)
            .payload("payload".getBytes(StandardCharsets.UTF_8))
            .contentType(CONTENT_TYPE)
            .storageBucket(Optional.of(BUCKET))
            .storagePrefix(Optional.of(PREFIX))
            .metadata(List.of())
            .build();
    private static final ArchiveDataSchema ARCHIVE_DATA_SCHEMA = ArchiveDataSchema.builder()
            .system("JME")
            .name("MySchema")
            .referenceIdType("ch.admin.bit.jeap.audit.type.MySchemaArchive")
            .version(1)
            .fileExtension("avpr")
            .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
            .build();
    private static final String EVENT_IDEMPOTENCE_ID = UUID.randomUUID().toString();
    private static final String OBJECT_NAME = "objectName";
    private static final ArchiveDataStorageInfo ARCHIVE_DATA_STORAGE_INFO = ArchiveDataStorageInfo.builder()
            .bucket(BUCKET)
            .key(KEY)
            .versionId(VERSION_ID)
            .name(OBJECT_NAME)
            .build();

    @Mock(strictness = Mock.Strictness.LENIENT)
    DomainEvent domainEvent;
    @Mock(strictness = Mock.Strictness.LENIENT)
    MessageType messageType;
    @Mock(strictness = Mock.Strictness.LENIENT)
    DomainEventIdentity domainEventIdentity;

    @BeforeEach
    void beforeEach() {
        when(domainEvent.getType()).thenReturn(messageType);
        when(messageType.getName()).thenReturn("TestEvent");
        when(domainEvent.getIdentity()).thenReturn(domainEventIdentity);
        when(domainEventIdentity.getId()).thenReturn("test-event-id");
        when(domainEventIdentity.getIdempotenceId()).thenReturn(EVENT_IDEMPOTENCE_ID);
        when(archiveDataObjectStore.store(ARCHIVE_DATA, ARCHIVE_DATA_SCHEMA)).thenReturn(ARCHIVE_DATA_STORAGE_INFO);
        doReturn(ARCHIVE_DATA_SCHEMA).when(archiveDataSchemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);

    }

    @Test
    void archive_featureFlagIsActive_artifactListenerCalled() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));

        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .featureFlag("FEATURE_ACTIVE")
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        verify(artifactArchivedListener, times(1)).onArtifactArchived(any());
    }

    @Test
    void archive_featureFlagNotActive_artifactListenerNotCalled() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));

        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .featureFlag("FEATURE_NOT_ACTIVE")
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        verify(artifactArchivedListener, never()).onArtifactArchived(any());
    }

    private ArchiveData domainEventDataProviderStub(DomainEvent domainEvent) {
        return ARCHIVE_DATA;
    }
}
