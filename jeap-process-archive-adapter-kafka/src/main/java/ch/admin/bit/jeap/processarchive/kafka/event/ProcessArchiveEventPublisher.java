package ch.admin.bit.jeap.processarchive.kafka.event;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messagetype.shared.processid.ProcessIdMessageKey;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.processarchive.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class ProcessArchiveEventPublisher {

    private final String topic;
    private final KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    ProcessArchiveEventPublisher(KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate,
                                 ArchivedArtifactEventProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.getEventTopic();
    }

    public void publish(SharedArchivedArtifactVersionCreatedEvent event) {
        try {
            ProcessIdMessageKey key = ProcessIdMessageKey.newBuilder()
                    .setProcessId(event.getProcessId())
                    .build();
            kafkaTemplate.send(topic, key, event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw KafkaException.interrupted(e);
        } catch (ExecutionException e) {
            throw KafkaException.sendFailed(e);
        }
    }
}
