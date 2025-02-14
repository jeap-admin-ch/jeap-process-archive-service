package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import com.github.fge.jackson.JsonLoader;
import com.networknt.schema.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ArchiveTypeDescriptorSchemaValidator {
    private static final String SCHEMA_FILE = "resource:/ArchiveTypeDescriptor.schema.json";
    private final static JsonSchema SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(SchemaLocation.of(SCHEMA_FILE));

    private final File archiveTypeDescriptor;

    static ValidationResult validate(ValidationContext validationContext) {
        ArchiveTypeDescriptorSchemaValidator archiveTypeValidator = new ArchiveTypeDescriptorSchemaValidator(validationContext.getDescriptor());
        return archiveTypeValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        try {
            Set<ValidationMessage> validationMessages = SCHEMA.validate(JsonLoader.fromFile(archiveTypeDescriptor));
            if (!validationMessages.isEmpty()) {
                String msg = String.format("Archive type descriptor file '%s' does not correspond to schema: %s",
                        archiveTypeDescriptor.getAbsolutePath(),
                        validationMessages.stream().map(ValidationMessage::toString).collect(Collectors.joining(", ")));
                return ValidationResult.fail(msg);
            }
            return ValidationResult.ok();
        } catch (IOException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    archiveTypeDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }
    }
}
