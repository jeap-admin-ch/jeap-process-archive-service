package ch.admin.bit.jeap.processarchive.avro;

import ch.admin.bit.jeap.processarchive.avro.repository.*;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidator;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.SchemaValidationException;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
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

    private final ArchiveTypeRepository repository;

    @Override
    public Set<String> getContentTypes() {
        // Content type as specified in http://avro.apache.org/docs/current/spec.html#HTTP+as+Transport
        return Set.of("avro/binary");
    }

    /**
     * Validates that a given archive data payload conforms to the avro schema for the archive data type version.
     *
     * @param archiveData Archive data including schema reference and payload
     * @throws SchemaValidationException  If the payload does not conform to the avro schema
     * @throws ArchiveTypeLoaderException If no avro schema for the triple system / archive type / version is found
     */
    @Override
    public ArchiveDataSchema validatePayloadConformsToSchema(ArchiveData archiveData) {
        ArchiveTypeId schemaId = ArchiveTypeId.builder()
                .system(archiveData.getSystem())
                .name(archiveData.getSchema())
                .version(archiveData.getSchemaVersion())
                .build();

        ArchiveType archiveType = repository.requireArchiveType(schemaId);

        validateConformsToSchema(archiveData, archiveType.getSchema());

        return createArchiveDataSchema(archiveData, archiveType);
    }

    private ArchiveDataSchema createArchiveDataSchema(ArchiveData archiveData, ArchiveType archiveType) {
        boolean pretty = true;
        String avroProtocol = archiveType.getSchema().toString(pretty);
        return ArchiveDataSchema.builder()
                .system(archiveData.getSystem())
                .name(archiveData.getSchema())
                .referenceIdType(archiveType.getReferenceIdType())
                .version(archiveData.getSchemaVersion())
                .fileExtension("avpr")
                .schemaDefinition(avroProtocol.getBytes(StandardCharsets.UTF_8))
                .expirationDays(archiveType.getExpirationDays())
                .encryptionKeyReference(getEncryptionKeyReferenceFromArchiveType(archiveType.getEncryption()))
                .encryptionKeyId(getEncryptionKeyIdReferenceFromArchiveType(archiveType.getEncryptionKey()))
                .build();
    }

    private EncryptionKeyReference getEncryptionKeyReferenceFromArchiveType(ArchiveTypeEncryption encryption) {
        if (encryption != null) {
            return EncryptionKeyReference.builder()
                    .keyName(encryption.getKeyName())
                    .secretEnginePath(encryption.getSecretEnginePath())
                    .build();
        }
        return null;
    }

    private EncryptionKeyId getEncryptionKeyIdReferenceFromArchiveType(ArchiveTypeEncryptionKey encryption) {
        if (encryption != null) {
            return EncryptionKeyId.builder()
                    .keyId(encryption.getKeyId())
                    .build();
        }
        return null;
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
