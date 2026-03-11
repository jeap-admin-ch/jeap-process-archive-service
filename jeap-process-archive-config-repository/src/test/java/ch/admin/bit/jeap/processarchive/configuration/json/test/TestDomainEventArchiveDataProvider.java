package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;

public class TestDomainEventArchiveDataProvider implements MessageArchiveDataProvider<DomainEvent> {
    @Override
    public ArchiveData getArchiveData(DomainEvent event) {
        throw new UnsupportedOperationException();
    }
}
