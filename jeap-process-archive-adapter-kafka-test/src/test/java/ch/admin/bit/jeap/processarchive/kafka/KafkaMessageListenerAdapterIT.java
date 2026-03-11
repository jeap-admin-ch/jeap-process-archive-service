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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "jeap.messaging.kafka.error-topic-name=error",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test"
})
@ExtendWith(MockitoExtension.class)
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
    void receiveDomainEvent() throws Exception {
        TestDomainEvent testDomainEvent = createTestEvent();
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        verify(messageReceiver, timeout(TEST_TIMEOUT)).messageReceived(testDomainEvent);
    }

    private TestDomainEvent createTestEvent() {
        return TestDomainEventBuilder.builder()
                .idempotenceId("test")
                .build();
    }
}
