package ch.admin.bit.jeap.processarchive.web;

import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

@UtilityClass
public class AvroWebConstants {

    /**
     * See <a href="http://avro.apache.org/docs/current/spec.html#HTTP+as+Transport">
     * The HTTP Content-Type of requests and responses should be specified as "avro/binary"
     * </a>
     */
    public static final String AVRO_BINARY = "avro/binary";

    public static final MediaType MEDIA_TYPE_AVRO_BINARY = MediaType.parseMediaType(AVRO_BINARY);
}
