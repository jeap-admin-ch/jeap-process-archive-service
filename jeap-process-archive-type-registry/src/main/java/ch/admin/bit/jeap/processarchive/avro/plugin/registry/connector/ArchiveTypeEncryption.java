package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import lombok.NonNull;
import lombok.Value;

@Value
public class ArchiveTypeEncryption {
    @NonNull
    String secretEnginePath;
    @NonNull
    String keyName;
}
