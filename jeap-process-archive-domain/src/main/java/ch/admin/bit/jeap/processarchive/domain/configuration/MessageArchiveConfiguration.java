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
public abstract class MessageArchiveConfiguration {

    /**
     * Optional identifier of this configuration, unique per message name. Mandatory when a message has
     * multiple configurations; used to disambiguate configurations (e.g. for backfill jobs).
     */
    private final String id;
    private final String messageName;
    private final String topicName;
    private final String clusterName;
    ArchiveDataCondition<Message> archiveDataCondition;
    MessageCorrelationProvider<Message> correlationProvider;
    private final String featureFlag;

    public abstract ArchiveDataFactory getArchiveDataFactory();

    public boolean acceptsMessage(Message message) {
        return archiveDataCondition == null || archiveDataCondition.isArchiveDataForMessage(message);

    }
}
