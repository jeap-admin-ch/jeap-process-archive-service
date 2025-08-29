package ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor;

import lombok.NonNull;
import lombok.Value;

@Value
public class ArchiveTypeEncryptionKey {
    @NonNull
    String keyId;
}
