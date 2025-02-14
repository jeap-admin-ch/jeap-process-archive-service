package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageStrategy;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageTarget;

class CustomObjectStorageStrategy implements ObjectStorageStrategy {

    static final String CUSTOM_BUCKET = "custom-bucket";
    static final String CUSTOM_PREFIX = "custom-prefix/";

    @Override
    public ObjectStorageTarget getObjectStorageTarget(ArchiveData archiveData, ArchiveDataSchema schema) {
        return ObjectStorageTarget.builder()
                .bucket(CUSTOM_BUCKET)
                .prefix(CUSTOM_PREFIX)
                .name(archiveData.getReferenceId() + schema.getReferenceIdType())
                .build();
    }

}
