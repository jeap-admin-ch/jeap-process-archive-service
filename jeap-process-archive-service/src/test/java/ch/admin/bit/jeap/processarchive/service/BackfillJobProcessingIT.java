package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillTaskEntity;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTaskState;
import ch.admin.bit.jeap.processarchive.kafka.KafkaMessageConsumerFactory;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.processarchive.test.DecreeReference;
import ch.admin.bit.jeap.processarchive.test.decree.v2.Decree;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.test.processarchive.TestConfig;
import ch.admin.bit.jeap.test.processarchive.TestConsumer;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost:12333/testdata/{id}/{version}",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test",
        "jeap.messaging.kafka.publish-without-contract-allowed=true",
        "jeap.messaging.kafka.consume-without-contract-allowed=true",
        "jeap.messaging.kafka.exposeMessageKeyToConsumer=true",
        "jeap.processarchive.backfill.enabled=true",
        "jeap.processarchive.backfill.topic=jeap-process-archive-createartifact",
        "jeap.processarchive.backfill.consumer-group=jeap-process-archive",
})
@AutoConfigureMockMvc
@Import({HashProviderTestConfig.class, TestConfig.class, TestTypeLoaderConfig.class, PostgresTestContainerBase.class})
@EnableWireMock(@ConfigureWireMock(port = 12333))
class BackfillJobProcessingIT extends KafkaIntegrationTestBase {

    private static final UUID JOB_ID = UUID.fromString("e5fc0888-92aa-42af-9ba5-b987e89948a1");
    private static final String REFERENCE_ID = "DOC-2024-7121";
    private static final int REFERENCE_VERSION = 1;
    private static final UUID ERROR_JOB_ID = UUID.fromString("aa603899-e70a-45a7-a5a5-b8cbadf6a721");
    private static final String ERROR_REFERENCE_ID = "DOC-2024-ERROR";
    private static final int ERROR_REFERENCE_VERSION = 1;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BackfillJobRepository backfillJobRepository;
    @Autowired
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;
    @Autowired
    private TestConsumer testConsumer;
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void putBackfillJob_consumesCreateArtifactCommandAndCompletesJob() throws Exception {
        stubRemoteArchiveData();

        String request = """
                message: TestDomainEvent
                archiveDataReferences:
                  - id: DOC-2024-7121
                    version: 1
                """;

        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType("application/yaml")
                        .content(request)
                        .with(authentication(createAuthenticationForUserRoles()))
                        .with(csrf()))
                .andExpect(status().isOk());

        String eventIdempotenceId = "CreateArtifactCommand_%s-%d:%s:v%d-event"
                .formatted(JOB_ID, REFERENCE_ID.length(), REFERENCE_ID, REFERENCE_VERSION);
        await().atMost(Duration.ofSeconds(30))
                .until(() -> testConsumer.eventWithIdempotenceIdReceived(eventIdempotenceId));

        SharedArchivedArtifactVersionCreatedEvent event = testConsumer.getEventByIdempotenceId(eventIdempotenceId);
        assertThat(event.getProcessId()).isEqualTo(JOB_ID.toString());
        assertThat(event.getReferences().getArchivedArtifact().getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(event.getReferences().getArchivedArtifact().getVersion()).isEqualTo(String.valueOf(REFERENCE_VERSION));
        assertThat(event.getReferences().getArchivedArtifactType().getContentType()).isEqualTo("avro/binary");
        assertThat(event.getReferences().getArchivedArtifactType().getSystem()).isEqualTo("JME");
        assertThat(event.getReferences().getArchivedArtifactType().getDataSchemaType()).isEqualTo("Decree");
        assertThat(event.getReferences().getArchivedArtifactType().getDataSchemaVersion()).isEqualTo(2);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            BackfillJobEntity job = backfillJobRepository.findWithTasksByJobId(JOB_ID).orElseThrow();
            assertThat(job.getJobState()).isEqualTo(BackfillJobState.COMPLETED);
            assertThat(job.getJobResult()).isEqualTo(BackfillJobResult.SUCCEEDED);
            assertThat(job.getReportCreatedAt()).isNotNull();
            assertThat(job.getTasks())
                    .extracting(BackfillTaskEntity::getReferenceId, BackfillTaskEntity::getReferenceVersion, BackfillTaskEntity::getTaskState)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(REFERENCE_ID, REFERENCE_VERSION, BackfillTaskState.SUCCEEDED));
        });
    }

    @Test
    void putBackfillJob_remoteArchiveDataErrorMarksTaskAndJobAsFailed() throws Exception {
        stubFor(get(urlEqualTo("/testdata/" + ERROR_REFERENCE_ID + "/" + ERROR_REFERENCE_VERSION)).willReturn(aResponse()
                .withStatus(500)
                .withBody("remote data failure")));

        String request = """
                message: TestDomainEvent
                archiveDataReferences:
                  - id: DOC-2024-ERROR
                    version: 1
                """;

        mockMvc.perform(put("/api/jobs/{jobId}", ERROR_JOB_ID)
                        .contentType("application/yaml")
                        .content(request)
                        .with(authentication(createAuthenticationForUserRoles()))
                        .with(csrf()))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            BackfillJobEntity job = backfillJobRepository.findWithTasksByJobId(ERROR_JOB_ID).orElseThrow();
            assertThat(job.getJobState()).isEqualTo(BackfillJobState.COMPLETED);
            assertThat(job.getJobResult()).isEqualTo(BackfillJobResult.FAILED);
            assertThat(job.getReportCreatedAt()).isNotNull();
            assertThat(job.getTasks())
                    .extracting(BackfillTaskEntity::getReferenceId, BackfillTaskEntity::getReferenceVersion,
                            BackfillTaskEntity::getTaskState, BackfillTaskEntity::getErrorMessage)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(ERROR_REFERENCE_ID, ERROR_REFERENCE_VERSION,
                            BackfillTaskState.FAILED, "Unable to read archive data for reference 'ArchiveDataReference(id=DOC-2024-ERROR, version=1)', HTTP status code '500 INTERNAL_SERVER_ERROR'"));
        });
    }

    private void stubRemoteArchiveData() throws IOException {
        stubFor(get(urlEqualTo("/testdata/" + REFERENCE_ID + "/" + REFERENCE_VERSION)).willReturn(aResponse()
                .withHeader("Content-Type", "avro/binary")
                .withHeader("Archive-Data-System", "JME")
                .withHeader("Archive-Data-Schema", "Decree")
                .withHeader("Archive-Data-Schema-Version", "2")
                .withBody(createPayload())));
    }

    private byte[] createPayload() throws IOException {
        SpecificRecord data = Decree.newBuilder()
                .setPayload("payload")
                .setDecreeReference(DecreeReference.newBuilder()
                        .setType("decree-id")
                        .setId(REFERENCE_ID)
                        .build())
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        DatumWriter<SpecificRecord> datumWriter = new SpecificDatumWriter<>(data.getSchema());
        datumWriter.write(data, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }

    private JeapAuthenticationToken createAuthenticationForUserRoles() {
        SemanticApplicationRole writeRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("backfilljob")
                .operation("write")
                .build();
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(writeRole).build();
    }

    @BeforeEach
    void setUp() {
        kafkaMessageConsumerFactory.getContainers().forEach(c -> ContainerTestUtils.waitForAssignment(c, 1));
    }
}
