package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Creates the idempotence ID for an archived artifact in the format {@code <messageType>_<sha256-hex>}.
 * <p>
 * The idempotenceId of a message is only unique within the message type, and a single message may produce
 * multiple artifacts (one per configuration registered for the message). The hash therefore covers the
 * message idempotenceId and a discriminator derived from the artifact (system, schema, referenceId and -
 * if present - version), keeping the ID unique per artifact. Each field in the hash input is length-prefixed
 * so that field boundaries are unambiguous and distinct field values cannot produce the same hash input.
 * Hashing bounds the length of the ID to the message type name plus 65 characters, while the message type
 * prefix keeps the ID attributable in logs. The ID is derived deterministically so retries stay idempotent.
 */
public final class ArchiveArtifactIdempotenceId {

    private ArchiveArtifactIdempotenceId() {
    }

    public static String create(String messageTypeName, String messageIdempotenceId, ArchiveData archiveData) {
        return create(messageTypeName, messageIdempotenceId,
                archiveData.getSystem(), archiveData.getSchema(), archiveData.getReferenceId(), archiveData.getVersion());
    }

    public static String create(String messageTypeName, String messageIdempotenceId,
                                String system, String schema, String referenceId, Integer version) {
        String hashInput = lengthPrefixed(messageIdempotenceId)
                + lengthPrefixed(system)
                + lengthPrefixed(schema)
                + lengthPrefixed(referenceId)
                + (version != null ? lengthPrefixed(String.valueOf(version)) : "");
        return messageTypeName + "_" + sha256Hex(hashInput);
    }

    private static String lengthPrefixed(String value) {
        return value.length() + ":" + value;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is required to be supported by every JDK implementation
            throw new IllegalStateException("SHA-256 message digest not available", ex);
        }
    }
}
