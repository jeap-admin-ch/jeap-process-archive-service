package ch.admin.bit.jeap.processarchive.web;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractSingleValueEncoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class AvroBinaryEncoder extends AbstractSingleValueEncoder<SpecificRecord> {

    private final AvroBinarySerializer avroBinarySerializer = new AvroBinarySerializer();

    AvroBinaryEncoder() {
        super(AvroWebConstants.MEDIA_TYPE_AVRO_BINARY);
    }

    @Override
    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return super.canEncode(elementType, mimeType) &&
                SpecificRecord.class.isAssignableFrom(elementType.toClass());
    }

    @Override
    protected Flux<DataBuffer> encode(SpecificRecord specificRecord,
                                      DataBufferFactory bufferFactory,
                                      ResolvableType type, MimeType mimeType, Map<String, Object> hints) {
        // doOnDiscard in base class will release the buffer
        return Mono.fromCallable(() -> encodeValue(specificRecord, bufferFactory)).
                flux();
    }

    private DataBuffer encodeValue(SpecificRecord specificRecord, DataBufferFactory bufferFactory) {
        try {
            return serializeToDataBuffer(specificRecord, bufferFactory);
        } catch (Exception ex) {
            throw new EncodingException("Could not serialize " + specificRecord.getClass() + " to avro", ex);
        }
    }

    private DataBuffer serializeToDataBuffer(SpecificRecord specificRecord, DataBufferFactory bufferFactory) throws IOException {
        DataBuffer buffer = bufferFactory.allocateBuffer(1024);
        try {
            OutputStream outputStream = buffer.asOutputStream();
            avroBinarySerializer.serialize(specificRecord, outputStream);
            return buffer;
        } catch (Throwable t) {
            DataBufferUtils.release(buffer);
            throw t;
        }
    }
}
