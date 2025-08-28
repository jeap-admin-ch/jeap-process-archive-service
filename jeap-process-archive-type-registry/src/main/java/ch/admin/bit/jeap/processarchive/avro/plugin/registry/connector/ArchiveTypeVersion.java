package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import lombok.NonNull;
import lombok.Value;

@Value
public class ArchiveTypeVersion {
    @NonNull
    Integer version;
    @NonNull
    String schema;
    String compatibilityMode;
    Integer compatibleVersion;
}
