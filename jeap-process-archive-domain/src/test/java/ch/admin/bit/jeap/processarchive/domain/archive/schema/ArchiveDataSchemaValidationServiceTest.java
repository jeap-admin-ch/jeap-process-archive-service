package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveDataSchemaValidationServiceTest {

    private static final String CONTENT_TYPE = "content/type";
    private String invokedValidator;

    @Test
    void initValidators_shouldBeOrderedAccordingToOrderAnnotations() {
        var archiveDataSchemaValidationService = new ArchiveDataSchemaValidationService(
                List.of(new Validator1(), new Validator3(), new Validator2())
        );
        archiveDataSchemaValidationService.initValidators();

        archiveDataSchemaValidationService.validateArchiveDataSchema(createArchiveData());

        assertEquals("Validator3", invokedValidator);
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    private class Validator1 extends AbstractValidator {
    }

    @Order(0)
    private class Validator2 extends AbstractValidator {
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    private class Validator3 extends AbstractValidator {
    }

    private abstract class AbstractValidator implements ArchiveDataSchemaValidator {

        @Override
        public Set<String> getContentTypes() {
            return Set.of(CONTENT_TYPE);
        }

        @Override
        public ArchiveDataSchema validatePayloadConformsToSchema(ArchiveData archiveData) {
            invokedValidator = getClass().getSimpleName();
            return ArchiveDataSchema.builder()
                    .system("sys")
                    .name("at")
                    .referenceIdType("ch.admin.bit.jeap.audit.type.Archive")
                    .fileExtension("avpr")
                    .version(1)
                    .schemaDefinition("test".getBytes(StandardCharsets.UTF_8))
                    .build();
        }
    }

    private static ArchiveData createArchiveData() {
        return ArchiveData.builder()
                .contentType(CONTENT_TYPE)
                .system("test-system")
                .schema("some schema")
                .schemaVersion(1)
                .referenceId(UUID.randomUUID().toString())
                .payload("{ \"data\" = \"test\"}".getBytes(StandardCharsets.UTF_8))
                .metadata(emptyList())
                .build();
    }
}
