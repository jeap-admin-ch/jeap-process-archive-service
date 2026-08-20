package ch.admin.bit.jeap.processarchive.web;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;

import java.io.IOException;
import java.io.InputStream;

public class AvroBinaryDeserializer {

    public <T extends SpecificRecord> T deserialize(Class<T> clazz, InputStream inputStream) throws IOException {
        DatumReader<T> datumReader = new SpecificDatumReader<>(clazz);
        Decoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
        return datumReader.read(null, decoder);
    }
}