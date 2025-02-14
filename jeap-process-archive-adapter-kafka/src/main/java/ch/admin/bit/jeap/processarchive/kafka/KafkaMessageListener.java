package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality;
import ch.admin.bit.jeap.processarchive.domain.archive.ProcessArchiveException;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventReceiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Set;

import static ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality.PERMANENT;
import static ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation.Temporality.TEMPORARY;

@RequiredArgsConstructor
@Slf4j
class KafkaMessageListener implements AcknowledgingMessageListener<AvroMessageKey, AvroDomainEvent> {
    private final static String ERROR_CODE = "domain-event-archive-failed";

    private final Set<String> eventNames;
    private final DomainEventReceiver domainEventReceiver;

    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, AvroDomainEvent> record, Acknowledgment acknowledgment) {
        try {
            handleDomainEvent(record);
        } catch (KafkaException ex) {
            throw MessageHandlerException.builder()
                    .temporality(Temporality.TEMPORARY)
                    .cause(ex)
                    .message("Kafka exception while handling domain event")
                    .errorCode("kafka-exception")
                    .description(ex.getMessage())
                    .build();
        } catch (ProcessArchiveException ex) {
            throw MessageHandlerException.builder()
                    .temporality(ex.isRetryable() ? Temporality.TEMPORARY : Temporality.PERMANENT)
                    .cause(ex)
                    .message("Exception while handling domain event")
                    .errorCode("process-archive-failed")
                    .description(ex.getMessage())
                    .build();
        }
        acknowledgment.acknowledge();
    }

    private void handleDomainEvent(ConsumerRecord<AvroMessageKey, AvroDomainEvent> record) {
        String receivedEventName = record.value().getType().getName();
        if (eventNames.contains(receivedEventName)) {
            DomainEvent domainEvent = record.value();
            notifyEventReceived(domainEvent);
        } else {
            logIgnoredEvent(record);
        }
    }

    private void notifyEventReceived(DomainEvent domainEvent) {
        try {
            domainEventReceiver.domainEventReceived(domainEvent);
        } catch (ProcessArchiveException ex) {
            throw createMessageHandlerException(ex, ex.isRetryable() ? TEMPORARY : PERMANENT);
        } catch (Exception ex) {
            // Always attempt to retry archiving data, might have failed i.e. due to failed remote call
            throw createMessageHandlerException(ex, TEMPORARY);
        }
    }

    private MessageHandlerException createMessageHandlerException(Exception ex, Temporality temporality) {
        return MessageHandlerException.builder()
                .description("Failed to archive data for domain event")
                .temporality(temporality)
                .message(ex.getMessage())
                .errorCode(ERROR_CODE)
                .cause(ex)
                .build();
    }

    private void logIgnoredEvent(ConsumerRecord<AvroMessageKey, AvroDomainEvent> record) {
        log.debug("Ignoring event {} on topic {} as there is no matching domain event archive configuration",
                record.value().getType().getName(), record.topic());
    }
}
