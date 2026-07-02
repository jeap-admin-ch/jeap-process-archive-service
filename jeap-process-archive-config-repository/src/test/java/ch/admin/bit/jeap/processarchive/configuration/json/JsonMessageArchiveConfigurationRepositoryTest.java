package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.MessageConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.SpringExpressionEvaluator;
import ch.admin.bit.jeap.processarchive.configuration.json.test.*;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {MessageConfigurationDeserializer.class, SpringExpressionEvaluator.class, TestMetricsConfig.class},
        properties = "jme.event-service.uri=http://localhost")
class JsonMessageArchiveConfigurationRepositoryTest {

    @MockitoBean
    private RemoteArchiveDataProvider remoteArchiveDataProvider;

    @Autowired
    private MessageConfigurationDeserializer messageConfigurationDeserializer;

    private JsonMessageArchiveConfigurationRepository jsonDomainEventArchiveConfigurationRepository;

    @MockitoBean
    private ContractsValidator contractsValidator;

    @BeforeEach
    void setUp() throws IOException {
        jsonDomainEventArchiveConfigurationRepository = new JsonMessageArchiveConfigurationRepository(messageConfigurationDeserializer, contractsValidator);
        jsonDomainEventArchiveConfigurationRepository.loadTemplates();
    }

    @Test
    void getAllDomainEventReferenceDefinitions_eventsFound() {
        List<MessageArchiveConfiguration> allMessageArchiveConfigurationDefinitions = jsonDomainEventArchiveConfigurationRepository.getAll();
        assertEquals(9, allMessageArchiveConfigurationDefinitions.size());
    }

    @Test
    void consumerContractsFromTemplateHasBeenValidated() {
        verify(contractsValidator).ensureConsumerContract("JmeRaceStartedEvent", "jme-race-started");
    }

    @Test
    void findByName_eventNotFound() {
        List<MessageArchiveConfiguration> dummy = jsonDomainEventArchiveConfigurationRepository.findByName("dummy");
        assertTrue(dummy.isEmpty());
    }

    @Test
    void findByName_eventFound_payload() {
        PayloadDataMessageArchiveConfiguration domainEventReference = (PayloadDataMessageArchiveConfiguration) findSingle("JmeRaceStartedEvent");
        assertEquals("JmeRaceStartedEvent", domainEventReference.getMessageName());
        assertEquals("jme-race-started", domainEventReference.getTopicName());
        assertEquals(TestDomainEventArchiveDataProvider.class, domainEventReference.getMessageArchiveDataProvider().getClass());
    }

    @Test
    void findByName_multipleConfigurationsForSameMessage_returnsAll() {
        List<MessageArchiveConfiguration> configurations = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithMultipleConfigs");

        assertEquals(2, configurations.size());
        assertTrue(configurations.stream().allMatch(config -> config.getMessageName().equals("eventWithMultipleConfigs")));
        assertTrue(configurations.stream().allMatch(config -> config.getTopicName().equals("topic-multi")));
        assertTrue(configurations.stream().anyMatch(PayloadDataMessageArchiveConfiguration.class::isInstance));
        assertTrue(configurations.stream().anyMatch(RemoteDataMessageArchiveConfiguration.class::isInstance));
    }

    @Test
    void findByName_eventFound_cluster() {
        MessageArchiveConfiguration eventWithCluster = findSingle("eventWithCluster");
        assertEquals("eventWithCluster", eventWithCluster.getMessageName());
        assertEquals("topic-cluster", eventWithCluster.getTopicName());
        assertEquals("cluster-name", eventWithCluster.getClusterName());
    }

    @Test
    void conditionHasBeenInstantiated() {
        MessageArchiveConfiguration eventWithCondition = findSingle("eventWithCondition");
        assertSame(TestCondition.class, eventWithCondition.getArchiveDataCondition().getClass());
    }

    @Test
    void conditionHasBeenInstantiatedWithRemoteDataProvider() {
        MessageArchiveConfiguration eventWithConditionRemoteData = findSingle("eventWithConditionRemoteData");
        assertSame(TestCondition.class, eventWithConditionRemoteData.getArchiveDataCondition().getClass());
    }

    @Test
    void findByName_eventFound_reference() {
        RemoteDataMessageArchiveConfiguration domainEventReferenceDefinition = (RemoteDataMessageArchiveConfiguration) findSingle("JmeRaceMobileCheckpointPassedEvent");
        assertEquals("JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getMessageName());
        assertEquals("jme-race-mobilecheckpoint-passed", domainEventReferenceDefinition.getTopicName());
        assertEquals(TestReferenceProvider.class, domainEventReferenceDefinition.getReferenceProvider().getClass());
        assertEquals("http://localhost/api/v1/archive/JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getDataReaderEndpoint());
        assertEquals("my-pas-service", domainEventReferenceDefinition.getOauthClientId());
    }

    @Test
    void findByName_eventFound_processDataArchiveProvider() {
        RemoteDataMessageArchiveConfiguration domainEventReferenceDefinition = (RemoteDataMessageArchiveConfiguration) findSingle("eventWithConditionRemoteDataInPayload");
        assertEquals("eventWithConditionRemoteDataInPayload", domainEventReferenceDefinition.getMessageName());
        assertEquals("jme-race-mobilecheckpoint-passed", domainEventReferenceDefinition.getTopicName());
        assertEquals(TestArchiveDataReferenceProvider.class, domainEventReferenceDefinition.getArchiveDataReferenceProvider().getClass());
        assertEquals("http://localhost/api/v1/archive/JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getDataReaderEndpoint());
        assertEquals("my-pas-service", domainEventReferenceDefinition.getOauthClientId());
    }

    @Test
    void correlationProviderHasBeenInstantiated() {
        MessageArchiveConfiguration eventWithCorrelationProvider = findSingle("eventWithCorrelationProvider");
        assertSame(TestCorrelationProvider.class, eventWithCorrelationProvider.getCorrelationProvider().getClass());
    }

    @Test
    void validateConfigurationsShareTopic_sameTopic_doesNotThrow() {
        List<MessageArchiveConfiguration> configurations = List.of(
                payloadConfiguration("SameTopicEvent", "topic-a", null),
                payloadConfiguration("SameTopicEvent", "topic-a", null));

        assertDoesNotThrow(() -> JsonMessageArchiveConfigurationRepository.validateConfigurationsShareTopic("SameTopicEvent", configurations));
    }

    @Test
    void validateConfigurationsShareTopic_differentTopics_throws() {
        List<MessageArchiveConfiguration> configurations = List.of(
                payloadConfiguration("MixedTopicEvent", "topic-a", null),
                payloadConfiguration("MixedTopicEvent", "topic-b", null));

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> JsonMessageArchiveConfigurationRepository.validateConfigurationsShareTopic("MixedTopicEvent", configurations));
        assertTrue(exception.getMessage().contains("topic-a"));
        assertTrue(exception.getMessage().contains("topic-b"));
    }

    @Test
    void findByName_multipleConfigurationsForSameMessage_haveDistinctIds() {
        List<MessageArchiveConfiguration> configurations = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithMultipleConfigs");

        assertEquals(List.of("payload", "remote"),
                configurations.stream().map(MessageArchiveConfiguration::getId).sorted().toList());
    }

    @Test
    void validateConfigurationIds_singleConfigurationWithoutId_doesNotThrow() {
        List<MessageArchiveConfiguration> configurations = List.of(payloadConfiguration(null, "SingleEvent", "topic-a", null));

        assertDoesNotThrow(() -> JsonMessageArchiveConfigurationRepository.validateConfigurationIds("SingleEvent", configurations));
    }

    @Test
    void validateConfigurationIds_multipleConfigurationsWithUniqueIds_doesNotThrow() {
        List<MessageArchiveConfiguration> configurations = List.of(
                payloadConfiguration("a", "MultiEvent", "topic-a", null),
                payloadConfiguration("b", "MultiEvent", "topic-a", null));

        assertDoesNotThrow(() -> JsonMessageArchiveConfigurationRepository.validateConfigurationIds("MultiEvent", configurations));
    }

    @Test
    void validateConfigurationIds_multipleConfigurationsWithMissingId_throws() {
        List<MessageArchiveConfiguration> configurations = List.of(
                payloadConfiguration("a", "MultiEvent", "topic-a", null),
                payloadConfiguration(null, "MultiEvent", "topic-a", null));

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> JsonMessageArchiveConfigurationRepository.validateConfigurationIds("MultiEvent", configurations));
        assertTrue(exception.getMessage().contains("non-blank 'id'"));
    }

    @Test
    void validateConfigurationIds_multipleConfigurationsWithDuplicateIds_throws() {
        List<MessageArchiveConfiguration> configurations = List.of(
                payloadConfiguration("dup", "MultiEvent", "topic-a", null),
                payloadConfiguration("dup", "MultiEvent", "topic-a", null));

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> JsonMessageArchiveConfigurationRepository.validateConfigurationIds("MultiEvent", configurations));
        assertTrue(exception.getMessage().contains("duplicate ids"));
    }

    @Test
    void validateConfigurationIds_idTooLong_throws() {
        String tooLongId = "x".repeat(256);
        List<MessageArchiveConfiguration> configurations = List.of(payloadConfiguration(tooLongId, "SingleEvent", "topic-a", null));

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> JsonMessageArchiveConfigurationRepository.validateConfigurationIds("SingleEvent", configurations));
        assertTrue(exception.getMessage().contains("must not exceed"));
    }

    private static MessageArchiveConfiguration payloadConfiguration(String messageName, String topicName, String clusterName) {
        return payloadConfiguration(null, messageName, topicName, clusterName);
    }

    private static MessageArchiveConfiguration payloadConfiguration(String id, String messageName, String topicName, String clusterName) {
        return PayloadDataMessageArchiveConfiguration.builder()
                .id(id)
                .messageName(messageName)
                .topicName(topicName)
                .clusterName(clusterName)
                .messageArchiveDataProvider(message -> null)
                .build();
    }

    private MessageArchiveConfiguration findSingle(String messageName) {
        List<MessageArchiveConfiguration> configurations = jsonDomainEventArchiveConfigurationRepository.findByName(messageName);
        assertEquals(1, configurations.size(), "Expected exactly one configuration for " + messageName);
        return configurations.getFirst();
    }
}
