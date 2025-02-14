package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.kafka.KafkaDomainEventConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain2EventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.test.decree.v3.Decree;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2Event;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
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
@ContextConfiguration(classes = {HashProviderTestConfig.class})
class ArchiveServicePayloadDataExtractorIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-2";
    private static final String PAYLOAD_DATA = "payload-data";
    private static final String EVENT_IDEMPOTENCE_ID = UUID.randomUUID().toString();
    private static final String ARCHIVE_IDEMPOTENCE_ID = "TestDomain2Event_" + EVENT_IDEMPOTENCE_ID;

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;
    @MockitoBean
    private ArtifactArchivedListener artifactArchivedListener;
    @Captor
    private ArgumentCaptor<ArchivedArtifact> archivedArtifactArgumentCaptor;

    @Test
    void when_eventReceived_then_shouldArchiveDataRetrievedFromDomainEvent() throws Exception {
        // given
        TestDomain2Event testDomainEvent = createTestEvent();

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertEquals(testDomainEvent.getProcessId(), archivedArtifact.getProcessId());
        Decree decree = deserialize(archivedArtifact.getArchiveData().getPayload());
        assertEquals(PAYLOAD_DATA, decree.getPayload());
        assertEquals(ARCHIVE_IDEMPOTENCE_ID, archivedArtifact.getIdempotenceId());
    }

    private Decree deserialize(byte[] payload) throws IOException {
        DatumReader<Decree> datumReader = new SpecificDatumReader<>(Decree.class);
        Decoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
        return datumReader.read(null, decoder);
    }

    private TestDomain2Event createTestEvent() {
        return TestDomain2EventBuilder.builder()
                .idempotenceId(EVENT_IDEMPOTENCE_ID)
                .payloadData(PAYLOAD_DATA)
                .build();
    }

    @BeforeEach
    void setUp() {
        kafkaDomainEventConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
