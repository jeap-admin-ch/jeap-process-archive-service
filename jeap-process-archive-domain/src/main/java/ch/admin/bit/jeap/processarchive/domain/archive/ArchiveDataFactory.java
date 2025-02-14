package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

public interface ArchiveDataFactory {

    ArchiveData createArchiveData(DomainEvent event);
}
