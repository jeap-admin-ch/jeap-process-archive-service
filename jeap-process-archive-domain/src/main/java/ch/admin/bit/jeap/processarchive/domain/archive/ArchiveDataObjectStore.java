package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;

public interface ArchiveDataObjectStore {

    ArchiveDataStorageInfo store(ArchiveData archiveData, ArchiveDataSchema schema);

}
