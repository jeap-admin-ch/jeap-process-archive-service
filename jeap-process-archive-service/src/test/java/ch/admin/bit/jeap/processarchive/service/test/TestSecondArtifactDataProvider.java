package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2EventPayload;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.test.DecreeReference;
import ch.admin.bit.jeap.processarchive.test.decree.v2.Decree;
import lombok.SneakyThrows;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;

/**
 * Second archive data provider for {@code TestDomain2Event}, used to verify that multiple archive configurations
 * registered for the same message produce multiple, distinct artifacts. It archives a second Decree artifact using a
 * distinct reference id (suffix {@value #REFERENCE_ID_SUFFIX}) so its idempotence id differs from the first artifact.
 */
public class TestSecondArtifactDataProvider implements MessageArchiveDataProvider<TestDomain2Event> {

    public static final String REFERENCE_ID_SUFFIX = "-second";

    @Override
    public ArchiveData getArchiveData(TestDomain2Event event) {
        return ArchiveData.builder()
                .system("JME")
                .schema("Decree")
                .schemaVersion(1)
                .referenceId(event.getIdentity().getEventId() + REFERENCE_ID_SUFFIX)
                .payload(createPayload(event.getOptionalPayload().orElseThrow()))
                .contentType("avro/binary")
                .build();
    }

    @SneakyThrows
    private byte[] createPayload(TestDomain2EventPayload testDomain2EventPayload) {
        SpecificRecord data = Decree.newBuilder()
                .setPayload(testDomain2EventPayload.getData())
                .setDecreeReference(DecreeReference.newBuilder()
                        .setType("decree-id")
                        .setId("456")
                        .build())
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
        datumWriter.write(data, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }
}
