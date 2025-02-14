package ch.admin.bit.jeap.processarchive.kafka.event;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import org.springframework.stereotype.Component;

@Component
public class ArchivedArtifactVersionCreatedEventFactory {
    private final String systemName;
    private final String serviceName;

    ArchivedArtifactVersionCreatedEventFactory(ArchivedArtifactEventProperties properties) {
        this.systemName = properties.getSystemName();
        this.serviceName = properties.getServiceName();
    }

    SharedArchivedArtifactVersionCreatedEvent createEvent(ArchivedArtifact archivedArtifact) {
        return ArchivedArtifactVersionCreatedEventBuilder.builder()
                .archivedArtifact(archivedArtifact)
                .referenceIdType(archivedArtifact.getReferenceIdType())
                .sender(systemName, serviceName)
                .build();
    }
}
