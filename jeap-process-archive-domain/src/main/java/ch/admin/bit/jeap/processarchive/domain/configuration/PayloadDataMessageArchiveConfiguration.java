package ch.admin.bit.jeap.processarchive.domain.configuration;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.domain.archive.MessageArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class PayloadDataMessageArchiveConfiguration extends MessageArchiveConfiguration {

    MessageArchiveDataProvider<Message> messageArchiveDataProvider;

    @Builder
    private PayloadDataMessageArchiveConfiguration(@NonNull String messageName,
                                                   @NonNull String topicName,
                                                   String clusterName,
                                                   ArchiveDataCondition<Message> archiveDataCondition,
                                                   @NonNull MessageArchiveDataProvider<Message> messageArchiveDataProvider,
                                                   MessageCorrelationProvider<Message> correlationProvider,
                                                   String featureFlag) {
        super(messageName, topicName, clusterName, archiveDataCondition, correlationProvider, featureFlag);
        this.messageArchiveDataProvider = messageArchiveDataProvider;
    }

    @Override
    public ArchiveDataFactory getArchiveDataFactory() {
        return new MessageArchiveDataFactory(this);
    }
}
