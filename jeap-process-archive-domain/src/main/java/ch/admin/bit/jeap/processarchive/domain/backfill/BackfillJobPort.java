package ch.admin.bit.jeap.processarchive.domain.backfill;

import java.util.Optional;
import java.util.UUID;

public interface BackfillJobPort {

    void saveJob(BackfillJob job);

    Optional<BackfillJob> findById(UUID jobId);

    boolean existsById(UUID jobId);
}
