package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.DomainEventArchiveDataProvider;

public class TestDomainEventArchiveDataProvider implements DomainEventArchiveDataProvider<DomainEvent> {
    @Override
    public ArchiveData getArchiveData(DomainEvent event) {
        throw new UnsupportedOperationException();
    }
}
