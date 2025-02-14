package ch.admin.bit.jeap.processarchive.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link org.springframework.http.converter.HttpMessageConverter} transparently serializing generated Avro classes
 * to binary avro.
 */
@Slf4j
public class AvroBinaryHttpMessageConverter extends AbstractHttpMessageConverter<SpecificRecord> {

    private final AvroBinarySerializer serializer;
    private final AvroBinaryDeserializer deserializer;

    public AvroBinaryHttpMessageConverter() {
        super(AvroWebConstants.MEDIA_TYPE_AVRO_BINARY);
        serializer = new AvroBinarySerializer();
        deserializer = new AvroBinaryDeserializer();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return SpecificRecord.class.isAssignableFrom(clazz);
    }

    @Override
    protected SpecificRecord readInternal(Class<? extends SpecificRecord> clazz, HttpInputMessage inputMessage) throws IOException {
        InputStream bodyStream = inputMessage.getBody();
        @SuppressWarnings("unchecked")
        SpecificRecord result = deserializer.deserialize((Class<? extends SpecificRecordBase>) clazz, bodyStream);
        return result;
    }

    @Override
    protected void writeInternal(SpecificRecord specificRecord, HttpOutputMessage outputMessage) throws IOException {
        serializer.serialize(specificRecord, outputMessage.getBody());
    }
}