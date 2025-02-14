package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.domainevent.DomainEvent;

public interface DomainEventArchiveDataProvider<E extends DomainEvent> {

    /**
     * @return ArchiveData extracted from event payload, or null if no data should be archived for this event
     */
    ArchiveData getArchiveData(E payload);

}
