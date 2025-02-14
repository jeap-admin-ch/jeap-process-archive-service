package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveDataEncryption;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveTypeRepository {

    private final ArchiveTypeLoader archiveTypeLoader;

    private final Map<ArchiveTypeId, ArchiveType> archiveTypes = new ConcurrentHashMap<>();

    private final ArchiveCryptoService archiveCryptoService;

    @PostConstruct
    void initialize() throws IOException {
        archiveTypes.putAll(archiveTypeLoader.loadArchiveTypes());
        log.info("Loaded {} archive types: {}", archiveTypes.size(), archiveTypes.keySet());
        Set<ArchiveTypeEncryption> encryptions = archiveTypes.values().stream()
                .map(ArchiveType::getEncryption)
                .filter(Objects::nonNull)
                .collect(toSet());
        validateEncryptionConfiguration(encryptions);
        Set<ArchiveTypeEncryptionKey> encryptionKeys = archiveTypes.values().stream()
                .map(ArchiveType::getEncryptionKey)
                .filter(Objects::nonNull)
                .collect(toSet());
        validateEncryptionKeyConfiguration(encryptionKeys);
    }

    private void validateEncryptionConfiguration(Set<ArchiveTypeEncryption> encryptionSet) {
        final byte[] payloadToEncrypt = "dummy".getBytes(StandardCharsets.UTF_8);
        for (ArchiveTypeEncryption encryptionConfig : encryptionSet) {
            log.info("Validate encryption configuration {}", encryptionConfig);
            EncryptionKeyReference keyRef = EncryptionKeyReference.builder()
                    .secretEnginePath(encryptionConfig.getSecretEnginePath())
                    .keyName(encryptionConfig.getKeyName())
                    .build();
            ArchiveDataEncryption encryption = ArchiveDataEncryption.builder()
                    .encryptionKeyReference(keyRef)
                    .build();
            archiveCryptoService.encrypt(payloadToEncrypt, encryption);
        }
    }

    private void validateEncryptionKeyConfiguration(Set<ArchiveTypeEncryptionKey> encryptionKeySet) {
        final byte[] payloadToEncrypt = "dummy".getBytes(StandardCharsets.UTF_8);
        for (ArchiveTypeEncryptionKey encryptionConfig : encryptionKeySet) {
            log.info("Validate encryption key configuration {}", encryptionConfig);
            EncryptionKeyId keyId = EncryptionKeyId.builder()
                    .keyId(encryptionConfig.getKeyId())
                    .build();
            ArchiveDataEncryption encryption = ArchiveDataEncryption.builder()
                    .encryptionKeyId(keyId)
                    .build();
            archiveCryptoService.encrypt(payloadToEncrypt, encryption);
        }
    }

    /**
     * @param archiveTypeId Coordinates for archive type (system, name, version)
     * @throws ArchiveTypeLoaderException If no archive type definition is available for the given archive type
     */
    public ArchiveType requireArchiveType(ArchiveTypeId archiveTypeId) {
        return archiveTypes.computeIfAbsent(archiveTypeId, this::archiveTypeNotFound);
    }

    private ArchiveType archiveTypeNotFound(ArchiveTypeId schemaId) {
        throw ArchiveTypeLoaderException.archiveTypeNotFound(schemaId);
    }

    List<ArchiveType> findAll() {
        List<ArchiveType> all = new ArrayList<>(archiveTypes.values());
        all.sort(Comparator.comparing(ArchiveType::getName));
        return all;
    }

}
