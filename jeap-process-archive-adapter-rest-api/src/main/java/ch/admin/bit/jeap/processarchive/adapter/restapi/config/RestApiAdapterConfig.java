package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.BackfillJobController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;

@AutoConfiguration
@ComponentScan(basePackageClasses = {BackfillJobController.class})
class RestApiAdapterConfig {

    @Bean
    JacksonYamlHttpMessageConverter yamlHttpMessageConverter() {
        JacksonYamlHttpMessageConverter converter = new JacksonYamlHttpMessageConverter();
        converter.setSupportedMediaTypes(java.util.List.of(
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("application/x-yaml")
        ));
        return converter;
    }
}
