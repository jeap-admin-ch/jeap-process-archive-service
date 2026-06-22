package ch.admin.bit.jeap.processarchive.domain.backfill;

import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackfillJobServiceTest {

    private static final UUID JOB_ID = UUID.fromString("2ad48b66-472b-4a83-8efe-66a5f53ca111");
    private static final String MESSAGE_NAME = "JmeDecreeDocumentCreatedEvent";
    private static final String TOPIC_NAME = "jme-process-archive-decreedocumentcreated";

    @Mock
    private MessageArchiveConfigurationRepository configurationRepository;
    @Mock
    private BackfillJobPort backfillJobPort;
    @Mock
    private BackfillCommandPublisher backfillCommandPublisher;

    @InjectMocks
    private BackfillJobService backfillJobService;

    @Test
    void submitBackfillJob_validRequest_persistsOpenJobAndPublishesCommands() {
        BackfillJobSubmission submission = submission();
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.of(remoteDataConfiguration()));
        when(backfillJobPort.existsById(JOB_ID)).thenReturn(false);

        backfillJobService.submitBackfillJob(submission);

        ArgumentCaptor<BackfillJob> jobCaptor = ArgumentCaptor.forClass(BackfillJob.class);
        verify(backfillJobPort).saveJob(jobCaptor.capture());
        BackfillJob savedJob = jobCaptor.getValue();
        assertEquals(JOB_ID, savedJob.jobId());
        assertEquals(MESSAGE_NAME, savedJob.messageName());
        assertEquals(TOPIC_NAME, savedJob.topicName());
        assertEquals(BackfillJobState.OPEN, savedJob.jobState());
        assertNull(savedJob.jobResult());
        assertNull(savedJob.reportCreatedAt());
        assertEquals("Jane DevOps", savedJob.startedByName());
        assertEquals("U12345", savedJob.startedByExtId());
        assertEquals(2, savedJob.tasks().size());
        assertTrue(savedJob.tasks().stream().allMatch(task -> task.taskState() == BackfillTaskState.OPEN));

        ArgumentCaptor<CreateArtifactCommandData> commandCaptor = ArgumentCaptor.forClass(CreateArtifactCommandData.class);
        verify(backfillCommandPublisher, times(2)).publish(commandCaptor.capture());
        assertEquals(List.of("DOC-2024-001", "DOC-2024-002"),
                commandCaptor.getAllValues().stream().map(CreateArtifactCommandData::referenceId).toList());
    }

    @Test
    void submitBackfillJob_withoutReferenceVersion_persistsTaskAndPublishesCommandWithoutVersion() {
        BackfillJobSubmission submission = new BackfillJobSubmission(JOB_ID, MESSAGE_NAME, TOPIC_NAME,
                List.of(new BackfillArchiveDataReference("DOC-2024-001", null)),
                new BackfillJobSubmitter("Jane DevOps", "U12345"));
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.of(remoteDataConfiguration()));
        when(backfillJobPort.existsById(JOB_ID)).thenReturn(false);

        backfillJobService.submitBackfillJob(submission);

        ArgumentCaptor<BackfillJob> jobCaptor = ArgumentCaptor.forClass(BackfillJob.class);
        verify(backfillJobPort).saveJob(jobCaptor.capture());
        assertNull(jobCaptor.getValue().tasks().getFirst().referenceVersion());

        ArgumentCaptor<CreateArtifactCommandData> commandCaptor = ArgumentCaptor.forClass(CreateArtifactCommandData.class);
        verify(backfillCommandPublisher).publish(commandCaptor.capture());
        assertEquals("DOC-2024-001", commandCaptor.getValue().referenceId());
        assertNull(commandCaptor.getValue().referenceVersion());
    }

    @Test
    void submitBackfillJob_existingJobWithSameContent_isIdempotent() {
        BackfillJobSubmission submission = submission();
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.of(remoteDataConfiguration()));
        when(backfillJobPort.existsById(JOB_ID)).thenReturn(true);
        when(backfillJobPort.findById(JOB_ID)).thenReturn(Optional.of(existingJob()));

        backfillJobService.submitBackfillJob(submission);

        verify(backfillJobPort, never()).saveJob(any());
        verifyNoInteractions(backfillCommandPublisher);
    }

    @Test
    void submitBackfillJob_existingJobWithDifferentContent_throwsConflict() {
        BackfillJobSubmission submission = new BackfillJobSubmission(JOB_ID, MESSAGE_NAME, TOPIC_NAME,
                List.of(new BackfillArchiveDataReference("DOC-2024-003", 1)), submission().submitter());
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.of(remoteDataConfiguration()));
        when(backfillJobPort.existsById(JOB_ID)).thenReturn(true);
        when(backfillJobPort.findById(JOB_ID)).thenReturn(Optional.of(existingJob()));

        BackfillJobException exception = assertThrows(BackfillJobException.class,
                () -> backfillJobService.submitBackfillJob(submission));

        assertEquals(BackfillJobExceptionReason.CONFLICT, exception.getReason());
        verify(backfillJobPort, never()).saveJob(any());
        verifyNoInteractions(backfillCommandPublisher);
    }

    @Test
    void submitBackfillJob_missingConfiguration_throwsBadRequest() {
        BackfillJobSubmission submission = submission();
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.empty());

        BackfillJobException exception = assertThrows(BackfillJobException.class,
                () -> backfillJobService.submitBackfillJob(submission));

        assertEquals(BackfillJobExceptionReason.CONFIGURATION_NOT_FOUND, exception.getReason());
        verifyNoInteractions(backfillCommandPublisher);
    }

    @Test
    void submitBackfillJob_configurationIsNotRemoteData_throwsBadRequest() {
        BackfillJobSubmission submission = submission();
        MessageArchiveDataProvider<?> dataProvider = mock(MessageArchiveDataProvider.class);
        when(configurationRepository.findByName(MESSAGE_NAME)).thenReturn(Optional.of(PayloadDataMessageArchiveConfiguration.builder()
                .messageName(MESSAGE_NAME)
                .topicName(TOPIC_NAME)
                .messageArchiveDataProvider((MessageArchiveDataProvider) dataProvider)
                .build()));

        BackfillJobException exception = assertThrows(BackfillJobException.class,
                () -> backfillJobService.submitBackfillJob(submission));

        assertEquals(BackfillJobExceptionReason.CONFIGURATION_NOT_REMOTE_DATA, exception.getReason());
        verifyNoInteractions(backfillCommandPublisher);
    }

    @Test
    void submitBackfillJob_emptyReferences_throwsBadRequest() {
        BackfillJobSubmission submission = new BackfillJobSubmission(JOB_ID, MESSAGE_NAME, TOPIC_NAME, List.of(),
                new BackfillJobSubmitter("Jane DevOps", "U12345"));

        BackfillJobException exception = assertThrows(BackfillJobException.class,
                () -> backfillJobService.submitBackfillJob(submission));

        assertEquals(BackfillJobExceptionReason.INVALID_REQUEST, exception.getReason());
        verifyNoInteractions(backfillJobPort, backfillCommandPublisher);
    }

    @Test
    void submitBackfillJob_nonPositiveReferenceVersion_throwsBadRequest() {
        BackfillJobException exception = assertThrows(BackfillJobException.class,
                () -> new BackfillArchiveDataReference("DOC-2024-001", 0));

        assertEquals(BackfillJobExceptionReason.INVALID_REQUEST, exception.getReason());
    }

    private BackfillJobSubmission submission() {
        return new BackfillJobSubmission(JOB_ID, MESSAGE_NAME, TOPIC_NAME,
                List.of(
                        new BackfillArchiveDataReference("DOC-2024-001", 1),
                        new BackfillArchiveDataReference("DOC-2024-002", 1)),
                new BackfillJobSubmitter("Jane DevOps", "U12345"));
    }

    private BackfillJob existingJob() {
        return new BackfillJob(JOB_ID, MESSAGE_NAME, TOPIC_NAME, BackfillJobState.OPEN, null, null, null,
                "Other User", "U00000",
                List.of(
                        new BackfillTask(1L, "DOC-2024-002", 1, BackfillTaskState.OPEN, null, null),
                        new BackfillTask(2L, "DOC-2024-001", 1, BackfillTaskState.OPEN, null, null)));
    }

    private RemoteDataMessageArchiveConfiguration remoteDataConfiguration() {
        return RemoteDataMessageArchiveConfiguration.builder()
                .messageName(MESSAGE_NAME)
                .topicName(TOPIC_NAME)
                .dataReaderEndpoint("http://source-service/{id}/{version}")
                .remoteArchiveDataProvider(mock(RemoteArchiveDataProvider.class))
                .meterRegistry(new SimpleMeterRegistry())
                .build();
    }
}
