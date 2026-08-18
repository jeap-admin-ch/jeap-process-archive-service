package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.BackfillJobController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackageClasses = {BackfillJobController.class})
class RestApiAdapterConfig {

    @Bean
    ServerHttpMessageConvertersCustomizer backfillYamlConverterCustomizer() {
        return new BackfillYamlConverterCustomizer();
    }
}
