package ch.admin.bit.jeap.processarchive.domain.configuration;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public abstract class DomainEventArchiveConfiguration {

    private final String eventName;
    private final String topicName;
    private final String clusterName;
    ArchiveDataCondition<Message> archiveDataCondition;
    MessageCorrelationProvider<Message> correlationProvider;

    public abstract ArchiveDataFactory getArchiveDataFactory();

    public boolean acceptsMessage(Message message) {
        return archiveDataCondition == null || archiveDataCondition.isArchiveDataForMessage(message);

    }
}
