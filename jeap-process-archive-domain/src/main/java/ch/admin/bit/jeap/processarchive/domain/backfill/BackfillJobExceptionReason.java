package ch.admin.bit.jeap.processarchive.domain.backfill;

public enum BackfillJobExceptionReason {
    INVALID_REQUEST,
    CONFIGURATION_NOT_FOUND,
    CONFIGURATION_NOT_REMOTE_DATA,
    CONFIGURATION_AMBIGUOUS,
    CONFLICT
}
