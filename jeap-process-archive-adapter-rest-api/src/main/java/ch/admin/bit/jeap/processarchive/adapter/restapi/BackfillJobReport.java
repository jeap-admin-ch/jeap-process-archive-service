package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.processarchive.domain.backfill.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BackfillJobReport(
        String message,
        @JsonProperty("config-id") String configId,
        @JsonProperty("job-state") String jobState,
        @JsonProperty("job-result") String jobResult,
        @JsonProperty("job-id") UUID jobId,
        Instant started,
        @JsonProperty("report-created") Instant reportCreated,
        @JsonProperty("started-by-name") String startedByName,
        @JsonProperty("started-by-ext_id") String startedByExtId,
        List<ArchiveDataReferenceReport> archiveDataReferences) {

    static BackfillJobReport from(BackfillJob job) {
        return new BackfillJobReport(
                job.messageName(),
                job.configId(),
                value(job.jobState()),
                value(job.jobResult()),
                job.jobId(),
                job.startedAt(),
                job.reportCreatedAt(),
                job.startedByName(),
                job.startedByExtId(),
                job.tasks().stream()
                        .map(ArchiveDataReferenceReport::from)
                        .toList());
    }

    private static String value(BackfillJobState state) {
        return state == null ? null : state.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String value(BackfillJobResult result) {
        return result == null ? null : result.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String value(BackfillTaskState state) {
        return state == null ? null : state.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ArchiveDataReferenceReport(String id,
                                      Integer version,
                                      String state,
                                      ArchiveDataReferenceError error) {

        static ArchiveDataReferenceReport from(BackfillTask task) {
            ArchiveDataReferenceError error = task.taskState() == BackfillTaskState.FAILED ?
                    new ArchiveDataReferenceError(task.errorMessage(), task.errorTraceId()) : null;
            return new ArchiveDataReferenceReport(
                    task.referenceId(),
                    task.referenceVersion(),
                    value(task.taskState()),
                    error);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ArchiveDataReferenceError(String message,
                                     String traceId) {
    }
}
