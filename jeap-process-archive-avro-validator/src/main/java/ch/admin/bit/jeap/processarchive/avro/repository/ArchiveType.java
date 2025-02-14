package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.avro.Schema;

@Value
@Builder
public class ArchiveType {

    @NonNull
    String system;

    @NonNull
    String name;

    @NonNull
    String referenceIdType;

    int version;

    @NonNull
    Schema schema;

    @NonNull
    Integer expirationDays;

    ArchiveTypeEncryption encryption;

    ArchiveTypeEncryptionKey encryptionKey;
}
