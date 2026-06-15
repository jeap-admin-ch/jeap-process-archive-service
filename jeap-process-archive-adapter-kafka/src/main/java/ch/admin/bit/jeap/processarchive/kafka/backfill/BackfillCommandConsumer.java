package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "jeap.processarchive.backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class BackfillCommandConsumer implements AcknowledgingMessageListener<AvroMessageKey, CreateArtifactCommand> {

    private final BackfillCommandProcessor processor;

    @KafkaListener(topics = "${jeap.processarchive.backfill.topic}")
    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, CreateArtifactCommand> record, Acknowledgment acknowledgment) {
        CreateArtifactCommand command = record.value();
        processor.processCommand(command);
        acknowledgment.acknowledge();
    }
}
