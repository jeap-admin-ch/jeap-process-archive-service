package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;

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
     * @param archiveData Archive data for which the payload is validated if it is well-formed
     * @throws SchemaValidationException If the archive data payload does not conform to the schema specified in
     *                                   {@link ArchiveData#getSchema()}
     */
    ArchiveDataSchema validatePayloadConformsToSchema(ArchiveData archiveData);

}
