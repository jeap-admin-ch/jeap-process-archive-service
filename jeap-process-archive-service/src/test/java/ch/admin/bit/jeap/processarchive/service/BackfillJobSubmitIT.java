package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillTaskEntity;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobResult;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillJobState;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillTaskState;
import ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.test.processarchive.TestConfig;
import ch.admin.bit.jeap.test.processarchive.TestConsumer;
import ch.admin.bit.jeap.test.processarchive.TestTypeLoaderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost/unused/{id}?version={version}",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test",
        "jeap.messaging.kafka.publish-without-contract-allowed=true",
        "jeap.messaging.kafka.consume-without-contract-allowed=true",
        "jeap.processarchive.backfill.enabled=true",
        "jeap.processarchive.backfill.topic=jeap-process-archive-createartifact",
})
@AutoConfigureMockMvc
@Import({HashProviderTestConfig.class, TestConfig.class, TestTypeLoaderConfig.class, PostgresTestContainerBase.class})
class BackfillJobSubmitIT extends KafkaIntegrationTestBase {

    private static final UUID JOB_ID = UUID.fromString("2ad48b66-472b-4a83-8efe-66a5f53ca111");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BackfillJobRepository backfillJobRepository;
    @Autowired
    private TestConsumer testConsumer;
    @MockitoBean
    private KeyReferenceCryptoService keyReferenceCryptoService;
    @MockitoBean
    private KeyIdCryptoService keyIdCryptoService;

    @Test
    void putBackfillJob_persistsJobAndTasksAndPublishesCreateArtifactCommands() throws Exception {
        String request = """
                message: TestDomainEvent
                topic: test-event-1
                archiveDataReferences:
                  - id: DOC-2024-001
                    version: 1
                  - id: DOC-2024-002
                    version: 2
                """;

        mockMvc.perform(put("/api/jobs/{jobId}", JOB_ID)
                        .contentType("application/yaml")
                        .content(request)
                        .with(authentication(createAuthenticationForUserRoles()))
                        .with(csrf()))
                .andExpect(status().isOk());

        BackfillJobEntity job = backfillJobRepository.findWithTasksByJobId(JOB_ID).orElseThrow();
        assertThat(job.getMessageName()).isEqualTo("TestDomainEvent");
        assertThat(job.getTopicName()).isEqualTo("test-event-1");
        assertThat(job.getJobState()).isEqualTo(BackfillJobState.OPEN);
        assertThat(job.getJobResult()).isNull();
        assertThat(job.getTasks())
                .extracting(BackfillTaskEntity::getReferenceId, BackfillTaskEntity::getReferenceVersion, BackfillTaskEntity::getTaskState)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("DOC-2024-001", 1, BackfillTaskState.OPEN),
                        org.assertj.core.groups.Tuple.tuple("DOC-2024-002", 2, BackfillTaskState.OPEN));

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(testConsumer.getCreateArtifactCommands()).hasSize(2));
        assertThat(testConsumer.getCreateArtifactCommands())
                .extracting(command -> command.getReferences().getArchiveData().getReferenceId(),
                        command -> command.getReferences().getArchiveData().getReferenceVersion())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("DOC-2024-001", 1),
                        org.assertj.core.groups.Tuple.tuple("DOC-2024-002", 2));
    }

    @Test
    void getReport_afterPersistedJobCompletion_returnsYamlReportWithTaskStates() throws Exception {
        UUID jobId = UUID.randomUUID();
        String request = """
                message: TestDomainEvent
                topic: test-event-1
                archiveDataReferences:
                  - id: DOC-2024-003
                    version: 1
                  - id: DOC-2024-004
                    version: 2
                """;

        mockMvc.perform(put("/api/jobs/{jobId}", jobId)
                        .contentType("application/yaml")
                        .content(request)
                        .with(authentication(createAuthenticationForUserRoles()))
                        .with(csrf()))
                .andExpect(status().isOk());

        BackfillJobEntity job = backfillJobRepository.findWithTasksByJobId(jobId).orElseThrow();
        job.setJobState(BackfillJobState.COMPLETED);
        job.setJobResult(BackfillJobResult.PARTIALLY_SUCCEEDED);
        job.setReportCreatedAt(Instant.parse("2026-05-08T07:30:15.456Z"));
        job.setStartedByName("John Doe");
        job.setStartedByExtId("287365");
        job.getTasks().sort(Comparator.comparing(BackfillTaskEntity::getReferenceId));
        job.getTasks().get(0).setTaskState(BackfillTaskState.SUCCEEDED);
        job.getTasks().get(1).setTaskState(BackfillTaskState.FAILED);
        job.getTasks().get(1).setErrorMessage("Failed reading artifact from source service");
        job.getTasks().get(1).setErrorTraceId("4bf92f3577b34da6a3ce929d0e0e4736");
        backfillJobRepository.saveAndFlush(job);

        mockMvc.perform(get("/api/jobs/{jobId}/report", jobId)
                        .accept("application/yaml")
                        .with(authentication(createAuthenticationForUserRoles(readRole()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/yaml"))
                .andExpect(content().string(containsString("message: TestDomainEvent")))
                .andExpect(content().string(containsString("topic: test-event-1")))
                .andExpect(content().string(containsString("job-state: completed")))
                .andExpect(content().string(containsString("job-result: partially-succeeded")))
                .andExpect(content().string(containsString("job-id: " + jobId + "")))
                .andExpect(content().string(containsString("started:")))
                .andExpect(content().string(containsString("report-created: 2026-05-08T07:30:15.456Z")))
                .andExpect(content().string(containsString("started-by-name: John Doe")))
                .andExpect(content().string(containsString("started-by-ext_id: 287365")))
                .andExpect(content().string(containsString("id: DOC-2024-003")))
                .andExpect(content().string(containsString("state: succeeded")))
                .andExpect(content().string(containsString("id: DOC-2024-004")))
                .andExpect(content().string(containsString("state: failed")))
                .andExpect(content().string(containsString("message: Failed reading artifact from source service")))
                .andExpect(content().string(containsString("traceId: 4bf92f3577b34da6a3ce929d0e0e4736")));
    }

    private JeapAuthenticationToken createAuthenticationForUserRoles() {
        SemanticApplicationRole writeRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("backfilljob")
                .operation("write")
                .build();
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(writeRole).build();
    }

    private JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole role) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(role).build();
    }

    private SemanticApplicationRole readRole() {
        return SemanticApplicationRole.builder()
                .system("jme")
                .resource("backfilljob")
                .operation("read")
                .build();
    }
}
