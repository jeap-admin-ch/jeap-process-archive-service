package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfoProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClasspathArchiveTypeInfoProvider implements ArchiveTypeInfoProvider {

    private final AvroArchiveTypeRepository avroArchiveTypeRepository;

    @Override
    public List<ArchiveTypeInfo> getArchiveTypes() {
        return avroArchiveTypeRepository.findAll().stream()
                .map(this::toArchiveTypeInfo)
                .toList();
    }

    private ArchiveTypeInfo toArchiveTypeInfo(ArchiveType archiveType) {
        return ArchiveTypeInfo.builder()
                .system(archiveType.getSystem())
                .name(archiveType.getName())
                .version(archiveType.getVersion())
                .referenceIdType(archiveType.getReferenceIdType())
                .expirationDays(archiveType.getExpirationDays())
                .encryptionKeyReference(toEncryptionKeyReference(archiveType.getEncryption()))
                .encryptionKeyId(toEncryptionKeyId(archiveType.getEncryptionKey()))
                .build();
    }

    private EncryptionKeyReference toEncryptionKeyReference(ArchiveTypeEncryption encryption) {
        if (encryption != null) {
            return EncryptionKeyReference.builder()
                    .secretEnginePath(encryption.getSecretEnginePath())
                    .keyName(encryption.getKeyName())
                    .build();
        }
        return null;
    }

    private EncryptionKeyId toEncryptionKeyId(ArchiveTypeEncryptionKey encryptionKey) {
        if (encryptionKey != null) {
            return EncryptionKeyId.builder()
                    .keyId(encryptionKey.getKeyId())
                    .build();
        }
        return null;
    }
}
