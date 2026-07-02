package ch.admin.bit.jeap.processarchive.configonly;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.kafka.KafkaMessageConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain2EventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.service.ProcessArchiveApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Integration test verifying that the PAS works with only config-based archive types,
 * without any classpath archive type provider (no Avro archive type descriptors).
 */
@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class)
@Import(HashProviderTestConfig.class)
class ConfigOnlyArchiveTypesIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-2";
    private static final String PAYLOAD_DATA = "config-only-payload";
    private static final String EVENT_IDEMPOTENCE_ID = UUID.randomUUID().toString();

    @Autowired
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Autowired
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;
    @MockitoBean
    private ArtifactArchivedListener artifactArchivedListener;
    private final ArgumentCaptor<ArchivedArtifact> archivedArtifactArgumentCaptor = ArgumentCaptor.forClass(ArchivedArtifact.class);
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void when_eventReceived_withConfigOnlyTypes_then_shouldArchiveData() throws Exception {
        // given
        TestDomain2Event testDomainEvent = TestDomain2EventBuilder.builder()
                .idempotenceId(EVENT_IDEMPOTENCE_ID)
                .payloadData(PAYLOAD_DATA)
                .build();

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertThat(archivedArtifact.getProcessId()).isEqualTo(testDomainEvent.getProcessId());
        assertThat(archivedArtifact.getIdempotenceId()).isEqualTo("TestDomain2Event_" + EVENT_IDEMPOTENCE_ID
                + "_TestSystem_ConfigOnlyType_" + archivedArtifact.getArchiveData().getReferenceId());
        assertThat(archivedArtifact.getReferenceIdType()).isEqualTo("ch.admin.bit.jeap.test.ConfigOnlyArtifact");
        assertThat(archivedArtifact.getExpirationDays()).isEqualTo(45);
        assertThat(archivedArtifact.getArchiveData()).isNotNull();
        assertThat(archivedArtifact.getArchiveData().getContentType()).isEqualTo("application/octet-stream");
    }

    @BeforeEach
    void setUp() {
        kafkaMessageConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
