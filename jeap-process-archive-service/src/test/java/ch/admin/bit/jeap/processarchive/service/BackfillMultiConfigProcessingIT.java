package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
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

/**
 * Verifies that a backfill job targets a specific archive configuration via {@code config-id} when a message has
 * multiple remote-data configurations, and that a submission without a config-id is rejected as ambiguous.
 */
@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "jeap.processarchive.configuration.location=classpath:/processarchive/messages-backfill-multi.json",
        "jeap.processarchive.consumer-contract-validator.enabled=false",
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
@EnableWireMock(@ConfigureWireMock(port = 12334))
class BackfillMultiConfigProcessingIT extends KafkaIntegrationTestBase {

    private static final UUID JOB_ID = UUID.fromString("6b1f8f2c-1a2b-4c3d-9e5f-0a1b2c3d4e5f");
    private static final UUID AMBIGUOUS_JOB_ID = UUID.fromString("7c2f9f3d-2b3c-5d4e-af60-1b2c3d4e5f60");
    private static final String REFERENCE_ID = "DOC-1";
    private static final int REFERENCE_VERSION = 1;

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
    void putBackfillJob_withConfigId_targetsSelectedConfigurationAndArchives() throws Exception {
        stubArchiveData("/a/" + REFERENCE_ID + "/" + REFERENCE_VERSION);

        String request = """
                message: TestDomain4Event
                config-id: config-a
                archiveDataReferences:
                  - id: DOC-1
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
        assertThat(event.getReferences().getArchivedArtifact().getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(event.getReferences().getArchivedArtifactType().getSystem()).isEqualTo("JME");
        assertThat(event.getReferences().getArchivedArtifactType().getDataSchemaType()).isEqualTo("Decree");

        // Only the selected configuration's endpoint (config-a) was called
        verify(getRequestedFor(urlEqualTo("/a/" + REFERENCE_ID + "/" + REFERENCE_VERSION)));
        verify(0, getRequestedFor(urlPathMatching("/b/.*")));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            BackfillJobEntity job = backfillJobRepository.findWithTasksByJobId(JOB_ID).orElseThrow();
            assertThat(job.getConfigId()).isEqualTo("config-a");
            assertThat(job.getJobState()).isEqualTo(BackfillJobState.COMPLETED);
            assertThat(job.getJobResult()).isEqualTo(BackfillJobResult.SUCCEEDED);
        });
    }

    @Test
    void putBackfillJob_withoutConfigId_isRejectedAsAmbiguous() throws Exception {
        String request = """
                message: TestDomain4Event
                archiveDataReferences:
                  - id: DOC-1
                    version: 1
                """;

        mockMvc.perform(put("/api/jobs/{jobId}", AMBIGUOUS_JOB_ID)
                        .contentType("application/yaml")
                        .content(request)
                        .with(authentication(createAuthenticationForUserRoles()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        assertThat(backfillJobRepository.findWithTasksByJobId(AMBIGUOUS_JOB_ID)).isEmpty();
    }

    private void stubArchiveData(String path) throws IOException {
        stubFor(get(urlEqualTo(path)).willReturn(aResponse()
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
