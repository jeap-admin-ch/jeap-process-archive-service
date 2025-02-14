package ch.admin.bit.jeap.processarchive.plugin.api.storage;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@ToString
@Value
@Builder
public class ObjectStorageTarget {
    /**
     * Name of the bucket in which to store the object
     */
    @NonNull
    String bucket;

    /**
     * Prefix to add to the object's basic name for organizing storage in the bucket
     */
    @NonNull
    String prefix;

    /**
     * The object's basic name (without prefix)
     */
    @NonNull
    String name;

    public String getFullObjectName() {
        return prefix + name;
    }

}
