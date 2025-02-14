package ch.admin.bit.jeap.processarchive.plugin.api.storage;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;

public interface ObjectStorageStrategy {

    ObjectStorageTarget getObjectStorageTarget(ArchiveData archiveData, ArchiveDataSchema schema);

}
