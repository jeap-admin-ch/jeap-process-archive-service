package ch.admin.bit.jeap.processarchive.domain.archive.event;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;

public interface ArchivedArtifactCreatedEventProducer {
    void onArchivedArtifact(ArchivedArtifact archivedArtifact);
}
