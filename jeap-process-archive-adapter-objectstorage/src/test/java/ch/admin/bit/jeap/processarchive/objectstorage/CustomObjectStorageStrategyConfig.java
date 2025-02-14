package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CustomObjectStorageStrategyConfig {

    @Bean
    ObjectStorageStrategy objectStorageStrategy() {
        return new CustomObjectStorageStrategy();
    }

}
