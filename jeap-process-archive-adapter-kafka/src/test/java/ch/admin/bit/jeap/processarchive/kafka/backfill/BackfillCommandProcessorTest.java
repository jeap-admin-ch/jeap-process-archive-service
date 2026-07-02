package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillTaskEntity;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommand;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.backfill.*;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BackfillCommandProcessorTest {

    private static final UUID JOB_ID = UUID.fromString("81d9f182-8d8f-4351-ab39-eae1bf79e84a");
    private static final String MESSAGE_NAME = "message-name";
    private static final String TOPIC_NAME = "topic-name";
    private static final String REFERENCE_ID = "reference-id";
    private static final int REFERENCE_VERSION = 3;

    private final MessageArchiveConfigurationRepository configurationRepository = mock(MessageArchiveConfigurationRepository.class);
    private final ArchiveDataSchemaValidationService schemaValidationService = mock(ArchiveDataSchemaValidationService.class);
    private final ArchiveDataObjectStore archiveDataObjectStore = mock(ArchiveDataObjectStore.class);
    private final ArchivedArtifactCreatedEventProducer eventProducer = mock(ArchivedArtifactCreatedEventProducer.class);
    private final BackfillJobRepository backfillJobRepository = mock(BackfillJobRepository.class);
    private final RemoteArchiveDataProvider remoteArchiveDataProvider = mock(RemoteArchiveDataProvider.class);

    private final BackfillCommandProcessor processor = new BackfillCommandProcessor(
            configurationRepository,
            schemaValidationService,
            archiveDataObjectStore,
            eventProducer,
            backfillJobRepository);

    @Test
    void processCommandArchivesRemoteDataAndCompletesJobWhenAllTasksSucceeded() {
        CreateArtifactCommand command = createCommand();
        ArchiveData archiveData = createArchiveData();
        ArchiveDataSchema schema = createSchema();
        ArchiveDataStorageInfo storageInfo = createStorageInfo();
        BackfillJobEntity job = createJob(createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN));
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(createConfiguration()));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any())).thenReturn(archiveData);
        when(schemaValidationService.validateArchiveDataSchema(archiveData)).thenReturn(schema);
        when(archiveDataObjectStore.store(archiveData, schema)).thenReturn(storageInfo);
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));

        processor.processCommand(command);

        ArgumentCaptor<ArchiveDataReference> referenceCaptor = ArgumentCaptor.forClass(ArchiveDataReference.class);
        verify(remoteArchiveDataProvider).readArchiveData(eq("endpoint"), eq("oauth-client"), referenceCaptor.capture());
        assertThat(referenceCaptor.getValue().getId()).isEqualTo(REFERENCE_ID);
        assertThat(referenceCaptor.getValue().getVersion()).isEqualTo(REFERENCE_VERSION);

        ArgumentCaptor<ArchivedArtifact> artifactCaptor = ArgumentCaptor.forClass(ArchivedArtifact.class);
        verify(eventProducer).onArchivedArtifact(artifactCaptor.capture());
        ArchivedArtifact artifact = artifactCaptor.getValue();
        assertThat(artifact.getArchiveData()).isSameAs(archiveData);
        assertThat(artifact.getProcessId()).isEqualTo(JOB_ID.toString());
        assertThat(artifact.getIdempotenceId()).isEqualTo("CreateArtifactCommand_" + command.getIdentity().getIdempotenceId());
        assertThat(artifact.getReferenceIdType()).isEqualTo("reference-id-type");
        assertThat(artifact.getStorageObjectBucket()).isEqualTo("bucket");
        assertThat(artifact.getStorageObjectKey()).isEqualTo("key");
        assertThat(artifact.getStorageObjectId()).isEqualTo("name");
        assertThat(artifact.getStorageObjectVersionId()).isEqualTo("version-id");
        assertThat(artifact.getExpirationDays()).isEqualTo(90);

        assertThat(job.getTasks().getFirst().getTaskState()).isEqualTo(BackfillTaskState.SUCCEEDED);
        assertThat(job.getJobState()).isEqualTo(BackfillJobState.COMPLETED);
        assertThat(job.getJobResult()).isEqualTo(BackfillJobResult.SUCCEEDED);
        assertThat(job.getReportCreatedAt()).isNotNull();
        verify(backfillJobRepository).save(job);
    }

    @Test
    void processCommandKeepsJobOpenWhenOtherTasksAreOpen() {
        BackfillTaskEntity completedTask = createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN);
        BackfillTaskEntity openTask = createTask("other-reference-id", 1, BackfillTaskState.OPEN);
        BackfillJobEntity job = createJob(completedTask, openTask);
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(createConfiguration()));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any())).thenReturn(createArchiveData());
        ArchiveDataSchema schema = createSchema();
        when(schemaValidationService.validateArchiveDataSchema(any())).thenReturn(schema);
        when(archiveDataObjectStore.store(any(), eq(schema))).thenReturn(createStorageInfo());
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));

        processor.processCommand(createCommand());

        assertThat(completedTask.getTaskState()).isEqualTo(BackfillTaskState.SUCCEEDED);
        assertThat(openTask.getTaskState()).isEqualTo(BackfillTaskState.OPEN);
        assertThat(job.getJobState()).isEqualTo(BackfillJobState.OPEN);
        assertThat(job.getJobResult()).isNull();
        assertThat(job.getReportCreatedAt()).isNull();
        verify(backfillJobRepository).save(job);
    }

    @Test
    void processCommandDoesNotUpdateStateWhenRemoteProviderReturnsNoData() {
        BackfillJobEntity job = createJob(createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN));
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(createConfiguration()));
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any())).thenReturn(null);

        processor.processCommand(createCommand());

        assertThat(job.getTasks().getFirst().getTaskState()).isEqualTo(BackfillTaskState.OPEN);
        assertThat(job.getJobState()).isEqualTo(BackfillJobState.OPEN);
        assertThat(job.getJobResult()).isNull();
        verifyNoInteractions(schemaValidationService, archiveDataObjectStore, eventProducer);
        verify(backfillJobRepository, never()).save(any());
    }

    @Test
    void processCommandWithoutReferenceVersionCallsRemoteProviderWithoutVersion() {
        CreateArtifactCommand command = createCommandWithoutVersion();
        BackfillJobEntity job = createJob(createTask(REFERENCE_ID, null, BackfillTaskState.OPEN));
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(createConfiguration()));
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any())).thenReturn(null);

        processor.processCommand(command);

        assertThat(command.getIdentity().getIdempotenceId()).isEqualTo(JOB_ID + "-12:" + REFERENCE_ID + ":none");
        assertThat(command.getReferences().getArchiveData().getReferenceVersion()).isNull();
        ArgumentCaptor<ArchiveDataReference> referenceCaptor = ArgumentCaptor.forClass(ArchiveDataReference.class);
        verify(remoteArchiveDataProvider).readArchiveData(eq("endpoint"), eq("oauth-client"), referenceCaptor.capture());
        assertThat(referenceCaptor.getValue().getId()).isEqualTo(REFERENCE_ID);
        assertThat(referenceCaptor.getValue().getVersion()).isNull();
        verify(backfillJobRepository, never()).save(any());
    }

    @Test
    void createCommandUsesDistinctIdempotenceIdsForVersionlessAndDashVersionedReferences() {
        CreateArtifactCommand versionlessReferenceWithDash = createCommand("reference-id-3", null);
        CreateArtifactCommand versionedReference = createCommand("reference-id", 3);

        assertThat(versionlessReferenceWithDash.getIdentity().getIdempotenceId())
                .isEqualTo(JOB_ID + "-14:reference-id-3:none");
        assertThat(versionedReference.getIdentity().getIdempotenceId())
                .isEqualTo(JOB_ID + "-12:reference-id:v3");
        assertThat(versionlessReferenceWithDash.getIdentity().getIdempotenceId())
                .isNotEqualTo(versionedReference.getIdentity().getIdempotenceId());
    }

    @Test
    void processCommandMarksTaskAndJobFailedWhenArchiveDataProcessingFails() {
        BackfillTaskEntity task = createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN);
        BackfillJobEntity job = createJob(task);
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(createConfiguration()));
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("remote data unavailable"));

        processor.processCommand(createCommand());

        assertThat(task.getTaskState()).isEqualTo(BackfillTaskState.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("remote data unavailable");
        assertThat(job.getJobState()).isEqualTo(BackfillJobState.COMPLETED);
        assertThat(job.getJobResult()).isEqualTo(BackfillJobResult.FAILED);
        assertThat(job.getReportCreatedAt()).isNotNull();
        verify(backfillJobRepository).save(job);
        verifyNoInteractions(schemaValidationService, archiveDataObjectStore, eventProducer);
    }

    @Test
    void processCommandThrowsWhenMultipleRemoteDataConfigurationsRegisteredForSameTopicAndJobHasNoConfigId() {
        BackfillJobEntity job = createJob(createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN));
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(configurationRepository.findByName(MESSAGE_NAME))
                .thenReturn(List.of(createConfiguration(), createConfiguration()));

        CreateArtifactCommand command = createCommand();
        assertThrows(BackfillJobException.class, () -> processor.processCommand(command));

        verifyNoInteractions(remoteArchiveDataProvider, schemaValidationService, archiveDataObjectStore, eventProducer);
    }

    @Test
    void processCommandSelectsRemoteDataConfigurationByJobConfigId() {
        ArchiveData archiveData = createArchiveData();
        ArchiveDataSchema schema = createSchema();
        BackfillJobEntity job = createJob(createTask(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.OPEN));
        job.setConfigId("config-b");
        RemoteDataMessageArchiveConfiguration configA = configurationWithIdAndEndpoint("config-a", "endpoint-a");
        RemoteDataMessageArchiveConfiguration configB = configurationWithIdAndEndpoint("config-b", "endpoint-b");
        when(backfillJobRepository.findWithTasksByJobId(JOB_ID)).thenReturn(Optional.of(job));
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(List.of(configA, configB));
        when(remoteArchiveDataProvider.readArchiveData(any(), any(), any())).thenReturn(archiveData);
        when(schemaValidationService.validateArchiveDataSchema(archiveData)).thenReturn(schema);
        when(archiveDataObjectStore.store(archiveData, schema)).thenReturn(createStorageInfo());

        processor.processCommand(createCommand());

        // The configuration selected by the job's config-id (config-b, endpoint-b) is used
        verify(remoteArchiveDataProvider).readArchiveData(eq("endpoint-b"), eq("oauth-client"), any());
        verify(eventProducer).onArchivedArtifact(any());
        assertThat(job.getTasks().getFirst().getTaskState()).isEqualTo(BackfillTaskState.SUCCEEDED);
    }

    private CreateArtifactCommand createCommand() {
        return createCommand(REFERENCE_VERSION);
    }

    private CreateArtifactCommand createCommandWithoutVersion() {
        return createCommand(null);
    }

    private CreateArtifactCommand createCommand(Integer referenceVersion) {
        return createCommand(REFERENCE_ID, referenceVersion);
    }

    private CreateArtifactCommand createCommand(String referenceId, Integer referenceVersion) {
        BackfillCommandProperties properties = new BackfillCommandProperties();
        properties.setSystemName("system");
        properties.setServiceName("service");
        return CreateArtifactCommandBuilder.builder(properties)
                .commandData(new CreateArtifactCommandData(JOB_ID, referenceId, referenceVersion, MESSAGE_NAME, TOPIC_NAME))
                .build();
    }

    private RemoteDataMessageArchiveConfiguration createConfiguration() {
        return createConfiguration(TOPIC_NAME);
    }

    private RemoteDataMessageArchiveConfiguration createConfiguration(String topicName) {
        return RemoteDataMessageArchiveConfiguration.builder()
                .messageName(MESSAGE_NAME)
                .topicName(topicName)
                .dataReaderEndpoint("endpoint")
                .oauthClientId("oauth-client")
                .remoteArchiveDataProvider(remoteArchiveDataProvider)
                .meterRegistry(mock(MeterRegistry.class))
                .build();
    }

    private RemoteDataMessageArchiveConfiguration configurationWithIdAndEndpoint(String id, String endpoint) {
        return RemoteDataMessageArchiveConfiguration.builder()
                .id(id)
                .messageName(MESSAGE_NAME)
                .topicName(TOPIC_NAME)
                .dataReaderEndpoint(endpoint)
                .oauthClientId("oauth-client")
                .remoteArchiveDataProvider(remoteArchiveDataProvider)
                .meterRegistry(mock(MeterRegistry.class))
                .build();
    }

    private ArchiveData createArchiveData() {
        return ArchiveData.builder()
                .contentType("application/json")
                .system("system")
                .schema("schema")
                .schemaVersion(1)
                .referenceId(REFERENCE_ID)
                .version(REFERENCE_VERSION)
                .payload("payload".getBytes())
                .build();
    }

    private ArchiveDataSchema createSchema() {
        return ArchiveDataSchema.builder()
                .system("system")
                .name("schema")
                .referenceIdType("reference-id-type")
                .version(1)
                .expirationDays(90)
                .build();
    }

    private ArchiveDataStorageInfo createStorageInfo() {
        return ArchiveDataStorageInfo.builder()
                .bucket("bucket")
                .key("key")
                .name("name")
                .versionId("version-id")
                .build();
    }

    private BackfillJobEntity createJob(BackfillTaskEntity... tasks) {
        BackfillJobEntity job = new BackfillJobEntity();
        job.setJobId(JOB_ID);
        job.setMessageName(MESSAGE_NAME);
        job.setJobState(BackfillJobState.OPEN);
        job.setStartedAt(Instant.now());
        for (BackfillTaskEntity task : tasks) {
            task.setJob(job);
            job.getTasks().add(task);
        }
        return job;
    }

    private BackfillTaskEntity createTask(String referenceId, Integer referenceVersion, BackfillTaskState state) {
        BackfillTaskEntity task = new BackfillTaskEntity();
        task.setReferenceId(referenceId);
        task.setReferenceVersion(referenceVersion);
        task.setTaskState(state);
        return task;
    }
}
