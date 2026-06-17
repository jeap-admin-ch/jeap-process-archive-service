package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import ch.admin.bit.jeap.processarchive.adapter.restapi.BackfillJobController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.util.List;

@AutoConfiguration
@ComponentScan(basePackageClasses = {BackfillJobController.class})
class RestApiAdapterConfig {

    @Bean
    JacksonYamlHttpMessageConverter yamlHttpMessageConverter() {
        var yamlFactory = YAMLFactory.builder()
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .build();

        var yamlMapper = new YAMLMapper(yamlFactory);

        var converter =
                new JacksonYamlHttpMessageConverter(yamlMapper);

        converter.setSupportedMediaTypes(List.of(
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("application/x-yaml")
        ));
        return converter;
    }
}
