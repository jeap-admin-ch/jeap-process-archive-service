package ch.admin.bit.jeap.processarchive.domain.backfill;

import java.util.List;
import java.util.UUID;

public record BackfillJobSubmission(UUID jobId,
                                    String messageName,
                                    String configId,
                                    List<BackfillArchiveDataReference> archiveDataReferences,
                                    BackfillJobSubmitter submitter) {

    public BackfillJobSubmission {
        archiveDataReferences = archiveDataReferences == null ? null : List.copyOf(archiveDataReferences);
    }
}
