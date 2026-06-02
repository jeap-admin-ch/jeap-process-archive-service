package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.IndexTypeConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.model.IndexTypesConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfigurationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@Component
@Slf4j
@RequiredArgsConstructor
class JsonIndexTypeConfigurationRepository implements IndexTypeConfigurationRepository {
    private static final String CLASSPATH_LOCATION = "classpath:/processarchive/indextypes.json"; // NOSONAR url not configurable
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final IndexTypeConfigurationDeserializer indexTypeConfigurationDeserializer;
    private Map<String, IndexTypeConfiguration> indexesByName;

    @Override
    public List<IndexTypeConfiguration> getAll() {
        return List.copyOf(indexesByName.values());
    }

    @Override
    public Optional<IndexTypeConfiguration> findByName(String indexType) {
        return Optional.ofNullable(indexesByName.get(indexType));
    }

    @PostConstruct
    protected void loadTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(CLASSPATH_LOCATION);
        if (!resource.exists()) {
            log.info("No index types configuration found at {}. Skipping loading ...", CLASSPATH_LOCATION);
            return;
        }

        try (var inputStream = resource.getInputStream()) {
            log.info("Loading index types configuration from {}", resource.getDescription());
            IndexTypesConfiguration definition = JSON_MAPPER.readValue(inputStream, IndexTypesConfiguration.class);
            this.indexesByName = indexTypeConfigurationDeserializer.toConfiguration(definition.getIndexTypes()).stream()
                    .collect(toMap(IndexTypeConfiguration::indexType, i -> i));
        }
    }

}
