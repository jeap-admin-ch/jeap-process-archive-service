package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

public interface ArchiveDataFactory {

    ArchiveData createArchiveData(Message message);
}
