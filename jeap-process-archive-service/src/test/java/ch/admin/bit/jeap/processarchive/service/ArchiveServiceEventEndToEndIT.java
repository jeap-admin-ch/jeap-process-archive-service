package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.kafka.KafkaDomainEventConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain2EventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.test.processarchive.TestConfig;
import ch.admin.bit.jeap.test.processarchive.TestConsumer;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost/unused",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test",
        "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"})
@Import({HashProviderTestConfig.class, TestConfig.class, TestTypeLoaderConfig.class})
class ArchiveServiceEventEndToEndIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-2";
    private static final String PAYLOAD_DATA = "payload-data";
    private static final String EVENT_IDEMPOTENCE_ID = UUID.randomUUID().toString();

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;
    @Autowired
    private TestConsumer testConsumer;
    @MockitoSpyBean
    private ArchiveDataObjectStore archiveDataObjectStore;
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void when_eventReceived_then_shouldProduceArchivedArtifactEvent() throws Exception {
        // given
        TestDomain2Event testDomainEvent = createTestEvent(PAYLOAD_DATA);

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then
        String idempotenceId = "TestDomain2Event_" + testDomainEvent.getIdentity().getIdempotenceId() + "-event";
        await().atMost(Duration.ofSeconds(30))
                .until(() -> testConsumer.eventWithIdempotenceIdReceived(idempotenceId));
        SharedArchivedArtifactVersionCreatedEvent event = testConsumer.getEventByIdempotenceId(idempotenceId);

        assertThat(event.getReferences().getArchivedArtifact().getReferenceId())
                .isEqualTo(testDomainEvent.getIdentity().getEventId());
    }

    @Test
    void when_eventReceived_then_shouldNotArchiveDataIfConditionIsNotMet() throws Exception {
        // given
        TestDomain2Event testDomainEvent = createTestEvent("ignored");

        // when: event received for which TestCondition evaluates to false
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then: no archive data must be stored
        verifyNoInteractions(archiveDataObjectStore);
    }

    private TestDomain2Event createTestEvent(String payloadData) {
        return TestDomain2EventBuilder.builder()
                .idempotenceId(EVENT_IDEMPOTENCE_ID)
                .payloadData(payloadData)
                .build();
    }

    @BeforeEach
    void setUp() {
        kafkaDomainEventConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
