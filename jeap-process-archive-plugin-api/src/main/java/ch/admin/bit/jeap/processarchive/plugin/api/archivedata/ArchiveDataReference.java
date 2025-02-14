package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ArchiveDataReference {
    /**
     * Id of the archive data
     */
    @NonNull
    String id;

    /**
     * Version of the archive data, if applicable
     */
    Integer version;

}
