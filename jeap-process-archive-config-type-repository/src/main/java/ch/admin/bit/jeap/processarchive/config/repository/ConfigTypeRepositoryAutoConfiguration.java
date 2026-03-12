package ch.admin.bit.jeap.processarchive.config.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ProcessArchiveRegistryProperties.class)
public class ConfigTypeRepositoryAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jeap.processarchive.registry", name = "types[0].archive-type")
    ConfigArchiveTypeRepository configArchiveTypeRepository(ProcessArchiveRegistryProperties properties,
                                                            ArchiveCryptoService archiveCryptoService) {
        return new ConfigArchiveTypeRepository(properties, archiveCryptoService);
    }
}
