package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

import java.util.Set;

public interface ArchiveDataSchemaValidator {

    /**
     * In case of content type clashes, validators are ordered according to precedence noted by
     * {@code @Order} annotations.
     *
     * @return Content types supported by this validator (see {@link ArchiveData#getContentType()})
     */
    Set<String> getContentTypes();

    /**
     * Validates that the archive data payload conforms to the schema and returns the schema definition.
     *
     * @param archiveData Archive data for which the payload is validated if it is well-formed
     * @return The schema definition that the payload was validated against
     * @throws SchemaValidationException If the archive data payload does not conform to the schema specified in
     *                                   {@link ArchiveData#getSchema()}
     */
    SchemaDefinition validatePayloadConformsToSchema(ArchiveData archiveData);

}
