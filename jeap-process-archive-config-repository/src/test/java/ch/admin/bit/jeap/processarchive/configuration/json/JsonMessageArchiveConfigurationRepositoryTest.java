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
import java.util.Optional;

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
        assertEquals(7, allMessageArchiveConfigurationDefinitions.size());
    }

    @Test
    void consumerContractsFromTemplateHasBeenValidated() {
        verify(contractsValidator).ensureConsumerContract("JmeRaceStartedEvent", "jme-race-started");
    }

    @Test
    void findByName_eventNotFound() {
        Optional<MessageArchiveConfiguration> dummy = jsonDomainEventArchiveConfigurationRepository.findByName("dummy");
        assertFalse(dummy.isPresent());
    }

    @Test
    void findByName_eventFound_payload() {
        Optional<MessageArchiveConfiguration> jmeRaceStartedEvent = jsonDomainEventArchiveConfigurationRepository.findByName("JmeRaceStartedEvent");
        assertTrue(jmeRaceStartedEvent.isPresent());
        PayloadDataMessageArchiveConfiguration domainEventReference = (PayloadDataMessageArchiveConfiguration) jmeRaceStartedEvent.get();
        assertEquals("JmeRaceStartedEvent", domainEventReference.getMessageName());
        assertEquals("jme-race-started", domainEventReference.getTopicName());
        assertEquals(TestDomainEventArchiveDataProvider.class, domainEventReference.getMessageArchiveDataProvider().getClass());
    }

    @Test
    void findByName_eventFound_cluster() {
        Optional<MessageArchiveConfiguration> eventWithCluster = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCluster");
        assertTrue(eventWithCluster.isPresent());
        assertEquals("eventWithCluster", eventWithCluster.get().getMessageName());
        assertEquals("topic-cluster", eventWithCluster.get().getTopicName());
        assertEquals("cluster-name", eventWithCluster.get().getClusterName());
    }

    @Test
    void conditionHasBeenInstantiated() {
        Optional<MessageArchiveConfiguration> eventWithCondition = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCondition");
        assertTrue(eventWithCondition.isPresent());
        assertSame(TestCondition.class, eventWithCondition.get().getArchiveDataCondition().getClass());
    }

    @Test
    void conditionHasBeenInstantiatedWithRemoteDataProvider() {
        Optional<MessageArchiveConfiguration> eventWithConditionRemoteData = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithConditionRemoteData");
        assertTrue(eventWithConditionRemoteData.isPresent());
        assertSame(TestCondition.class, eventWithConditionRemoteData.get().getArchiveDataCondition().getClass());
    }

    @Test
    void findByName_eventFound_reference() {
        Optional<MessageArchiveConfiguration> jmeRaceStartedEvent = jsonDomainEventArchiveConfigurationRepository.findByName("JmeRaceMobileCheckpointPassedEvent");
        assertTrue(jmeRaceStartedEvent.isPresent());
        RemoteDataMessageArchiveConfiguration domainEventReferenceDefinition = (RemoteDataMessageArchiveConfiguration) jmeRaceStartedEvent.get();
        assertEquals("JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getMessageName());
        assertEquals("jme-race-mobilecheckpoint-passed", domainEventReferenceDefinition.getTopicName());
        assertEquals(TestReferenceProvider.class, domainEventReferenceDefinition.getReferenceProvider().getClass());
        assertEquals("http://localhost/api/v1/archive/JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getDataReaderEndpoint());
        assertEquals("my-pas-service", domainEventReferenceDefinition.getOauthClientId());
    }

    @Test
    void findByName_eventFound_processDataArchiveProvider() {
        Optional<MessageArchiveConfiguration> jmeRaceStartedEvent = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithConditionRemoteDataInPayload");
        assertTrue(jmeRaceStartedEvent.isPresent());
        RemoteDataMessageArchiveConfiguration domainEventReferenceDefinition = (RemoteDataMessageArchiveConfiguration) jmeRaceStartedEvent.get();
        assertEquals("eventWithConditionRemoteDataInPayload", domainEventReferenceDefinition.getMessageName());
        assertEquals("jme-race-mobilecheckpoint-passed", domainEventReferenceDefinition.getTopicName());
        assertEquals(TestArchiveDataReferenceProvider.class, domainEventReferenceDefinition.getArchiveDataReferenceProvider().getClass());
        assertEquals("http://localhost/api/v1/archive/JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getDataReaderEndpoint());
        assertEquals("my-pas-service", domainEventReferenceDefinition.getOauthClientId());
    }

    @Test
    void correlationProviderHasBeenInstantiated() {
        Optional<MessageArchiveConfiguration> eventWithCorrelationProvider = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCorrelationProvider");
        assertTrue(eventWithCorrelationProvider.isPresent());
        assertSame(TestCorrelationProvider.class, eventWithCorrelationProvider.get().getCorrelationProvider().getClass());
    }
}
