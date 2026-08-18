package ch.admin.bit.jeap.processarchive.adapter.restapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Makes sure the YAML converter of the backfill REST API stays confined to the server-side converters.
 * <p>
 * Registering it as an {@link HttpMessageConverter} bean used to add it to the client converters as well, ahead of
 * the default converters and therefore ahead of the JSON converter. Every {@code RestClient} built from the shared
 * auto-configured {@link RestClient.Builder} then serialized bodies without an explicit content type as YAML.
 */
class RestApiAdapterConfigTest {

    private static final MediaType APPLICATION_X_YAML = MediaType.parseMediaType("application/x-yaml");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class,
                    RestClientAutoConfiguration.class,
                    RestApiAdapterConfig.class));

    @Test
    void restClientFromSharedBuilder_bodyWithoutContentType_isSentAsJson() {
        contextRunner.run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("/api/dbschemas"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("{\"name\":\"pas\",\"version\":1}"))
                    .andRespond(withSuccess());

            builder.build()
                    .post()
                    .uri("/api/dbschemas")
                    .body(new DbSchemaDto("pas", 1))
                    .retrieve()
                    .toBodilessEntity();

            server.verify();
        });
    }

    @Test
    void yamlConverterIsNotExposedAsHttpMessageConverterBean() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(HttpMessageConverter.class));
    }

    @Test
    void serverConverters_containBackfillYamlConverterBehindJsonConverter() {
        contextRunner.run(context -> {
            HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
            context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class).orderedStream()
                    .forEach(customizer -> customizer.customize(builder));

            List<HttpMessageConverter<?>> converters = new ArrayList<>();
            builder.build().forEach(converters::add);

            int jsonIndex = indexOfConverterSupporting(converters, MediaType.APPLICATION_JSON);
            int yamlIndex = indexOfConverterSupporting(converters, APPLICATION_X_YAML);
            assertThat(jsonIndex).isNotNegative();
            assertThat(yamlIndex).isGreaterThan(jsonIndex);
            assertThat(converters.get(yamlIndex).getSupportedMediaTypes())
                    .containsExactly(MediaType.APPLICATION_YAML, APPLICATION_X_YAML);
        });
    }

    private static int indexOfConverterSupporting(List<HttpMessageConverter<?>> converters, MediaType mediaType) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i).getSupportedMediaTypes().contains(mediaType)) {
                return i;
            }
        }
        return -1;
    }

    record DbSchemaDto(String name, int version) {
    }
}
