package ch.admin.bit.jeap.processarchive.registry.verifier.common;

enum CompatibilityMode {
    BACKWARD,
    FORWARD,
    FULL,
    NONE;

    static CompatibilityMode valueOfNullSafe(String compatibilityMode) {
        if (compatibilityMode == null) {
            return null;
        }
        return valueOf(compatibilityMode);
    }

    boolean isForwardOrFull() {
        return this == FORWARD || this == FULL;
    }

    boolean isBackwardOrFull() {
        return this == BACKWARD || this == FULL;
    }
}
