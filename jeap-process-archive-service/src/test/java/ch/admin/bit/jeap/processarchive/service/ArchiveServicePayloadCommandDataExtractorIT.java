package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.command.test.TestCommand;
import ch.admin.bit.jeap.processarchive.domain.archive.ArchiveArtifactIdempotenceId;
import ch.admin.bit.jeap.processarchive.kafka.KafkaMessageConsumerFactory;
import ch.admin.bit.jeap.processarchive.kafka.TestCommandBuilder;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.test.decree.v2.Decree;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
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
@Import({HashProviderTestConfig.class, TestTypeLoaderConfig.class, PostgresTestContainerBase.class})
class ArchiveServicePayloadCommandDataExtractorIT extends KafkaIntegrationTestBase {

    private static final String TOPIC = "test-command";
    private static final String MESSAGE = "test-message";
    private static final String IDEMPOTENCE_ID = UUID.randomUUID().toString();

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
    void when_commandReceived_then_shouldArchiveDataRetrievedFromCommand() throws Exception {
        // given
        TestCommand testCommand = createTestCommand();

        // when
        kafkaTemplate.send(TOPIC, testCommand).get();

        // then
        verify(artifactArchivedListener, timeout(TEST_TIMEOUT))
                .onArtifactArchived(archivedArtifactArgumentCaptor.capture());

        ArchivedArtifact archivedArtifact = archivedArtifactArgumentCaptor.getValue();
        assertEquals(testCommand.getProcessId(), archivedArtifact.getProcessId());
        Decree decree = deserialize(archivedArtifact.getArchiveData().getPayload());
        assertEquals(MESSAGE, decree.getPayload());
        assertEquals(ArchiveArtifactIdempotenceId.create("TestCommand", IDEMPOTENCE_ID,
                        "JME", "Decree", archivedArtifact.getArchiveData().getReferenceId(), null),
                archivedArtifact.getIdempotenceId());
    }

    private Decree deserialize(byte[] payload) throws IOException {
        DatumReader<Decree> datumReader = new SpecificDatumReader<>(Decree.class);
        Decoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
        return datumReader.read(null, decoder);
    }

    private TestCommand createTestCommand() {
        return TestCommandBuilder.builder()
                .idempotenceId(IDEMPOTENCE_ID)
                .message(MESSAGE)
                .build();
    }

    @BeforeEach
    void setUp() {
        kafkaMessageConsumerFactory.getContainers().forEach(c ->
                ContainerTestUtils.waitForAssignment(c, 1));
    }
}
