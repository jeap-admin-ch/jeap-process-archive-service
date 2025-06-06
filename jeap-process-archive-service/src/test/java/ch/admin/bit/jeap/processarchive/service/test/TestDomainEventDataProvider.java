package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.DomainEventArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.test.decree.v3.Decree;
import ch.admin.bit.jeap.processarchive.test.decree.v3.DecreeReference;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2EventPayload;
import lombok.SneakyThrows;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

public class TestDomainEventDataProvider implements DomainEventArchiveDataProvider<TestDomain2Event> {

    @Override
    public ArchiveData getArchiveData(TestDomain2Event event) {
        return ArchiveData.builder()
                .system("JME")
                .schema("Decree")
                .schemaVersion(1)
                .referenceId(event.getIdentity().getEventId())
                .payload(createPayload(event.getOptionalPayload().orElseThrow()))
                .contentType("avro/binary")
                .build();
    }

    @SneakyThrows
    private byte[] createPayload(TestDomain2EventPayload testDomain2EventPayload) {
        SpecificRecord data = Decree.newBuilder()
                .setCreatedAt(Instant.now())
                .setPayload(testDomain2EventPayload.getData())
                .setTitle("title")
                .setDecreeReference(DecreeReference.newBuilder()
                        .setType("decree-id")
                        .setId("123")
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
