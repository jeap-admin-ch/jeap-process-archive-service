package ch.admin.bit.jeap.processarchive.avro;

import ch.admin.bit.jeap.processarchive.avro.repository.ArchiveTypeId;
import ch.admin.bit.jeap.processarchive.avro.repository.ArchiveTypeLoader;
import ch.admin.bit.jeap.processarchive.avro.repository.ArchiveTypeRepository;
import ch.admin.bit.jeap.processarchive.avro.repository.TestArchiveTypeProvider;
import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.SchemaValidationException;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {ArchiveTypeRepository.class, ArchiveTypeLoader.class, ArchiveDataAvroSchemaValidator.class, TestArchiveTypeProvider.class},
        properties = {"spring.cloud.vault.enabled=false"})
class ArchiveDataAvroSchemaValidatorTest {

    private static final int V_1 = 1;
    private static final int V_2 = 2;

    @Autowired
    private ArchiveTypeRepository repository;

    @Autowired
    private ArchiveDataAvroSchemaValidator validator;

    @MockitoBean
    private ArchiveCryptoService archiveCryptoService;

    @Test
    void validatePayloadConformsToSchema() throws IOException {
        ArchiveData archiveData = createArchiveData(payloadAccordingToSchemaV1(), V_1);
        validator.validatePayloadConformsToSchema(archiveData);
    }

    @Test
    void validatePayloadConformsToSchema_shouldThrowExceptionForMissingMandatoryField() throws IOException {
        ArchiveData archiveData = createArchiveData(payloadAccordingToSchemaV1(), V_2);

        assertThrows(SchemaValidationException.class, () ->
                validator.validatePayloadConformsToSchema(archiveData));
    }

    private static ArchiveData createArchiveData(byte[] payload, int schemaVersion) {
        return ArchiveData.builder()
                .contentType("application/avro+binary")
                .system("JME")
                .schema("Decree")
                .schemaVersion(schemaVersion)
                .version(schemaVersion)
                .referenceId("ref-id")
                .payload(payload)
                .metadata(emptyList())
                .build();
    }

    private byte[] payloadAccordingToSchemaV1() throws IOException {
        ArchiveTypeId schemaId = ArchiveTypeId.builder().system("JME").name("Decree").version(1).build();
        Schema schema = repository.requireArchiveType(schemaId).getSchema();
        return serializeAvro(schema);
    }

    private static byte[] serializeAvro(Schema schema) throws IOException {
        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("payload", "payloadValue");
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BinaryEncoder binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            datumWriter.write(user1, binaryEncoder);
            binaryEncoder.flush();
            return outputStream.toByteArray();
        }
    }
}
