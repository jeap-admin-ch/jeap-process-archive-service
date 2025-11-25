package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RemoteArchiveDataFactory implements ArchiveDataFactory {

    private final RemoteArchiveDataProvider remoteArchiveDataProvider;
    private final RemoteDataDomainEventArchiveConfiguration configuration;

    private final MeterRegistry meterRegistry;

    @Override
    public ArchiveData createArchiveData(DomainEvent domainEvent) {
        final var reference = configuration.getArchiveDataReferenceProvider() != null
                ? configuration.getArchiveDataReferenceProvider().getReference(domainEvent)
                : configuration.getReferenceProvider().getReference(domainEvent.getReferences());

        if (reference == null) {
            return null;
        }

        return meterRegistry.timer("jeap_pas_remote_archive_data_factory", "domainEvent", domainEvent.getType().getName())
                .record(() -> remoteArchiveDataProvider.readArchiveData(
                configuration.getDataReaderEndpoint(),
                configuration.getOauthClientId(),
                reference));
    }
}
