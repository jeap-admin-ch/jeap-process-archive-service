package ch.admin.bit.jeap.test.processarchive;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messagetype.shared.processid.ProcessIdMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TestConsumer {

    @Getter
    private final List<SharedArchivedArtifactVersionCreatedEvent> events = new CopyOnWriteArrayList<>();

    @TestKafkaListener(topics = "event-topic")
    public void onEvent(@Payload SharedArchivedArtifactVersionCreatedEvent event,
                        @Header(KafkaHeaders.RECEIVED_KEY) ProcessIdMessageKey key) {
        log.info("Received {}", event);
        assertThat(key)
                .isNotNull()
                .matches(actual -> actual.getProcessId().equals(event.getProcessId()));
        this.events.add(event);
    }

    public boolean eventWithIdempotenceIdReceived(String eventIdempotenceId) {
        return events.stream().anyMatch(e -> e.getIdentity().getIdempotenceId().equals(eventIdempotenceId));
    }

    public SharedArchivedArtifactVersionCreatedEvent getEventByIdempotenceId(String eventIdempotenceId) {
        return events.stream()
                .filter(e -> e.getIdentity().getIdempotenceId().equals(eventIdempotenceId))
                .findFirst().orElseThrow();
    }
}
