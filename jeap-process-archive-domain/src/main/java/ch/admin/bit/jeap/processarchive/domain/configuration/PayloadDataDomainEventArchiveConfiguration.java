package ch.admin.bit.jeap.processarchive.domain.configuration;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.domain.archive.DomainEventArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.DomainEventArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class PayloadDataDomainEventArchiveConfiguration extends DomainEventArchiveConfiguration {

    DomainEventArchiveDataProvider<DomainEvent> domainEventArchiveDataProvider;

    @Builder
    private PayloadDataDomainEventArchiveConfiguration(@NonNull String eventName,
                                                       @NonNull String topicName,
                                                       String clusterName,
                                                       ArchiveDataCondition<Message> archiveDataCondition,
                                                       @NonNull DomainEventArchiveDataProvider<DomainEvent> domainEventArchiveDataProvider,
                                                       MessageCorrelationProvider<Message> correlationProvider,
                                                       String featureFlag) {
        super(eventName, topicName, clusterName, archiveDataCondition, correlationProvider, featureFlag);
        this.domainEventArchiveDataProvider = domainEventArchiveDataProvider;
    }

    @Override
    public ArchiveDataFactory getArchiveDataFactory() {
        return new DomainEventArchiveDataFactory(this);
    }
}
