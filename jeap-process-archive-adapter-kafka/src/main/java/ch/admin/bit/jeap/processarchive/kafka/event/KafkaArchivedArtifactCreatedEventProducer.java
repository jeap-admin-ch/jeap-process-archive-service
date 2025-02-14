package ch.admin.bit.jeap.processarchive.kafka.event;

import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@RequiredArgsConstructor
@Slf4j
public class KafkaArchivedArtifactCreatedEventProducer implements ArchivedArtifactCreatedEventProducer {
    private final ProcessArchiveEventPublisher eventPublisher;
    private final ArchivedArtifactVersionCreatedEventFactory eventFactory;

    @Override
    public void onArchivedArtifact(ArchivedArtifact archivedArtifact) {
        log.debug("Publishing event for archived artifact with reference ID {} and version {}.",
                keyValue("referenceId", archivedArtifact.getArchiveData().getReferenceId()),
                keyValue("version", archivedArtifact.getArchiveData().getVersion()));

        eventPublisher.publish(
                eventFactory.createEvent(archivedArtifact));
    }
}
