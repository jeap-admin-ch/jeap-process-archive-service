package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.domainevent.DomainEvent;

import java.util.function.Supplier;

class DomainEventReceiverException extends RuntimeException {

    private DomainEventReceiverException(String message) {
        super(message);
    }

    static Supplier<DomainEventReceiverException> unexpectedEvent(DomainEvent event) {
        return () -> new DomainEventReceiverException(
                "Received unexpected event (missing archive configuration for event): " + event.getType().getName());
    }
}
