package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DomainEventArchiveDataFactory implements ArchiveDataFactory {

    private final PayloadDataDomainEventArchiveConfiguration configuration;

    @Override
    public ArchiveData createArchiveData(DomainEvent domainEvent) {
        return configuration.getDomainEventArchiveDataProvider().getArchiveData(domainEvent);
    }
}
