package ch.admin.bit.jeap.processarchive.domain.archive;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ArchiveDataStorageInfo {

    /**
     * Name of the bucket in which the archive data has been stored
     */
    @NonNull
    String bucket;

    /**
     * The key (prefix + name) under which the archive data has been stored
     */
    @NonNull
    String key;

    /**
     * The name part of the key (without the prefix) under which the archive data has been stored
     */
    @NonNull
    String name;

    /**
     * The version id under which the archive data has been stored.
     */
    @NonNull
    String versionId;

}
