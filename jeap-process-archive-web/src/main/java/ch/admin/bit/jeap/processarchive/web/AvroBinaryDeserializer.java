package ch.admin.bit.jeap.processarchive.web;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;

import java.io.IOException;
import java.io.InputStream;

public class AvroBinaryDeserializer {

    static {
        // Since Avro 1.12.2 every class resolved from a schema has to be trusted explicitly. Install the default
        // whitelist unless the application already installed one: this module is used by consumers of the archive
        // REST API that are not jEAP messaging services and therefore have neither the jeap-messaging Spring
        // auto-configuration nor its JUnit listener installing the whitelist for them.
        AvroClassSecurity.installDefaultIfMissing();
    }

    public <T extends SpecificRecord> T deserialize(Class<T> clazz, InputStream inputStream) throws IOException {
        DatumReader<T> datumReader = new SpecificDatumReader<>(clazz);
        Decoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        return datumReader.read(null, decoder);
    }
}