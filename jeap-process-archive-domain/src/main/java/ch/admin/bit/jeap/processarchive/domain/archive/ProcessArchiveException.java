package ch.admin.bit.jeap.processarchive.domain.archive;

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

    static Supplier<ProcessArchiveException> mandatoryProcessIdMissing(Message message) {
        return () -> new ProcessArchiveException(
                format("Unable to archive message %s with ID %s, no process ID present in message",
                        message.getType().getName(), message.getIdentity().getId()));
    }

    static ProcessArchiveException failedToReadData(Message message, Exception ex) {
        return new ProcessArchiveException(
                format("Failed to read archive data for message %s with ID %s", message.getType().getName(), message.getIdentity().getId()), ex, true);
    }

    static ProcessArchiveException duplicateArtifactIdempotenceId(Message message, String idempotenceId) {
        return new ProcessArchiveException(
                format("Multiple archive configurations for message %s with ID %s produced artifacts with the same " +
                                "idempotence ID '%s'. Configurations registered for the same message must produce distinct artifacts " +
                                "(distinct system/schema/referenceId/version).",
                        message.getType().getName(), message.getIdentity().getId(), idempotenceId));
    }

    static ProcessArchiveException processIdFromCorrelationProviderMissing(Message message,
                                                                           MessageCorrelationProvider<Message> correlationProvider) {
        return new ProcessArchiveException(
                format("Unable to archive message %s with ID %s, no process ID returned from correlation provider %s",
                        message.getType().getName(),
                        message.getIdentity().getId(),
                        correlationProvider.getClass().getName()));
    }
}
