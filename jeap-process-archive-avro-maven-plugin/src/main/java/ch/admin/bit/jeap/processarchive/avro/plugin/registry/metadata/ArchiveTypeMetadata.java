package ch.admin.bit.jeap.processarchive.avro.plugin.registry.metadata;

import lombok.Builder;
import lombok.Value;
import org.apache.avro.Schema;

@Value
@Builder
public class ArchiveTypeMetadata {
    String archiveTypeName;
    int archiveTypeVersion;
    String systemName;
    String referenceIdType;
    int expirationDays;

    String registryUrl;
    String registryBranch;
    String registryCommit;

    String compatibilityMode;
    Integer compatibleVersion;

    String encryptionSecretEnginePath;
    String encryptionKeyName;
    String encryptionKeyId;

    @SuppressWarnings("unused") // referenced in velocity template
    public boolean shouldGenerateFor(Schema schema) {
        return archiveTypeName.equals(schema.getName());
    }
}
