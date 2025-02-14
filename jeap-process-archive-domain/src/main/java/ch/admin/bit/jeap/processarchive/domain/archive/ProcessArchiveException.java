package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import lombok.Getter;

import java.util.function.Supplier;

import static java.lang.String.format;

@Getter
public final class ProcessArchiveException extends RuntimeException {

    private final boolean retryable;

    private ProcessArchiveException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    private ProcessArchiveException(String message) {
        this(message, null, false);
    }

    static Supplier<ProcessArchiveException> mandatoryProcessIdMissing(DomainEvent event) {
        return () -> new ProcessArchiveException(
                format("Unable to archive event %s with ID %s, no process ID present in event", event.getType().getName(), event.getIdentity().getId()));
    }

    static ProcessArchiveException failedToReadData(DomainEvent event, Exception ex) {
        return new ProcessArchiveException(
                format("Failed to read archive data for event %s with ID %s", event.getType().getName(), event.getIdentity().getId()), ex, true);
    }

    static ProcessArchiveException processIdFromCorrelationProviderMissing(DomainEvent event, MessageCorrelationProvider<Message> correlationProvider) {
        return new ProcessArchiveException(
                format("Unable to archive event %s with ID %s, no process ID returned from correlation provider %s",
                        event.getType().getName(),
                        event.getIdentity().getId(),
                        correlationProvider.getClass().getName()));
    }
}
