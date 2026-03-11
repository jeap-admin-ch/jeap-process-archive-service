package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.command.test.TestCommand;
import ch.admin.bit.jeap.processarchive.command.test.TestCommandPayload;
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

public class TestCommandDataProvider implements MessageArchiveDataProvider<TestCommand> {

    @Override
    public ArchiveData getArchiveData(TestCommand command) {
        return ArchiveData.builder()
                .system("JME")
                .schema("Decree")
                .schemaVersion(1)
                .referenceId(command.getIdentity().getId())
                .payload(createPayload(command.getOptionalPayload().orElseThrow()))
                .contentType("avro/binary")
                .build();
    }

    @SneakyThrows
    private byte[] createPayload(TestCommandPayload payload) {
        SpecificRecord data = Decree.newBuilder()
                .setPayload(payload.getMessage())
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
