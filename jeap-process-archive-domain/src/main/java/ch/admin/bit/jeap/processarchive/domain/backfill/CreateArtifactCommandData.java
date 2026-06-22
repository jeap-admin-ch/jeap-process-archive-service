package ch.admin.bit.jeap.processarchive.domain.backfill;

import java.util.UUID;

public record CreateArtifactCommandData(UUID jobId,
                                        String referenceId,
                                        Integer referenceVersion,
                                        String messageName,
                                        String topicName) {

    public CreateArtifactCommandData {
        if (referenceVersion != null && referenceVersion < 1) {
            throw BackfillJobException.invalidRequest("referenceVersion must be positive when provided");
        }
    }
}
