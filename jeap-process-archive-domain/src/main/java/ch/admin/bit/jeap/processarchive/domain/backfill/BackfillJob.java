package ch.admin.bit.jeap.processarchive.domain.backfill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BackfillJob(UUID jobId,
                          String messageName,
                          String configId,
                          BackfillJobState jobState,
                          BackfillJobResult jobResult,
                          Instant startedAt,
                          Instant reportCreatedAt,
                          String startedByName,
                          String startedByExtId,
                          List<BackfillTask> tasks) {

    public BackfillJob {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
