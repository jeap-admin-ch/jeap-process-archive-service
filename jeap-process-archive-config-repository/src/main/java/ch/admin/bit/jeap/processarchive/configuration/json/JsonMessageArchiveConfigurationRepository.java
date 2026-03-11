package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.MessageConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.model.ProcessArchiveMessageConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@Component
@Slf4j
@RequiredArgsConstructor
class JsonMessageArchiveConfigurationRepository implements MessageArchiveConfigurationRepository {
    private static final String LEGACY_CLASSPATH_LOCATION = "classpath:/processarchive/events.json"; // NOSONAR url not configurable
    private static final String CLASSPATH_LOCATION = "classpath:/processarchive/messages.json"; // NOSONAR url not configurable
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final MessageConfigurationDeserializer messageConfigurationDeserializer;
    private final ContractsValidator contractsValidator;
    private Map<String, MessageArchiveConfiguration> messagesByName;

    @Value("${jeap.processarchive.consumer-contract-validator.enabled:true}")
    private boolean consumerContractValidatorEnabled = true;

    @Override
    public List<MessageArchiveConfiguration> getAll() {
        return List.copyOf(messagesByName.values());
    }

    @Override
    public Optional<MessageArchiveConfiguration> findByName(String messageName) {
        return Optional.ofNullable(messagesByName.get(messageName));
    }

    @PostConstruct
    protected void loadTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(CLASSPATH_LOCATION);
        if (!resource.exists()) {
            resource = resolver.getResource(LEGACY_CLASSPATH_LOCATION);
            if (!resource.exists()) {
                throw new MessageArchiveConfigurationException("No process archive message configuration file found in classpath at " + CLASSPATH_LOCATION + " or " + LEGACY_CLASSPATH_LOCATION);
            }
        }

        try (var inputStream = resource.getInputStream()) {
            log.info("Loading process archive message configuration from {}", resource.getDescription());
            ProcessArchiveMessageConfiguration definition = JSON_MAPPER.readValue(inputStream, ProcessArchiveMessageConfiguration.class);
            this.messagesByName = messageConfigurationDeserializer.toConfiguration(definition.getMessages()).stream()
                    .collect(toMap(MessageArchiveConfiguration::getMessageName, event -> event));
            if (consumerContractValidatorEnabled) {
                this.messagesByName.forEach((key, domainEventArchiveConfiguration) -> validateConsumerContract(domainEventArchiveConfiguration));
            }
        }
    }

    private void validateConsumerContract(MessageArchiveConfiguration messageArchiveConfiguration) {
        contractsValidator.ensureConsumerContract(messageArchiveConfiguration.getMessageName(), messageArchiveConfiguration.getTopicName());
    }
}
