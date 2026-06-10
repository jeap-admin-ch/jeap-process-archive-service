package ch.admin.bit.jeap.processarchive.adapter.db;

import ch.admin.bit.jeap.processarchive.domain.backfill.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JpaBackfillJobAdapter implements BackfillJobPort {

    private final BackfillJobRepository backfillJobRepository;

    @Override
    public void saveJob(BackfillJob job) {
        backfillJobRepository.save(toEntity(job));
    }

    @Override
    public Optional<BackfillJob> findById(UUID jobId) {
        return backfillJobRepository.findWithTasksByJobId(jobId).map(this::toDomain);
    }

    @Override
    public boolean existsById(UUID jobId) {
        return backfillJobRepository.existsById(jobId);
    }

    private BackfillJobEntity toEntity(BackfillJob job) {
        BackfillJobEntity entity = new BackfillJobEntity();
        entity.setJobId(job.jobId());
        entity.setMessageName(job.messageName());
        entity.setTopicName(job.topicName());
        entity.setJobState(job.jobState());
        entity.setJobResult(job.jobResult());
        entity.setStartedAt(job.startedAt());
        entity.setReportCreatedAt(job.reportCreatedAt());
        entity.setStartedByName(job.startedByName());
        entity.setStartedByExtId(job.startedByExtId());
        job.tasks().forEach(task -> entity.addTask(toEntity(task)));
        return entity;
    }

    private BackfillTaskEntity toEntity(BackfillTask task) {
        BackfillTaskEntity entity = new BackfillTaskEntity();
        entity.setId(task.id());
        entity.setReferenceId(task.referenceId());
        entity.setReferenceVersion(task.referenceVersion());
        entity.setTaskState(task.taskState());
        entity.setErrorMessage(task.errorMessage());
        entity.setErrorTraceId(task.errorTraceId());
        return entity;
    }

    private BackfillJob toDomain(BackfillJobEntity entity) {
        return new BackfillJob(
                entity.getJobId(),
                entity.getMessageName(),
                entity.getTopicName(),
                entity.getJobState(),
                entity.getJobResult(),
                entity.getStartedAt(),
                entity.getReportCreatedAt(),
                entity.getStartedByName(),
                entity.getStartedByExtId(),
                entity.getTasks().stream()
                        .sorted(Comparator.comparing(BackfillTaskEntity::getId))
                        .map(this::toDomain)
                        .toList());
    }

    private BackfillTask toDomain(BackfillTaskEntity entity) {
        return new BackfillTask(
                entity.getId(),
                entity.getReferenceId(),
                entity.getReferenceVersion(),
                entity.getTaskState(),
                entity.getErrorMessage(),
                entity.getErrorTraceId());
    }
}
