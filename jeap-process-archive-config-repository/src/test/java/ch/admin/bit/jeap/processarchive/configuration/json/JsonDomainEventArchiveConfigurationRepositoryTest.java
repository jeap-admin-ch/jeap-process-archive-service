package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.EventConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.SpringExpressionEvaluator;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestCondition;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestCorrelationProvider;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestDomainEventArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestReferenceProvider;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataDomainEventArchiveConfiguration;
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

@SpringBootTest(classes = {EventConfigurationDeserializer.class, SpringExpressionEvaluator.class, TestMetricsConfig.class},
        properties = "jme.event-service.uri=http://localhost")
class JsonDomainEventArchiveConfigurationRepositoryTest {

    @MockitoBean
    private RemoteArchiveDataProvider remoteArchiveDataProvider;

    @Autowired
    private EventConfigurationDeserializer eventConfigurationDeserializer;

    private JsonDomainEventArchiveConfigurationRepository jsonDomainEventArchiveConfigurationRepository;

    @MockitoBean
    private ContractsValidator contractsValidator;

    @BeforeEach
    void setUp() throws IOException {
        jsonDomainEventArchiveConfigurationRepository = new JsonDomainEventArchiveConfigurationRepository(eventConfigurationDeserializer, contractsValidator);
        jsonDomainEventArchiveConfigurationRepository.loadTemplates();
    }

    @Test
    void getAllDomainEventReferenceDefinitions_eventsFound() {
        List<DomainEventArchiveConfiguration> allDomainEventArchiveConfigurationDefinitions = jsonDomainEventArchiveConfigurationRepository.getAll();
        assertEquals(6, allDomainEventArchiveConfigurationDefinitions.size());
    }

    @Test
    void consumerContractsFromTemplateHasBeenValidated() {
        verify(contractsValidator).ensureConsumerContract("JmeRaceStartedEvent", "jme-race-started");
    }

    @Test
    void findByName_eventNotFound() {
        Optional<DomainEventArchiveConfiguration> dummy = jsonDomainEventArchiveConfigurationRepository.findByName("dummy");
        assertFalse(dummy.isPresent());
    }

    @Test
    void findByName_eventFound_payload() {
        Optional<DomainEventArchiveConfiguration> jmeRaceStartedEvent = jsonDomainEventArchiveConfigurationRepository.findByName("JmeRaceStartedEvent");
        assertTrue(jmeRaceStartedEvent.isPresent());
        PayloadDataDomainEventArchiveConfiguration domainEventReference = (PayloadDataDomainEventArchiveConfiguration) jmeRaceStartedEvent.get();
        assertEquals("JmeRaceStartedEvent", domainEventReference.getEventName());
        assertEquals("jme-race-started", domainEventReference.getTopicName());
        assertEquals(TestDomainEventArchiveDataProvider.class, domainEventReference.getDomainEventArchiveDataProvider().getClass());
    }

    @Test
    void findByName_eventFound_cluster() {
        Optional<DomainEventArchiveConfiguration> eventWithCluster = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCluster");
        assertTrue(eventWithCluster.isPresent());
        assertEquals("eventWithCluster", eventWithCluster.get().getEventName());
        assertEquals("topic-cluster", eventWithCluster.get().getTopicName());
        assertEquals("cluster-name", eventWithCluster.get().getClusterName());
    }

    @Test
    void conditionHasBeenInstantiated() {
        Optional<DomainEventArchiveConfiguration> eventWithCondition = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCondition");
        assertTrue(eventWithCondition.isPresent());
        assertSame(TestCondition.class, eventWithCondition.get().getArchiveDataCondition().getClass());
    }

    @Test
    void conditionHasBeenInstantiatedWithRemoteDataProvider() {
        Optional<DomainEventArchiveConfiguration> eventWithConditionRemoteData = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithConditionRemoteData");
        assertTrue(eventWithConditionRemoteData.isPresent());
        assertSame(TestCondition.class, eventWithConditionRemoteData.get().getArchiveDataCondition().getClass());
    }

    @Test
    void findByName_eventFound_reference() {
        Optional<DomainEventArchiveConfiguration> jmeRaceStartedEvent = jsonDomainEventArchiveConfigurationRepository.findByName("JmeRaceMobileCheckpointPassedEvent");
        assertTrue(jmeRaceStartedEvent.isPresent());
        RemoteDataDomainEventArchiveConfiguration domainEventReferenceDefinition = (RemoteDataDomainEventArchiveConfiguration) jmeRaceStartedEvent.get();
        assertEquals("JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getEventName());
        assertEquals("jme-race-mobilecheckpoint-passed", domainEventReferenceDefinition.getTopicName());
        assertEquals(TestReferenceProvider.class, domainEventReferenceDefinition.getReferenceProvider().getClass());
        assertEquals("http://localhost/api/v1/archive/JmeRaceMobileCheckpointPassedEvent", domainEventReferenceDefinition.getDataReaderEndpoint());
        assertEquals("my-pas-service", domainEventReferenceDefinition.getOauthClientId());
    }

    @Test
    void correlationProviderHasBeenInstantiated() {
        Optional<DomainEventArchiveConfiguration> eventWithCorrelationProvider = jsonDomainEventArchiveConfigurationRepository.findByName("eventWithCorrelationProvider");
        assertTrue(eventWithCorrelationProvider.isPresent());
        assertSame(TestCorrelationProvider.class, eventWithCorrelationProvider.get().getCorrelationProvider().getClass());
    }
}
