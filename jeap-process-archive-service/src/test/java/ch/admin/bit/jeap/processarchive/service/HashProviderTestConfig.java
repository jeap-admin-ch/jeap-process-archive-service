package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
class HashProviderTestConfig {

    @Bean
    HashProvider hashProvider() {
        return new HashProvider() {
            @Override
            public String hashPayload(byte[] payload) {
                return Integer.toString(Arrays.hashCode(payload));
            }

            @Override
            public String hashStorageObjectId(String referenceId, String referenceIdType) {
                return "hash_" + referenceId + "_" + referenceIdType;
            }
        };
    }

}
