package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BackfillCommandConsumerTest {

    @Test
    void onMessageProcessesCommandAndAcknowledgesRecord() {
        BackfillCommandProcessor processor = mock(BackfillCommandProcessor.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        CreateArtifactCommand command = new CreateArtifactCommand();
        ConsumerRecord<AvroMessageKey, CreateArtifactCommand> record = new ConsumerRecord<>("topic", 0, 0, null, command);

        BackfillCommandConsumer consumer = new BackfillCommandConsumer(processor);

        consumer.onMessage(record, acknowledgment);

        verify(processor).processCommand(command);
        verify(acknowledgment).acknowledge();
    }
}
