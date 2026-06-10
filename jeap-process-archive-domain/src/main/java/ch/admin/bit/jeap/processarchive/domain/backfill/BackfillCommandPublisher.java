package ch.admin.bit.jeap.processarchive.domain.backfill;

public interface BackfillCommandPublisher {

    void publish(CreateArtifactCommandData command);
}
