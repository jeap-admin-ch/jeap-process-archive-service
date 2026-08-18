package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.util.List;

/**
 * Registers the YAML converter used by the backfill REST API on the server-side message converters.
 * <p>
 * The converter must not be exposed as an {@link org.springframework.http.converter.HttpMessageConverter} bean:
 * such a bean is collected by both the client and the server converters customizer of Spring Boot's
 * {@code HttpMessageConvertersAutoConfiguration}, and the client customizer adds it <em>ahead of the default
 * converters</em>. It would therefore precede the JSON converter in the auto-configured {@code RestClient.Builder}
 * shared by the whole application, which makes every request that does not set a content type explicitly serialize
 * its body as YAML.
 * <p>
 * {@link HttpMessageConverters.Builder#withYamlConverter(org.springframework.http.converter.HttpMessageConverter)}
 * replaces the default Jackson YAML converter at its existing position among the core converters, i.e. after the JSON
 * converter, instead of changing the order of the converters. The backfill endpoints declare their media types with
 * {@code consumes}/{@code produces}, so they do not depend on the converter being placed ahead of the defaults.
 */
public class BackfillYamlConverterCustomizer implements ServerHttpMessageConvertersCustomizer {

    @Override
    public void customize(HttpMessageConverters.ServerBuilder builder) {
        builder.withYamlConverter(backfillYamlConverter());
    }

    private static JacksonYamlHttpMessageConverter backfillYamlConverter() {
        var yamlFactory = YAMLFactory.builder()
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .build();

        var yamlMapper = new YAMLMapper(yamlFactory);

        var converter = new JacksonYamlHttpMessageConverter(yamlMapper);

        converter.setSupportedMediaTypes(List.of(
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("application/x-yaml")
        ));
        return converter;
    }
}
