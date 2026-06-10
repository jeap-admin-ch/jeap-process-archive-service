package ch.admin.bit.jeap.processarchive.domain.backfill;

import lombok.Getter;

@Getter
public class BackfillJobException extends RuntimeException {

    private final BackfillJobExceptionReason reason;

    private BackfillJobException(BackfillJobExceptionReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static BackfillJobException invalidRequest(String message) {
        return new BackfillJobException(BackfillJobExceptionReason.INVALID_REQUEST, message);
    }

    public static BackfillJobException configurationNotFound(String messageName, String topicName) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_NOT_FOUND,
                "No process archive configuration found for message '%s' and topic '%s'".formatted(messageName, topicName));
    }

    public static BackfillJobException configurationNotRemoteData(String messageName, String topicName) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_NOT_REMOTE_DATA,
                "Process archive configuration for message '%s' and topic '%s' is not a remote-data configuration".formatted(messageName, topicName));
    }

    public static BackfillJobException conflict(String message) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFLICT, message);
    }
}
