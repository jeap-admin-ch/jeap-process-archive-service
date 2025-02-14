package ch.admin.bit.jeap.processarchive.crypto;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArchiveDataEncryption {
    EncryptionKeyReference encryptionKeyReference;
    EncryptionKeyId encryptionKeyId;

    public static ArchiveDataEncryption from(ArchiveDataSchema schema) {
        if (schema.getEncryptionKeyReference() != null) {
            return ArchiveDataEncryption.builder()
                    .encryptionKeyReference(schema.getEncryptionKeyReference())
                    .build();
        } else if (schema.getEncryptionKeyId() != null) {
            return ArchiveDataEncryption.builder()
                    .encryptionKeyId(schema.getEncryptionKeyId())
                    .build();
        }
        return null;
    }
}
