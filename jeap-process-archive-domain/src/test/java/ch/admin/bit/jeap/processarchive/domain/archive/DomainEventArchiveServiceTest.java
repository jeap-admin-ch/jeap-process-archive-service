package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.DomainEventIdentity;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidator;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.SchemaValidationException;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventArchiveServiceTest {

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
    private static final String ARCHIVE_IDEMPOTENCE_ID = "TestEvent_" + EVENT_IDEMPOTENCE_ID;
    private static final String ENDPOINT = "endpoint";
    private static final String CLIENT_ID = "clientId";
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
    @Mock(strictness = Mock.Strictness.LENIENT)
    ArchiveDataObjectStore archiveDataObjectStore;
    @Mock(strictness = Mock.Strictness.LENIENT)
    ArchiveDataSchemaValidationService schemaValidationService;
    @Mock(strictness = Mock.Strictness.LENIENT)
    FeatureManager featureManager;

    @BeforeEach
    void beforeEach() {
        when(domainEvent.getType()).thenReturn(messageType);
        when(messageType.getName()).thenReturn("TestEvent");
        when(domainEvent.getIdentity()).thenReturn(domainEventIdentity);
        when(domainEventIdentity.getId()).thenReturn("test-event-id");
        when(domainEventIdentity.getIdempotenceId()).thenReturn(EVENT_IDEMPOTENCE_ID);
        when(archiveDataObjectStore.store(ARCHIVE_DATA, ARCHIVE_DATA_SCHEMA)).thenReturn(ARCHIVE_DATA_STORAGE_INFO);
    }

    @Test
    void archive_fromDomainEventPayload() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(1, archivedArtifacts.size());
        ArchivedArtifact archivedArtifact = archivedArtifacts.getFirst();
        assertSame(ARCHIVE_DATA, archivedArtifact.getArchiveData());
        assertEquals(ARCHIVE_IDEMPOTENCE_ID, archivedArtifact.getIdempotenceId());
        assertSame(processId, archivedArtifact.getProcessId());
        assertSame(BUCKET, archivedArtifact.getStorageObjectBucket());
        assertSame(KEY, archivedArtifact.getStorageObjectKey());
        assertSame(OBJECT_NAME, archivedArtifact.getStorageObjectId());
        assertSame(VERSION_ID, archivedArtifact.getStorageObjectVersionId());
    }

    @Test
    void archive_fromRemoteData() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        RemoteDataDomainEventArchiveConfiguration configuration = RemoteDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .remoteArchiveDataProvider(this::remoteArchiveDataProvider)
                .oauthClientId(CLIENT_ID)
                .dataReaderEndpoint(ENDPOINT)
                .referenceProvider(this::referenceProvider)
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(1, archivedArtifacts.size());
        ArchivedArtifact archivedArtifact = archivedArtifacts.getFirst();
        assertSame(ARCHIVE_DATA, archivedArtifact.getArchiveData());
        assertEquals(ARCHIVE_IDEMPOTENCE_ID, archivedArtifact.getIdempotenceId());
        assertSame(processId, archivedArtifact.getProcessId());
        assertSame(BUCKET, archivedArtifact.getStorageObjectBucket());
        assertSame(KEY, archivedArtifact.getStorageObjectKey());
        assertSame(VERSION_ID, archivedArtifact.getStorageObjectVersionId());
    }

    @Test
    void archive_doNotArchiveDataWhenConditionEvaluatesToFalse() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        RemoteDataDomainEventArchiveConfiguration configuration = RemoteDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .remoteArchiveDataProvider(this::remoteArchiveDataProvider)
                .oauthClientId(CLIENT_ID)
                .dataReaderEndpoint(ENDPOINT)
                .referenceProvider(this::referenceProvider)
                .meterRegistry(new SimpleMeterRegistry())
                .archiveDataCondition(message -> false)
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(0, archivedArtifacts.size());
    }

    @Test
    void archive_doArchiveDataWhenConditionEvaluatesToTrue() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        RemoteDataDomainEventArchiveConfiguration configuration = RemoteDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .remoteArchiveDataProvider(this::remoteArchiveDataProvider)
                .oauthClientId(CLIENT_ID)
                .dataReaderEndpoint(ENDPOINT)
                .referenceProvider(this::referenceProvider)
                .meterRegistry(new SimpleMeterRegistry())
                .archiveDataCondition(message -> true)
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(1, archivedArtifacts.size());
        assertThat(archivedArtifacts.getFirst().getProcessId()).isEqualTo(processId);
    }

    @Test
    void archive_nothingToArchive() {
        // given
        String processId = "test-process-id";
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        RemoteDataDomainEventArchiveConfiguration configuration = RemoteDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .remoteArchiveDataProvider(this::remoteArchiveDataProvider)
                .oauthClientId(CLIENT_ID)
                .dataReaderEndpoint(ENDPOINT)
                .referenceProvider(refs -> null)
                .meterRegistry(new SimpleMeterRegistry())
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(0, archivedArtifacts.size());
    }

    @Test
    void archive_whenSchemaValidationFails_shouldThrow() {
        // given
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of("test-process-id"));
        ArchiveDataSchemaValidator validatorMock = mock(ArchiveDataSchemaValidator.class);
        when(validatorMock.getContentTypes()).thenReturn(Set.of(CONTENT_TYPE));
        ArchiveDataSchemaValidationService archiveDataSchemaValidationService =
                new ArchiveDataSchemaValidationService(List.of(validatorMock));
        doThrow(SchemaValidationException.class).when(validatorMock).validatePayloadConformsToSchema(any());
        archiveDataSchemaValidationService.initValidators();
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                null, archiveDataObjectStore, archiveDataSchemaValidationService, featureManager);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .build();

        // when
        assertThrows(SchemaValidationException.class, () ->
                domainEventArchiveService.archive(configuration, domainEvent));
    }

    private ArchiveDataReference referenceProvider(MessageReferences messageReferences) {
        return ArchiveDataReference.builder().id(REFERENCE_ID).version(DATA_VERSION).build();
    }

    @Test
    void archive_whenProcessIdMissingInEvent_shouldThrow() {
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                null, archiveDataObjectStore, schemaValidationService, featureManager);

        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .build();

        try {
            domainEventArchiveService.archive(configuration, domainEvent);
            fail("Expected ProcessArchiveException");
        } catch (ProcessArchiveException ex) {
            assertTrue(ex.getMessage().contains("no process ID present in event"));
        }
    }

    @Test
    void archive_withCorrelationProvider() {
        // given
        String processId = "test-process-id";
        List<ArchivedArtifact> archivedArtifacts = new ArrayList<>();
        ArtifactArchivedListener artifactArchivedListener = archivedArtifacts::add;
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .correlationProvider(correlationProvider(processId))
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        assertEquals(1, archivedArtifacts.size());
        assertThat(archivedArtifacts.getFirst().getProcessId()).isEqualTo(processId);
    }

    @Test
    void archive_withCorrelationProvider_noProcessId_shouldThrow() {
        // given
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(), archiveDataObjectStore, schemaValidationService, featureManager);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .correlationProvider(correlationProvider(null))
                .build();

        // when
        assertThrows(ProcessArchiveException.class, () -> domainEventArchiveService.archive(configuration, domainEvent));
    }

    @Test
    void archive_featureFlagIsActive_artifactListenerCalled() {
        // given
        String processId = "test-process-id";
        ArtifactArchivedListener artifactArchivedListener = mock(ArtifactArchivedListener.class);
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        when(featureManager.isActive(new NamedFeature("myActiveFeature"))).thenReturn(true);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .featureFlag("myActiveFeature")
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        verify(artifactArchivedListener, times(1)).onArtifactArchived(any());
    }

    @Test
    void archive_featureFlagIsNotActive_artifactListenerNotCalled() {
        // given
        String processId = "test-process-id";
        ArtifactArchivedListener artifactArchivedListener = mock(ArtifactArchivedListener.class);
        DomainEventArchiveService domainEventArchiveService = new DomainEventArchiveService(
                List.of(artifactArchivedListener), archiveDataObjectStore, schemaValidationService, featureManager);
        when(domainEvent.getOptionalProcessId()).thenReturn(Optional.of(processId));
        when(featureManager.isActive(new NamedFeature("myInactiveFeature"))).thenReturn(false);
        doReturn(ARCHIVE_DATA_SCHEMA).when(schemaValidationService).validateArchiveDataSchema(ARCHIVE_DATA);
        DomainEventArchiveConfiguration configuration = PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName("topic")
                .eventName("event")
                .domainEventArchiveDataProvider(this::domainEventDataProviderStub)
                .featureFlag("myInactiveFeature")
                .build();

        // when
        domainEventArchiveService.archive(configuration, domainEvent);

        // then
        verify(artifactArchivedListener, never()).onArtifactArchived(any());
    }

    private ArchiveData remoteArchiveDataProvider(String endpointTemplate, String oauthClientId, ArchiveDataReference reference) {
        assertEquals(ENDPOINT, endpointTemplate);
        assertEquals(REFERENCE_ID, reference.getId());
        assertEquals(DATA_VERSION, reference.getVersion());
        assertEquals(CLIENT_ID, oauthClientId);
        return ARCHIVE_DATA;
    }

    private MessageCorrelationProvider<Message> correlationProvider(String processId) {
        return message -> processId;
    }

    private ArchiveData domainEventDataProviderStub(DomainEvent domainEvent) {
        return ARCHIVE_DATA;
    }
}
