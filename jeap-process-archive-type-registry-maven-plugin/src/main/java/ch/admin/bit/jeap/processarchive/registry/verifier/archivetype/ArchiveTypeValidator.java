package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.FileNotChangedValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.AvroSchemaValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ArchiveTypeValidator {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    public static ValidationResult validate(ValidationContext validationContext) {
        JsonNode archiveTypeDescriptorJson;
        try {
            archiveTypeDescriptorJson = OBJECT_MAPPER.readTree(validationContext.getDescriptor());
        } catch (RuntimeException e) {
            String message = String.format("File '%s' is not a valid JSON-File: %s",
                    validationContext.getDescriptor().getAbsolutePath(), e.getMessage());
            return ValidationResult.fail(message);
        }
        ValidationResult validationResult = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);
        if (!validationResult.isValid()) {
            //If the file format is not valid, do not even try to check other aspects
            return validationResult;
        }
        return ValidationResult.merge(
                AvroSchemaValidator.validate(validationContext, archiveTypeDescriptorJson),
                FileNotChangedValidator.noExistingSchemasChanged(validationContext)
        );
    }
}
