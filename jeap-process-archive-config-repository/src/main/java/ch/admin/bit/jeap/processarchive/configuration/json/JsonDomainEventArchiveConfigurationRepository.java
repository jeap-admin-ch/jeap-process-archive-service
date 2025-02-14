package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.EventConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.model.ProcessArchiveEventConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfigurationRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@Component
@Slf4j
@RequiredArgsConstructor
class JsonDomainEventArchiveConfigurationRepository implements DomainEventArchiveConfigurationRepository {
    private static final String CLASSPATH_LOCATION = "classpath:/processarchive/events.json";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final EventConfigurationDeserializer eventConfigurationDeserializer;
    private final ContractsValidator contractsValidator;
    private Map<String, DomainEventArchiveConfiguration> eventsByName;

    @Value("${jeap.processarchive.consumer-contract-validator.enabled:true}")
    private boolean consumerContractValidatorEnabled = true;

    @Override
    public List<DomainEventArchiveConfiguration> getAll() {
        return List.copyOf(eventsByName.values());
    }

    @Override
    public Optional<DomainEventArchiveConfiguration> findByName(String eventName) {
        return Optional.ofNullable(eventsByName.get(eventName));
    }

    @PostConstruct
    protected void loadTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(CLASSPATH_LOCATION);

        try (var inputStream = resource.getInputStream()) {
            log.info("Loading process archive event configuration from {}", resource.getDescription());
            ProcessArchiveEventConfiguration definition = JSON_MAPPER.readValue(inputStream, ProcessArchiveEventConfiguration.class);
            this.eventsByName = eventConfigurationDeserializer.toDomainEventReferences(definition.getEvents()).stream()
                    .collect(toMap(DomainEventArchiveConfiguration::getEventName, event -> event));
            if (consumerContractValidatorEnabled) {
                this.eventsByName.forEach((key, domainEventArchiveConfiguration) -> validateConsumerContract(domainEventArchiveConfiguration));
            }
        }
    }

    private void validateConsumerContract(DomainEventArchiveConfiguration domainEventArchiveConfiguration) {
        contractsValidator.ensureConsumerContract(domainEventArchiveConfiguration.getEventName(), domainEventArchiveConfiguration.getTopicName());
    }
}
