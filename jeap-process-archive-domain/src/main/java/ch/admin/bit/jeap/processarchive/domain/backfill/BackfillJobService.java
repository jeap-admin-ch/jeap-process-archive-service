package ch.admin.bit.jeap.processarchive.domain.backfill;

import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnBean({BackfillJobPort.class, BackfillCommandPublisher.class})
@RequiredArgsConstructor
@Slf4j
public class BackfillJobService {

    private final MessageArchiveConfigurationRepository configurationRepository;
    private final BackfillJobPort backfillJobPort;
    private final BackfillCommandPublisher backfillCommandPublisher;

    @Transactional
    public void submitBackfillJob(BackfillJobSubmission submission) {
        validateRequest(submission);
        MessageArchiveConfiguration configuration = validateArchiveConfiguration(submission.messageName(), submission.topicName());

        if (backfillJobPort.existsById(submission.jobId())) {
            BackfillJob existingJob = backfillJobPort.findById(submission.jobId())
                    .orElseThrow(() -> BackfillJobException.conflict("Backfill job already exists but could not be loaded: " + submission.jobId()));
            if (hasSameContent(existingJob, submission)) {
                log.info("Backfill job '{}' already exists with same content. Treating request as idempotent.", submission.jobId());
                return;
            }
            throw BackfillJobException.conflict("Backfill job '%s' already exists with different content".formatted(submission.jobId()));
        }

        BackfillJob job = createOpenJob(submission);
        backfillJobPort.saveJob(job);
        publishCommands(job, configuration);
    }

    private void validateRequest(BackfillJobSubmission submission) {
        if (submission == null) {
            throw BackfillJobException.invalidRequest("Backfill job request must not be null");
        }
        if (submission.jobId() == null) {
            throw BackfillJobException.invalidRequest("jobId must not be null");
        }
        if (!StringUtils.hasText(submission.messageName())) {
            throw BackfillJobException.invalidRequest("message must not be empty");
        }
        if (!StringUtils.hasText(submission.topicName())) {
            throw BackfillJobException.invalidRequest("topic must not be empty");
        }
        if (CollectionUtils.isEmpty(submission.archiveDataReferences())) {
            throw BackfillJobException.invalidRequest("archiveDataReferences must not be empty");
        }
    }

    private MessageArchiveConfiguration validateArchiveConfiguration(String messageName, String topicName) {
        MessageArchiveConfiguration configuration = configurationRepository.findByName(messageName)
                .filter(config -> topicName.equals(config.getTopicName()))
                .orElseThrow(() -> BackfillJobException.configurationNotFound(messageName, topicName));

        if (!(configuration instanceof RemoteDataMessageArchiveConfiguration)) {
            throw BackfillJobException.configurationNotRemoteData(messageName, topicName);
        }
        return configuration;
    }

    private BackfillJob createOpenJob(BackfillJobSubmission submission) {
        BackfillJobSubmitter submitter = submission.submitter() == null ? new BackfillJobSubmitter(null, null) : submission.submitter();
        List<BackfillTask> tasks = submission.archiveDataReferences().stream()
                .map(reference -> new BackfillTask(null, reference.id(), reference.version(), BackfillTaskState.OPEN, null, null))
                .toList();

        return new BackfillJob(
                submission.jobId(),
                submission.messageName(),
                submission.topicName(),
                BackfillJobState.OPEN,
                null,
                Instant.now(),
                null,
                submitter.name(),
                submitter.extId(),
                tasks);
    }

    private void publishCommands(BackfillJob job, MessageArchiveConfiguration configuration) {
        job.tasks().forEach(task -> backfillCommandPublisher.publish(new CreateArtifactCommandData(
                job.jobId(),
                task.referenceId(),
                task.referenceVersion(),
                configuration.getMessageName(),
                configuration.getTopicName())));
    }

    private boolean hasSameContent(BackfillJob existingJob, BackfillJobSubmission submission) {
        return existingJob.messageName().equals(submission.messageName()) &&
                existingJob.topicName().equals(submission.topicName()) &&
                sortedReferences(existingJob.tasks()).equals(sortedReferencesFromSubmission(submission));
    }

    private List<BackfillArchiveDataReference> sortedReferences(List<BackfillTask> tasks) {
        return tasks.stream()
                .map(task -> new BackfillArchiveDataReference(task.referenceId(), task.referenceVersion()))
                .sorted()
                .toList();
    }

    private List<BackfillArchiveDataReference> sortedReferencesFromSubmission(BackfillJobSubmission submission) {
        return submission.archiveDataReferences().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
