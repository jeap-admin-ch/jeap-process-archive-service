package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveDataSchemaValidationService {

    /**
     * List of schema validators. Validators are ordered according to precedence noted by {@code @Order} annotations.
     */
    private final List<ArchiveDataSchemaValidator> schemaValidatorBeans;
    private final Map<String, ArchiveDataSchemaValidator> schemaValidatorsByContentType = new ConcurrentHashMap<>();

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
        String contentType = archiveData.getContentType().toLowerCase();
        ArchiveDataSchemaValidator archiveDataSchemaValidator = schemaValidatorsByContentType.get(contentType);
        if (archiveDataSchemaValidator != null) {
            return archiveDataSchemaValidator.validatePayloadConformsToSchema(archiveData);
        } else {
            throw SchemaValidationException.noValidatorForContentType(archiveData, contentType);
        }
    }
}
