package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ArchiveTypeDescriptorSchemaValidator {
    private static final String SCHEMA_FILE = "resource:/ArchiveTypeDescriptor.schema.json";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final Schema SCHEMA = SchemaRegistry
            .withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of(SCHEMA_FILE));

    private final File archiveTypeDescriptor;

    static ValidationResult validate(ValidationContext validationContext) {
        ArchiveTypeDescriptorSchemaValidator archiveTypeValidator = new ArchiveTypeDescriptorSchemaValidator(validationContext.getDescriptor());
        return archiveTypeValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        try {
            JsonNode descriptor = OBJECT_MAPPER.readTree(archiveTypeDescriptor);
            List<Error> validationErrors = SCHEMA.validate(descriptor);
            if (!validationErrors.isEmpty()) {
                String msg = String.format("Archive type descriptor file '%s' does not correspond to schema: %s",
                        archiveTypeDescriptor.getAbsolutePath(),
                        validationErrors.stream().map(Error::toString).collect(Collectors.joining(", ")));
                return ValidationResult.fail(msg);
            }
            return ValidationResult.ok();
        } catch (RuntimeException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    archiveTypeDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }
    }
}
