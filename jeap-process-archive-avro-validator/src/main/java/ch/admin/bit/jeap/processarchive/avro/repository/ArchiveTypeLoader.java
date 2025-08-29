package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.plugin.api.archivetype.ArchiveTypeProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ArchiveTypeLoader {

    private final ArchiveTypeProvider archiveTypeProvider;

    ArchiveTypeLoader(ArchiveTypeProvider archiveTypeProvider) {
        this.archiveTypeProvider = archiveTypeProvider;
    }

    Map<ArchiveTypeId, ArchiveType> loadArchiveTypes() {
        List<Class<? extends SpecificRecordBase>> typeVersions = archiveTypeProvider.getArchiveTypeVersions();
        log.info("Loading archive types: {}", typeVersions.stream().map(Class::getName).toList());

        Map<ArchiveTypeId, ArchiveType> archiveTypes = new HashMap<>();
        typeVersions.forEach(v -> loadArchiveTypeVersion(v, archiveTypes));
        return archiveTypes;
    }

    private void loadArchiveTypeVersion(Class<? extends SpecificRecordBase> archiveTypeClass, Map<ArchiveTypeId, ArchiveType> archiveTypes) {
        Map<String, Object> metadata = getArchiveTypeMetadata(archiveTypeClass);
        ArchiveType archiveType = createArchiveType(metadata);

        ArchiveTypeId archiveTypeId = ArchiveTypeId.builder()
                .system(archiveType.getSystem())
                .name(archiveType.getName())
                .version(archiveType.getVersion())
                .build();

        archiveTypes.put(archiveTypeId, archiveType);
    }

    private ArchiveType createArchiveType(Map<String, Object> metadata) {
        return ArchiveType.builder()
                .name((String) metadata.get("archiveTypeName"))
                .version((Integer) metadata.get("archiveTypeVersion"))
                .system((String) metadata.get("systemName"))
                .referenceIdType((String) metadata.get("referenceIdType"))
                .expirationDays((Integer) metadata.get("expirationDays"))
                .schema((Schema) metadata.get("schema"))
                .encryption(getArchiveTypeEncryptionFromDescriptor(metadata))
                .encryptionKey(getArchiveTypeEncryptionKeyFromDescriptor(metadata))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getArchiveTypeMetadata(Class<? extends SpecificRecordBase> archiveTypeClass) {
        Object archiveTypeMetadata = ReflectionUtils.getField(getField(archiveTypeClass), archiveTypeClass);
        return (Map<String, Object>) archiveTypeMetadata;
    }

    private static Field getField(Class<? extends SpecificRecordBase> archiveTypeClass) {
        try {
            return archiveTypeClass.getField("ARCHIVE_TYPE_METADATA");
        } catch (NoSuchFieldException e) {
            throw ArchiveTypeLoaderException.missingMetadataField(archiveTypeClass, e);
        }
    }

    private ArchiveTypeEncryption getArchiveTypeEncryptionFromDescriptor(Map<String, Object> metadata) {
        if (metadata.containsKey("encryptionSecretEnginePath")) {
            return ArchiveTypeEncryption.builder()
                    .secretEnginePath((String) metadata.get("encryptionSecretEnginePath"))
                    .keyName((String) metadata.get("encryptionKeyName"))
                    .build();
        }
        return null;
    }

    private ArchiveTypeEncryptionKey getArchiveTypeEncryptionKeyFromDescriptor(Map<String, Object> metadata) {
        if (metadata.containsKey("encryptionKeyId")) {
            return ArchiveTypeEncryptionKey.builder()
                    .keyId((String) metadata.get("encryptionKeyId"))
                    .build();
        }
        return null;
    }
}
