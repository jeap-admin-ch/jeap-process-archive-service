package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.MessageConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.model.ProcessArchiveMessageConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Component
@Slf4j
@RequiredArgsConstructor
class JsonMessageArchiveConfigurationRepository implements MessageArchiveConfigurationRepository {
    static final String DEFAULT_CLASSPATH_LOCATION = "classpath:/processarchive/messages.json"; // NOSONAR url not configurable
    static final int MAX_CONFIG_ID_LENGTH = 255;
    private static final String LEGACY_CLASSPATH_LOCATION = "classpath:/processarchive/events.json"; // NOSONAR url not configurable
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private final MessageConfigurationDeserializer messageConfigurationDeserializer;
    private final ContractsValidator contractsValidator;
    private Map<String, List<MessageArchiveConfiguration>> messagesByName;

    /**
     * Location of the archive configuration file. Defaults to {@value #DEFAULT_CLASSPATH_LOCATION}; override to isolate
     * a specific configuration (e.g. in tests).
     */
    @Value("${jeap.processarchive.configuration.location:" + DEFAULT_CLASSPATH_LOCATION + "}")
    private String configurationLocation = DEFAULT_CLASSPATH_LOCATION;

    @Value("${jeap.processarchive.consumer-contract-validator.enabled:true}")
    private boolean consumerContractValidatorEnabled = true;

    @Override
    public List<MessageArchiveConfiguration> getAll() {
        return messagesByName.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public List<MessageArchiveConfiguration> findByName(String messageName) {
        return messagesByName.getOrDefault(messageName, List.of());
    }

    @PostConstruct
    protected void loadTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(configurationLocation);
        if (!resource.exists()) {
            resource = resolver.getResource(LEGACY_CLASSPATH_LOCATION);
            if (!resource.exists()) {
                throw new MessageArchiveConfigurationException("No process archive message configuration file found in classpath at " + configurationLocation + " or " + LEGACY_CLASSPATH_LOCATION);
            }
        }

        try (var inputStream = resource.getInputStream()) {
            log.info("Loading process archive message configuration from {}", resource.getDescription());
            ProcessArchiveMessageConfiguration definition = JSON_MAPPER.readValue(inputStream, ProcessArchiveMessageConfiguration.class);
            this.messagesByName = messageConfigurationDeserializer.toConfiguration(definition.getMessages()).stream()
                    .collect(groupingBy(MessageArchiveConfiguration::getMessageName));
            this.messagesByName.forEach(JsonMessageArchiveConfigurationRepository::validateConfigurationsShareTopic);
            this.messagesByName.forEach(JsonMessageArchiveConfigurationRepository::validateConfigurationIds);
            if (consumerContractValidatorEnabled) {
                getAll().forEach(this::validateConsumerContract);
            }
        }
    }

    /**
     * Multiple archive configurations may be registered for the same message to archive multiple artifacts, but they
     * must all reference the same topic (and cluster): the runtime applies all configurations of a message regardless
     * of the topic a message was consumed from, so mixing topics for one message name is rejected as a misconfiguration.
     */
    static void validateConfigurationsShareTopic(String messageName, List<MessageArchiveConfiguration> configurations) {
        List<String> distinctTopics = configurations.stream()
                .map(config -> config.getTopicName() + "@" + config.getClusterName())
                .distinct()
                .sorted()
                .toList();
        if (distinctTopics.size() > 1) {
            throw new MessageArchiveConfigurationException(
                    "All archive configurations for message '" + messageName + "' must use the same topic and cluster, but found (topic@cluster): " + distinctTopics);
        }
    }

    /**
     * When a message has more than one configuration, each configuration must carry a non-blank, unique {@code id}
     * so that a specific configuration can be addressed unambiguously (e.g. for backfill jobs). A single configuration
     * may omit the id.
     */
    static void validateConfigurationIds(String messageName, List<MessageArchiveConfiguration> configurations) {
        configurations.stream()
                .map(MessageArchiveConfiguration::getId)
                .filter(id -> id != null && id.length() > MAX_CONFIG_ID_LENGTH)
                .findFirst()
                .ifPresent(id -> {
                    throw new MessageArchiveConfigurationException(
                            "Archive configuration 'id' for message '" + messageName + "' must not exceed " + MAX_CONFIG_ID_LENGTH + " characters: '" + id + "'");
                });
        if (configurations.size() <= 1) {
            return;
        }
        List<String> ids = configurations.stream().map(MessageArchiveConfiguration::getId).toList();
        if (ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new MessageArchiveConfigurationException(
                    "Message '" + messageName + "' has multiple archive configurations, so each configuration must define a non-blank 'id'.");
        }
        if (ids.stream().distinct().count() != ids.size()) {
            throw new MessageArchiveConfigurationException(
                    "Message '" + messageName + "' has archive configurations with duplicate ids: " + ids.stream().sorted().toList());
        }
    }

    private void validateConsumerContract(MessageArchiveConfiguration messageArchiveConfiguration) {
        contractsValidator.ensureConsumerContract(messageArchiveConfiguration.getMessageName(), messageArchiveConfiguration.getTopicName());
    }
}
