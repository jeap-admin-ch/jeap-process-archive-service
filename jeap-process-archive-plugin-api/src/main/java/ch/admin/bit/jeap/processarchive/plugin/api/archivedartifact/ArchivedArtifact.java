package ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ArchivedArtifact {
    @NonNull
    ArchiveData archiveData;
    @NonNull
    String processId;
    @NonNull
    String idempotenceId;
    @NonNull
    String referenceIdType;
    @NonNull
    String storageObjectBucket;
    /**
     * Full key of the object inside the bucket, including the prefix
     */
    @NonNull
    String storageObjectKey;
    /**
     * Name of the object inside the bucket, excluding the prefix
     */
    @NonNull
    String storageObjectId;
    @NonNull
    String storageObjectVersionId;
    int expirationDays;
}
