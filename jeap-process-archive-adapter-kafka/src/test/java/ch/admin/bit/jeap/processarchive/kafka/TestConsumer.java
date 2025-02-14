package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messagetype.shared.processid.ProcessIdMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import lombok.Getter;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConsumer {

    @Getter
    private final List<SharedArchivedArtifactVersionCreatedEvent> events = new CopyOnWriteArrayList<>();

    @TestKafkaListener(topics = "archived-artifact-version-created-event-topic")
    public void onEvent(@Payload SharedArchivedArtifactVersionCreatedEvent event,
                        @Header(KafkaHeaders.RECEIVED_KEY) ProcessIdMessageKey key) {
        assertThat(key)
                .isNotNull()
                .matches(actual -> actual.getProcessId().equals(event.getProcessId()));
        this.events.add(event);
    }
}
