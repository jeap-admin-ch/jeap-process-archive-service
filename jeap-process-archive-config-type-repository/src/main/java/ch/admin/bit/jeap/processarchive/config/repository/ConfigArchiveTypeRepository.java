package ch.admin.bit.jeap.processarchive.config.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfoProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class ConfigArchiveTypeRepository implements ArchiveTypeInfoProvider {

    private final ProcessArchiveRegistryProperties properties;
    private final ArchiveCryptoService archiveCryptoService;
    private final Map<ConfigArchiveTypeId, ArchiveTypeDefinition> archiveTypes = new ConcurrentHashMap<>();

    @PostConstruct
    void initialize() {
        for (ArchiveTypeDefinition type : properties.getTypes()) {
            ConfigArchiveTypeId id = new ConfigArchiveTypeId(type.getSystem(), type.getArchiveType(), type.getVersion());
            archiveTypes.put(id, type);
        }
        log.info("Loaded {} archive types from configuration: {}", archiveTypes.size(), archiveTypes.keySet());
        validateEncryptionConfiguration();
    }

    private void validateEncryptionConfiguration() {
        final byte[] payloadToEncrypt = "dummy".getBytes(StandardCharsets.UTF_8);
        archiveTypes.values().stream()
                .map(ArchiveTypeDefinition::getEncryptionKey)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(keyId -> validateEncryptionKeyIsAccessible(keyId, payloadToEncrypt));
    }

    // Validates accessibility of the key by attempting a dummy encryption - an exception is thrown if the key is not accessible.
    private void validateEncryptionKeyIsAccessible(String keyId, byte[] payloadToEncrypt) {
        log.info("Validate encryption key configuration for key '{}'", keyId);
        ArchiveDataEncryption encryption = ArchiveDataEncryption.builder()
                .encryptionKeyId(EncryptionKeyId.builder().keyId(keyId).build())
                .build();
        archiveCryptoService.encrypt(payloadToEncrypt, encryption);
    }

    @Override
    public List<ArchiveTypeInfo> getArchiveTypes() {
        return archiveTypes.values().stream()
                .map(this::toArchiveTypeInfo)
                .toList();
    }

    private ArchiveTypeInfo toArchiveTypeInfo(ArchiveTypeDefinition type) {
        ArchiveTypeInfo.ArchiveTypeInfoBuilder builder = ArchiveTypeInfo.builder()
                .system(type.getSystem())
                .name(type.getArchiveType())
                .version(type.getVersion())
                .referenceIdType(type.getReferenceIdType())
                .expirationDays(type.getExpirationDays());
        if (type.getEncryptionKey() != null) {
            builder.encryptionKeyId(EncryptionKeyId.builder().keyId(type.getEncryptionKey()).build());
        }
        return builder.build();
    }

    record ConfigArchiveTypeId(String system, String name, int version) {
    }
}
