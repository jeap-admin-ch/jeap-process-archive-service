package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeRepository;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ArchiveDataSchemaValidationService {

    /**
     * List of schema validators. Validators are ordered according to precedence noted by {@code @Order} annotations.
     */
    private final List<ArchiveDataSchemaValidator> schemaValidatorBeans;
    private final ArchiveTypeRepository archiveTypeRepository;
    private final Map<String, ArchiveDataSchemaValidator> schemaValidatorsByContentType = new ConcurrentHashMap<>();

    public ArchiveDataSchemaValidationService(List<ArchiveDataSchemaValidator> schemaValidatorBeans,
                                              ArchiveTypeRepository archiveTypeRepository) {
        this.schemaValidatorBeans = schemaValidatorBeans;
        this.archiveTypeRepository = archiveTypeRepository;
    }

    @PostConstruct
    public void initValidators() {
        schemaValidatorBeans.stream()
                // Order is reversed as the last validator inserted into the map with put() wins
                .sorted(AnnotationAwareOrderComparator.INSTANCE.reversed())
                .forEach(this::registerValidator);

        log.info("Registered schema validators: {}", schemaValidatorsByContentType);
    }

    private void registerValidator(ArchiveDataSchemaValidator schemaValidator) {
        for (String contentType : schemaValidator.getContentTypes()) {
            var registeredValidator = schemaValidatorsByContentType.put(contentType.toLowerCase(), schemaValidator);
            if (registeredValidator != null) {
                log.warn("More than one schema validator registered for content type {}: {} and {}",
                        contentType, registeredValidator, schemaValidator);
            }
        }
    }

    public ArchiveDataSchema validateArchiveDataSchema(ArchiveData archiveData) {
        ArchiveTypeInfo typeInfo = archiveTypeRepository.requireType(
                archiveData.getSystem(), archiveData.getSchema(), archiveData.getSchemaVersion());

        SchemaDefinition schemaDef = null;
        String contentType = archiveData.getContentType().toLowerCase();
        ArchiveDataSchemaValidator validator = schemaValidatorsByContentType.get(contentType);
        if (validator != null) {
            schemaDef = validator.validatePayloadConformsToSchema(archiveData);
        } else {
            log.info("No schema validator registered for content type '{}', skipping schema validation", contentType);
        }

        return buildArchiveDataSchema(typeInfo, schemaDef);
    }

    private ArchiveDataSchema buildArchiveDataSchema(ArchiveTypeInfo typeInfo, SchemaDefinition schemaDef) {
        ArchiveDataSchema.ArchiveDataSchemaBuilder builder = ArchiveDataSchema.builder()
                .system(typeInfo.getSystem())
                .name(typeInfo.getName())
                .version(typeInfo.getVersion())
                .referenceIdType(typeInfo.getReferenceIdType())
                .expirationDays(typeInfo.getExpirationDays())
                .encryptionKeyReference(typeInfo.getEncryptionKeyReference())
                .encryptionKeyId(typeInfo.getEncryptionKeyId());
        if (schemaDef != null) {
            builder.schemaDefinition(schemaDef.getDefinition())
                    .fileExtension(schemaDef.getFileExtension());
        }
        return builder.build();
    }
}
