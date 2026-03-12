package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeNotFoundException;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchiveDataSchemaValidationServiceTest {

    private static final String CONTENT_TYPE = "content/type";
    private String invokedValidator;

    private static final ArchiveTypeInfo TYPE_INFO = ArchiveTypeInfo.builder()
            .system("test-system")
            .name("some schema")
            .version(1)
            .referenceIdType("ch.admin.bit.jeap.audit.type.Archive")
            .expirationDays(90)
            .build();

    private ArchiveTypeRepository mockRepository() {
        ArchiveTypeRepository repo = mock(ArchiveTypeRepository.class);
        when(repo.requireType("test-system", "some schema", 1)).thenReturn(TYPE_INFO);
        return repo;
    }

    @Test
    void initValidators_shouldBeOrderedAccordingToOrderAnnotations() {
        var service = new ArchiveDataSchemaValidationService(
                List.of(new Validator1(), new Validator3(), new Validator2()),
                mockRepository()
        );
        service.initValidators();

        ArchiveDataSchema result = service.validateArchiveDataSchema(createArchiveData());

        assertEquals("Validator3", invokedValidator);
        assertEquals("test-system", result.getSystem());
        assertEquals("some schema", result.getName());
        assertEquals(90, result.getExpirationDays());
        assertNotNull(result.getSchemaDefinition());
    }

    @Test
    void validateArchiveDataSchema_noValidator_typeExists_shouldReturnSchemaWithoutDefinition() {
        var service = new ArchiveDataSchemaValidationService(List.of(), mockRepository());
        service.initValidators();

        ArchiveDataSchema result = service.validateArchiveDataSchema(createArchiveData());

        assertEquals("test-system", result.getSystem());
        assertEquals("some schema", result.getName());
        assertEquals("ch.admin.bit.jeap.audit.type.Archive", result.getReferenceIdType());
        assertEquals(1, result.getVersion());
        assertEquals(90, result.getExpirationDays());
        assertNull(result.getSchemaDefinition());
        assertNull(result.getFileExtension());
    }

    @Test
    void validateArchiveDataSchema_unknownType_shouldThrow() {
        ArchiveTypeRepository repo = mock(ArchiveTypeRepository.class);
        when(repo.requireType("test-system", "some schema", 1))
                .thenThrow(ArchiveTypeNotFoundException.forType("test-system", "some schema", 1));

        var service = new ArchiveDataSchemaValidationService(List.of(), repo);
        service.initValidators();
        ArchiveData archiveData = createArchiveData();

        assertThrows(ArchiveTypeNotFoundException.class, () ->
                service.validateArchiveDataSchema(archiveData));
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
        public SchemaDefinition validatePayloadConformsToSchema(ArchiveData archiveData) {
            invokedValidator = getClass().getSimpleName();
            return SchemaDefinition.builder()
                    .definition("test".getBytes(StandardCharsets.UTF_8))
                    .fileExtension("avpr")
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
