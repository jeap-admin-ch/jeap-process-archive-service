package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.domain.archive.objectsstorage.StorageObjectProperties;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;

import java.util.Optional;

public interface ArchiveDataObjectStore {

    ArchiveDataStorageInfo store(ArchiveData archiveData, ArchiveDataSchema schema);

    Optional<StorageObjectProperties> getObjectProperties(String bucketName, String objectKey);

    Optional<Object> retrieveObject(Class<Object> archiveTypeClass, String objectBucket, String objectKey, String version);
}
