package ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
public class ArchiveDataSchema {
    @NonNull
    String system;
    @NonNull
    String name;
    @NonNull
    String referenceIdType;
    @NonNull
    String fileExtension;
    int version;
    @NonNull
    @ToString.Exclude
    byte[] schemaDefinition;
    int expirationDays;
    EncryptionKeyReference encryptionKeyReference;
    EncryptionKeyId encryptionKeyId;
}
