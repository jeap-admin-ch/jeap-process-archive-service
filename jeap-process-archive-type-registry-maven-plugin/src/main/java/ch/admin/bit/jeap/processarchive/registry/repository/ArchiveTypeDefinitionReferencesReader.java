package ch.admin.bit.jeap.processarchive.registry.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.networknt.schema.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class ArchiveTypeDefinitionReferencesReader {
    static final String REFERENCES_SCHEMA_FILE = "classpath:/ArchiveTypeDefinitionReferences.schema.json";
    private static final JsonSchema SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
            .getSchema(SchemaLocation.of(REFERENCES_SCHEMA_FILE));

    public static ArchiveTypeDefinitionReferences read(File definitionFile) {
        try {
            JsonNode referencesAsJson = JsonLoader.fromFile(definitionFile);
            Set<ValidationMessage> validationMessages = SCHEMA.validate(referencesAsJson);
            if (!validationMessages.isEmpty()) {
                String errorMessage = validationMessages.stream()
                        .map(ValidationMessage::toString)
                        .collect(Collectors.joining(", "));
                throw RepositoryException.schemaValidationFailed(definitionFile, errorMessage);
            }
            return new ObjectMapper().treeToValue(referencesAsJson, ArchiveTypeDefinitionReferences.class);

        } catch (IOException e) {
            throw RepositoryException.cannotReadFile(definitionFile, e);
        }
    }
}
