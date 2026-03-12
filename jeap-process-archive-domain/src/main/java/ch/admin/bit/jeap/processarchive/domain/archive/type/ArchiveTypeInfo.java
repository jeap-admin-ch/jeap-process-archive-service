package ch.admin.bit.jeap.processarchive.domain.archive.type;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ArchiveTypeInfo {
    @NonNull
    String system;
    @NonNull
    String name;
    int version;
    @NonNull
    String referenceIdType;
    int expirationDays;
    EncryptionKeyReference encryptionKeyReference;
    EncryptionKeyId encryptionKeyId;
}
