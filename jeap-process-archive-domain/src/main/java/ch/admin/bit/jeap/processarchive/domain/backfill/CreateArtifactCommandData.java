package ch.admin.bit.jeap.processarchive.domain.backfill;

import java.util.UUID;

public record CreateArtifactCommandData(UUID jobId,
                                        String referenceId,
                                        int referenceVersion,
                                        String messageName,
                                        String topicName) {
}
