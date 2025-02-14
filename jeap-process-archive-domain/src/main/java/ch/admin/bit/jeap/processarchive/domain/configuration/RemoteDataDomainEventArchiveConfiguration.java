package ch.admin.bit.jeap.processarchive.domain.configuration;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataFactory;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ReferenceProvider;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoteDataDomainEventArchiveConfiguration extends DomainEventArchiveConfiguration {

    ReferenceProvider<MessageReferences> referenceProvider;
    String dataReaderEndpoint;
    String oauthClientId;
    RemoteArchiveDataProvider remoteArchiveDataProvider;
    MeterRegistry meterRegistry;

    @Builder
    @SuppressWarnings("java:S107")
    private RemoteDataDomainEventArchiveConfiguration(@NonNull RemoteArchiveDataProvider remoteArchiveDataProvider,
                                                      ArchiveDataCondition<Message> archiveDataCondition,
                                                      @NonNull String eventName,
                                                      @NonNull String topicName,
                                                      String clusterName,
                                                      @NonNull ReferenceProvider<MessageReferences> referenceProvider,
                                                      @NonNull String dataReaderEndpoint,
                                                      String oauthClientId,
                                                      @NonNull MeterRegistry meterRegistry,
                                                      MessageCorrelationProvider<Message> correlationProvider) {
        super(eventName, topicName, clusterName, archiveDataCondition, correlationProvider);
        this.remoteArchiveDataProvider = remoteArchiveDataProvider;
        this.archiveDataCondition = archiveDataCondition;
        this.referenceProvider = referenceProvider;
        this.dataReaderEndpoint = dataReaderEndpoint;
        this.oauthClientId = oauthClientId;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ArchiveDataFactory getArchiveDataFactory() {
        return new RemoteArchiveDataFactory(remoteArchiveDataProvider, this, meterRegistry);
    }
}
