package ch.admin.bit.jeap.processarchive.avro;

import ch.admin.bit.jeap.processarchive.avro.repository.ArchiveType;
import ch.admin.bit.jeap.processarchive.avro.repository.ArchiveTypeId;
import ch.admin.bit.jeap.processarchive.avro.repository.AvroArchiveTypeRepository;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidator;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.SchemaDefinition;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.SchemaValidationException;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.ValidatingDecoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Order(value = 0)
@Component
@RequiredArgsConstructor
public class ArchiveDataAvroSchemaValidator implements ArchiveDataSchemaValidator {

    private final AvroArchiveTypeRepository repository;

    @Override
    public Set<String> getContentTypes() {
        // Content type as specified in http://avro.apache.org/docs/current/spec.html#HTTP+as+Transport
        return Set.of("avro/binary");
    }

    /**
     * Validates that a given archive data payload conforms to the avro schema for the archive data type version.
     *
     * @param archiveData Archive data including schema reference and payload
     * @throws SchemaValidationException If the payload does not conform to the avro schema
     * @throws ch.admin.bit.jeap.processarchive.avro.repository.ArchiveTypeLoaderException If no avro schema for the triple system / archive type / version is found
     */
    @Override
    public SchemaDefinition validatePayloadConformsToSchema(ArchiveData archiveData) {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system(archiveData.getSystem())
                .name(archiveData.getSchema())
                .version(archiveData.getSchemaVersion())
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        validateConformsToSchema(archiveData, archiveType.getSchema());

        boolean pretty = true;
        String avroProtocol = archiveType.getSchema().toString(pretty);
        return SchemaDefinition.builder()
                .definition(avroProtocol.getBytes(StandardCharsets.UTF_8))
                .fileExtension("avpr")
                .build();
    }

    private void validateConformsToSchema(ArchiveData archiveData, Schema schema) {
        byte[] payload = archiveData.getPayload();
        DatumReader<Object> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);

        try {
            ValidatingDecoder validatingDecoder = DecoderFactory.get().validatingDecoder(schema, decoder);
            reader.read(null, validatingDecoder);
        } catch (Exception ex) {
            throw SchemaValidationException.schemaValidationFailed(archiveData, ex);
        }
    }
}
