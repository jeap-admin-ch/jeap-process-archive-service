package ch.admin.bit.jeap.processarchive.kafka.event;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import static java.nio.charset.StandardCharsets.UTF_8;

class Hashes {

    private static final int DEFAULT_DIGEST_LENGTH_IN_BYTES = 32;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    static String hashReferenceId(String referenceId, String referenceIdType) {
        return hash(referenceId, referenceIdType);
    }

    static String hashReferenceIdType(String referenceIdType) {
        return hash(referenceIdType);
    }

    private static String hash(String... parts) {
        final Blake3Digest messageDigest = new Blake3Digest(DEFAULT_DIGEST_LENGTH_IN_BYTES);
        for (String part : parts) {
            byte[] partBytes = part.getBytes(UTF_8);
            messageDigest.update(partBytes, 0, partBytes.length);
        }
        return generateHash(messageDigest);
    }

    private static String generateHash(Blake3Digest messageDigest) {
        final byte[] hashedString = new byte[messageDigest.getDigestSize()];
        messageDigest.doFinal(hashedString, 0);
        return Hex.encodeHexString(hashedString);
    }
}
