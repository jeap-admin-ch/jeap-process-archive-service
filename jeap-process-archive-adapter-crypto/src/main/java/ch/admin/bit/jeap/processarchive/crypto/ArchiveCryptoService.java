package ch.admin.bit.jeap.processarchive.crypto;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReference;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.crypto.vault.keymanagement.VaultKeyLocation;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyId;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.EncryptionKeyReference;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ArchiveCryptoService {

    private final Optional<KeyReferenceCryptoService> keyReferenceCryptoService;
    private final Optional<KeyIdCryptoService> keyIdCryptoService;

    @Timed(value = "jeap_pas_encrypt_payload", description = "Encrypt payload with crypto service and return encrypted payload")
    public byte[] encrypt(byte[] payload, ArchiveDataEncryption encryption) {
        if (encryption.getEncryptionKeyReference() != null) {
            return encrypt(payload, encryption.getEncryptionKeyReference());
        } else if (encryption.getEncryptionKeyId() != null) {
            return encrypt(payload, encryption.getEncryptionKeyId());
        } else {
            throw new IllegalStateException("Unexpected encryption invocation - no key reference or ID configured");
        }
    }

    private byte[] encrypt(byte[] payload, EncryptionKeyReference encryptionKeyReference) {
        KeyReference vaultKeyRef = VaultKeyLocation.asKeyReference(encryptionKeyReference.getSecretEnginePath(), encryptionKeyReference.getKeyName());
        return getCryptoService().encrypt(payload, vaultKeyRef);
    }

    private KeyReferenceCryptoService getCryptoService() {
        return keyReferenceCryptoService.orElseThrow(() -> new IllegalStateException("No Bean KeyReferenceCryptoService found"));
    }

    private byte[] encrypt(byte[] payload, EncryptionKeyId encryptionKeyId) {
        KeyId keyId = KeyId.of(encryptionKeyId.getKeyId());
        return getKeyIdCryptoService().encrypt(payload, keyId);
    }

    private KeyIdCryptoService getKeyIdCryptoService() {
        return keyIdCryptoService.orElseThrow(() -> new IllegalStateException("No Bean KeyIdCryptoService found"));
    }
}
