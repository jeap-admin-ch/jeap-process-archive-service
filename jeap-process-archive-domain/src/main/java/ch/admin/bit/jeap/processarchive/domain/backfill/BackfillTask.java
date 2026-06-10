package ch.admin.bit.jeap.processarchive.domain.backfill;

public record BackfillTask(Long id,
                           String referenceId,
                           Integer referenceVersion,
                           BackfillTaskState taskState,
                           String errorMessage,
                           String errorTraceId) {
}
