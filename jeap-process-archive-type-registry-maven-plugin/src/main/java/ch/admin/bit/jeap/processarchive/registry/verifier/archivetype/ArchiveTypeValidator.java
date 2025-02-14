package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.FileNotChangedValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.AvroSchemaValidator;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ArchiveTypeValidator {
    public static ValidationResult validate(ValidationContext validationContext) {
        JsonNode archiveTypeDescriptorJson;
        try {
            archiveTypeDescriptorJson = JsonLoader.fromFile(validationContext.getDescriptor());
        } catch (IOException e) {
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
