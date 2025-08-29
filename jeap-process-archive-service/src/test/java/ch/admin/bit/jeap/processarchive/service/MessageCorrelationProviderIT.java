package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.kafka.KafkaDomainEventConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain3EventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processcontext.event.test3.TestDomain3Event;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost/unused",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test"})
@Import({HashProviderTestConfig.class, TestTypeLoaderConfig.class})
class MessageCorrelationProviderIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-3";
    private static final String PAYLOAD_DATA = "payload-data";
    private static final String EVENT_IDEMPOTENCE_ID = UUID.randomUUID().toString();

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;
    @MockitoBean
    private ArtifactArchivedListener artifactArchivedListener;
    @Captor
    private ArgumentCaptor<ArchivedArtifact> archivedArtifactArgumentCaptor;
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void when_eventReceived_then_shouldArchiveDataRetrievedFromDomainEvent() throws Exception {
        // given
        TestDomain3Event testDomainEvent = createTestEvent();

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT)).onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertEquals(testDomainEvent.getPayload().getOtherCustomId(), archivedArtifact.getProcessId());
    }

    private TestDomain3Event createTestEvent() {
        return TestDomain3EventBuilder.builder()
                .idempotenceId(EVENT_IDEMPOTENCE_ID)
                .payloadData(PAYLOAD_DATA, UUID.randomUUID().toString())
                .build();
    }

    @BeforeEach
    void setUp() {
        kafkaDomainEventConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
