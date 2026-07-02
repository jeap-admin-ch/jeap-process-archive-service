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

    public static BackfillJobException configurationNotFound(String messageName) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_NOT_FOUND,
                "No process archive configuration found for message '%s'".formatted(messageName));
    }

    public static BackfillJobException configurationNotRemoteData(String messageName) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_NOT_REMOTE_DATA,
                "Process archive configuration for message '%s' is not a remote-data configuration".formatted(messageName));
    }

    public static BackfillJobException configurationAmbiguous(String messageName) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_AMBIGUOUS,
                "Multiple remote-data process archive configurations found for message '%s'; specify 'config-id' to select one".formatted(messageName));
    }

    public static BackfillJobException configurationConfigIdNotFound(String messageName, String configId) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFIGURATION_NOT_FOUND,
                "No remote-data process archive configuration found for message '%s' and config-id '%s'".formatted(messageName, configId));
    }

    public static BackfillJobException conflict(String message) {
        return new BackfillJobException(BackfillJobExceptionReason.CONFLICT, message);
    }
}
