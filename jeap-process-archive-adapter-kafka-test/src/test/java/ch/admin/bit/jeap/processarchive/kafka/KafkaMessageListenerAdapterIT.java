package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveDataObjectStore;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import ch.admin.bit.jeap.processarchive.domain.event.MessageReceiver;
import ch.admin.bit.jeap.processarchive.event.test.TestDomainEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "jeap.messaging.kafka.error-topic-name=error",
        "jeap.messaging.kafka.service-name=process-archive-service",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test"
})
@EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.jpa.OutboxJpaConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.OutboxMessagingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.metrics.OutboxMetricsConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling.OutboxSchedulingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.transaction.OutboxTransactionConfig"
})
@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class KafkaMessageListenerAdapterIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-domain-event";

    @MockitoBean
    private MessageReceiver messageReceiver;
    @MockitoBean
    private MessageArchiveConfigurationRepository messageArchiveConfigurationRepository;
    @MockitoBean
    private RemoteArchiveDataProvider remoteArchiveDataProvider;
    @MockitoBean
    private ArchiveDataObjectStore archiveDataObjectStore;
    @Autowired
    private KafkaMessageListenerAdapter kafkaDomainEventListenerAdapter;
    @Autowired
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;
    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    @BeforeEach
    void setUp() {
        MessageArchiveConfiguration eventConfig = mock(MessageArchiveConfiguration.class);
        doReturn(TestDomainEvent.class.getSimpleName()).when(eventConfig).getMessageName();
        doReturn(DOMAIN_EVENT_TOPIC).when(eventConfig).getTopicName();
        kafkaDomainEventListenerAdapter.start(List.of(eventConfig));
        kafkaMessageConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }

    @Test
    void filterResubmittedMessageForOtherServiceAndContinueWithTargetedAndNormalMessages(CapturedOutput output)
            throws Exception {
        TestDomainEvent otherServiceEvent = createTestEvent("other-service-event");
        TestDomainEvent targetedEvent = createTestEvent("targeted-event");
        TestDomainEvent normalEvent = createTestEvent("normal-event");

        sendToPartition(otherServiceEvent, "other-service");
        sendToPartition(targetedEvent, "process-archive-service");
        sendToPartition(normalEvent, null);

        verify(messageReceiver, timeout(TEST_TIMEOUT)).messageReceived(targetedEvent);
        verify(messageReceiver, timeout(TEST_TIMEOUT)).messageReceived(normalEvent);
        verify(messageReceiver, never()).messageReceived(otherServiceEvent);
        verify(messageReceiver, times(2)).messageReceived(any());
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("Filtering out message because 'jeap_eh_target_service=other-service'");
    }

    private void sendToPartition(TestDomainEvent event, String targetService) throws Exception {
        ProducerRecord<AvroMessageKey, AvroMessage> record =
                new ProducerRecord<>(DOMAIN_EVENT_TOPIC, 0, null, event);
        if (targetService != null) {
            record.headers().add("jeap_eh_target_service", targetService.getBytes(StandardCharsets.UTF_8));
            record.headers().add("jeap_eh_error_handling_service",
                    "test-error-handling-service".getBytes(StandardCharsets.UTF_8));
        }
        kafkaTemplate.send(record).get();
    }

    private TestDomainEvent createTestEvent(String idempotenceId) {
        return TestDomainEventBuilder.builder()
                .idempotenceId(idempotenceId)
                .build();
    }
}
