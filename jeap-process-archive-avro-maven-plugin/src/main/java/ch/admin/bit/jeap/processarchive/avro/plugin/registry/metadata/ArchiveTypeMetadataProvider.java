package ch.admin.bit.jeap.processarchive.avro.plugin.registry.metadata;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeVersion;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArchiveTypeMetadataProvider {

    public static ArchiveTypeMetadata createMetadata(ArchiveTypeDescriptor typeDescriptor, ArchiveTypeVersion version, String currentBranch, String commitId, String gitUrl) {
        return ArchiveTypeMetadata.builder()
                .systemName(typeDescriptor.getSystem())
                .archiveTypeName(typeDescriptor.getArchiveType())
                .archiveTypeVersion(version.getVersion())
                .expirationDays(typeDescriptor.getExpirationDays())
                .referenceIdType(typeDescriptor.getReferenceIdType())
                .compatibilityMode(version.getCompatibilityMode())
                .compatibleVersion(version.getCompatibleVersion())
                .encryptionKeyId(typeDescriptor.getEncryptionKey() == null ? null : typeDescriptor.getEncryptionKey().getKeyId())
                .encryptionKeyName(typeDescriptor.getEncryption() == null ? null : typeDescriptor.getEncryption().getKeyName())
                .encryptionSecretEnginePath(typeDescriptor.getEncryption() == null ? null : typeDescriptor.getEncryption().getSecretEnginePath())
                .registryBranch(currentBranch)
                .registryCommit(commitId)
                .registryUrl(gitUrl)
                .build();
    }
}
