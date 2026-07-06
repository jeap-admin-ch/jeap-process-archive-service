package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveArtifactIdempotenceId;
import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.kafka.KafkaMessageConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestDomain2EventBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.service.test.TestDomain2RemoteReferenceProviderA;
import ch.admin.bit.jeap.processarchive.service.test.TestDomain2RemoteReferenceProviderB;
import ch.admin.bit.jeap.processarchive.service.test.TestSecondArtifactDataProvider;
import ch.admin.bit.jeap.processarchive.test.DecreeReference;
import ch.admin.bit.jeap.processarchive.test.decree.v2.Decree;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
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
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Verifies that registering multiple archive configurations for the same message (via a dedicated
 * {@code messages-multi-artifact.json} loaded through the {@code jeap.processarchive.configuration.location} property)
 * archives multiple artifacts - covering both the payload-data path (two configurations) and the remote-data path
 * (two configurations) - and that each artifact receives a distinct, discriminated idempotence id.
 */
@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost:12333/testdata/{id}",
        "jeap.processarchive.configuration.location=classpath:/processarchive/messages-multi-artifact.json",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test"})
@Import({HashProviderTestConfig.class, TestTypeLoaderConfig.class, PostgresTestContainerBase.class})
@EnableWireMock(@ConfigureWireMock(port = 12333))
class ArchiveServiceMultipleArtifactsIT extends KafkaIntegrationTestBase {

    private static final String DOMAIN_EVENT_TOPIC = "test-event-2";
    private static final String PAYLOAD_DATA = "payload-data";
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
    void when_multipleConfigurationsRegistered_then_archivesMultipleDistinctArtifacts() throws Exception {
        // given: one message, four configurations (two payload-data, two remote-data)
        TestDomain2Event testDomainEvent = TestDomain2EventBuilder.builder()
                .idempotenceId(EVENT_IDEMPOTENCE_ID)
                .payloadData(PAYLOAD_DATA)
                .build();
        String eventId = testDomainEvent.getIdentity().getEventId();
        stubRemoteArtifact(eventId + TestDomain2RemoteReferenceProviderA.REFERENCE_ID_SUFFIX);
        stubRemoteArtifact(eventId + TestDomain2RemoteReferenceProviderB.REFERENCE_ID_SUFFIX);

        // when
        kafkaTemplate.send(DOMAIN_EVENT_TOPIC, testDomainEvent).get();

        // then: all four configurations produce an artifact
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT).times(4))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        List<ArchivedArtifact> archivedArtifacts = archivedArtifactArgumentCaptor.getAllValues();
        String referenceIdPayload1 = eventId;
        String referenceIdPayload2 = eventId + TestSecondArtifactDataProvider.REFERENCE_ID_SUFFIX;
        String referenceIdRemoteA = eventId + TestDomain2RemoteReferenceProviderA.REFERENCE_ID_SUFFIX;
        String referenceIdRemoteB = eventId + TestDomain2RemoteReferenceProviderB.REFERENCE_ID_SUFFIX;

        assertThat(archivedArtifacts).extracting(artifact -> artifact.getArchiveData().getReferenceId())
                .containsExactlyInAnyOrder(referenceIdPayload1, referenceIdPayload2, referenceIdRemoteA, referenceIdRemoteB);
        assertThat(archivedArtifacts).extracting(ArchivedArtifact::getIdempotenceId)
                .containsExactlyInAnyOrder(
                        idempotenceId(referenceIdPayload1),
                        idempotenceId(referenceIdPayload2),
                        idempotenceId(referenceIdRemoteA),
                        idempotenceId(referenceIdRemoteB));
    }

    private String idempotenceId(String referenceId) {
        return ArchiveArtifactIdempotenceId.create("TestDomain2Event", EVENT_IDEMPOTENCE_ID,
                "JME", "Decree", referenceId, null);
    }

    private void stubRemoteArtifact(String referenceId) throws IOException {
        stubFor(get(urlEqualTo("/testdata/" + referenceId)).willReturn(aResponse()
                .withHeader("Content-Type", "avro/binary")
                .withHeader("Archive-Data-System", "JME")
                .withHeader("Archive-Data-Schema", "Decree")
                .withHeader("Archive-Data-Schema-Version", "1")
                .withBody(createPayload())));
    }

    private byte[] createPayload() throws IOException {
        SpecificRecord data = Decree.newBuilder()
                .setPayload(PAYLOAD_DATA)
                .setDecreeReference(DecreeReference.newBuilder()
                        .setType("decree-id")
                        .setId("789")
                        .build())
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
        datumWriter.write(data, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }

    @BeforeEach
    void setUp() {
        kafkaMessageConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
