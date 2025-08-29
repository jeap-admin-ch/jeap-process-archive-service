package ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class ArchiveTypeDescriptor {
    @NonNull
    String system;
    @NonNull
    String archiveType;
    @NonNull
    String referenceIdType;
    @NonNull
    List<ArchiveTypeVersion> versions;
    @NonNull
    Integer expirationDays;

    ArchiveTypeEncryption encryption;
    ArchiveTypeEncryptionKey encryptionKey;
}
