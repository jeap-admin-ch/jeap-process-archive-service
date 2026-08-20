package ch.admin.bit.jeap.processarchive.web;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.IOException;
import java.io.OutputStream;

public class AvroBinarySerializer {

    static {
        // Since Avro 1.12.2 every class resolved from a schema has to be trusted explicitly. Install the default
        // whitelist unless the application already installed one: this module is used by consumers of the archive
        // REST API that are not jEAP messaging services and therefore have neither the jeap-messaging Spring
        // auto-configuration nor its JUnit listener installing the whitelist for them.
        AvroClassSecurity.installDefaultIfMissing();
    }

    public void serialize(SpecificRecord data, OutputStream outputStream) throws IOException {
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
        datumWriter.write(data, encoder);
        encoder.flush();
    }
}