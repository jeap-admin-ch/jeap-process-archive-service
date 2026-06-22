package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillTaskEntity;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommand;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataStorageInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTaskState;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(value = "jeap.processarchive.backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class BackfillCommandProcessor {

    private final MessageArchiveConfigurationRepository configurationRepository;
    private final ArchiveDataSchemaValidationService schemaValidationService;
    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final ArchivedArtifactCreatedEventProducer eventProducer;
    private final BackfillJobRepository backfillJobRepository;

    @Transactional
    public void processCommand(CreateArtifactCommand command) {
        UUID jobId = UUID.fromString(command.getPayload().getJobId());
        String messageName = command.getPayload().getMessageName();
        String referenceId = command.getReferences().getArchiveData().getReferenceId();
        Integer referenceVersion = command.getReferences().getArchiveData().getReferenceVersion();

        log.debug("Processing CreateArtifactCommand for backfill job '{}' and reference '{}' version '{}'.",
                jobId, referenceId, referenceVersion);

        RemoteDataMessageArchiveConfiguration configuration = loadRemoteDataConfiguration(messageName);
        BackfillJobEntity job = loadBackfillJob(jobId);
        BackfillTaskEntity task = findTask(job, referenceId, referenceVersion);

        try {
            ArchiveData archiveData = readArchiveData(configuration, referenceId, referenceVersion);
            if (archiveData == null) {
                log.info("No data to archive for backfill job '{}' and reference '{}' version '{}'.", jobId, referenceId, referenceVersion);
                return;
            }
            ArchiveDataSchema schema = schemaValidationService.validateArchiveDataSchema(archiveData);
            ArchiveDataStorageInfo storageInfo = archiveDataObjectStore.store(archiveData, schema);

            eventProducer.onArchivedArtifact(createArchivedArtifact(command, archiveData, schema, storageInfo));
            markTaskSucceeded(job, task);
        } catch (RuntimeException e) {
            markTaskFailed(job, task, e);
        }
    }

    private RemoteDataMessageArchiveConfiguration loadRemoteDataConfiguration(String messageName) {
        MessageArchiveConfiguration configuration = configurationRepository.findByName(messageName)
                .orElseThrow(() -> new IllegalStateException("No archive configuration found for message '%s'".formatted(messageName)));
        if (configuration instanceof RemoteDataMessageArchiveConfiguration remoteDataConfiguration) {
            return remoteDataConfiguration;
        }
        throw new IllegalStateException("Archive configuration for message '%s' is not a remote data configuration".formatted(messageName));
    }

    private ArchiveData readArchiveData(RemoteDataMessageArchiveConfiguration configuration, String referenceId, Integer referenceVersion) {
        ArchiveDataReference reference = ArchiveDataReference.builder()
                .id(referenceId)
                .version(referenceVersion)
                .build();
        return configuration.getRemoteArchiveDataProvider().readArchiveData(
                configuration.getDataReaderEndpoint(),
                configuration.getOauthClientId(),
                reference);
    }

    private BackfillJobEntity loadBackfillJob(UUID jobId) {
        return backfillJobRepository.findWithTasksByJobId(jobId)
                .orElseThrow(() -> new IllegalStateException("Backfill job '%s' not found".formatted(jobId)));
    }

    private BackfillTaskEntity findTask(BackfillJobEntity job, String referenceId, Integer referenceVersion) {
        return job.getTasks().stream()
                .filter(candidate -> referenceId.equals(candidate.getReferenceId()))
                .filter(candidate -> Objects.equals(referenceVersion, candidate.getReferenceVersion()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Backfill task for job '%s', reference '%s' version '%s' not found".formatted(job.getJobId(), referenceId, referenceVersion)));
    }

    private ArchivedArtifact createArchivedArtifact(CreateArtifactCommand command, ArchiveData archiveData,
                                                    ArchiveDataSchema schema, ArchiveDataStorageInfo storageInfo) {
        return ArchivedArtifact.builder()
                .archiveData(archiveData)
                .idempotenceId(command.getType().getName() + "_" + command.getIdentity().getIdempotenceId())
                .referenceIdType(schema.getReferenceIdType())
                .processId(command.getProcessId())
                .storageObjectBucket(storageInfo.getBucket())
                .storageObjectKey(storageInfo.getKey())
                .storageObjectId(storageInfo.getName())
                .storageObjectVersionId(storageInfo.getVersionId())
                .expirationDays(schema.getExpirationDays())
                .build();
    }

    private void markTaskSucceeded(BackfillJobEntity job, BackfillTaskEntity task) {
        task.setTaskState(BackfillTaskState.SUCCEEDED);
        updateJobStateIfAllTasksFinished(job);
        backfillJobRepository.save(job);
    }

    private void markTaskFailed(BackfillJobEntity job, BackfillTaskEntity task, RuntimeException exception) {
        log.warn("Failed to process CreateArtifactCommand for backfill job '{}' and reference '{}' version '{}'.",
                job.getJobId(), task.getReferenceId(), task.getReferenceVersion(), exception);
        task.setTaskState(BackfillTaskState.FAILED);
        task.setErrorMessage(errorMessage(exception));
        updateJobStateIfAllTasksFinished(job);
        backfillJobRepository.save(job);
    }

    private void updateJobStateIfAllTasksFinished(BackfillJobEntity job) {
        boolean hasOpenTasks = job.getTasks().stream()
                .anyMatch(candidate -> candidate.getTaskState() == BackfillTaskState.OPEN);
        if (hasOpenTasks) {
            return;
        }

        boolean hasFailedTasks = job.getTasks().stream()
                .anyMatch(candidate -> candidate.getTaskState() == BackfillTaskState.FAILED);
        boolean hasSucceededTasks = job.getTasks().stream()
                .anyMatch(candidate -> candidate.getTaskState() == BackfillTaskState.SUCCEEDED);

        job.setJobState(BackfillJobState.COMPLETED);
        job.setReportCreatedAt(Instant.now());
        if (hasFailedTasks && hasSucceededTasks) {
            job.setJobResult(BackfillJobResult.PARTIALLY_SUCCEEDED);
        } else if (hasFailedTasks) {
            job.setJobResult(BackfillJobResult.FAILED);
        } else {
            job.setJobResult(BackfillJobResult.SUCCEEDED);
        }
    }

    private String errorMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage();
    }
}
