package ch.admin.bit.jeap.processarchive.domain.archive.event;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link ArtifactArchivedListener} producing a SharedArchivedArtifactVersionCreatedEvent
 */
@Component
@RequiredArgsConstructor
public class EventProducingArtifactArchivedListener implements ArtifactArchivedListener {

    private final ArchivedArtifactCreatedEventProducer eventProducer;

    @Override
    public void onArtifactArchived(ArchivedArtifact archivedArtifact) {
        eventProducer.onArchivedArtifact(archivedArtifact);
    }
}
