package ch.admin.bit.jeap.processarchive.service;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobEntity;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillJobRepository;
import ch.admin.bit.jeap.processarchive.adapter.db.BackfillTaskEntity;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles(ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(classes = ProcessArchiveApplication.class, properties = {
        "spring.cloud.vault.enabled=false",
        "test.service.uri=http://localhost/unused/{id}",
        "jeap.processarchive.objectstorage.storage.bucket=testbucket",
        "jeap.processarchive.archivedartifact.event-topic=event-topic",
        "jeap.processarchive.archivedartifact.system-id=com.test.System",
        "jeap.processarchive.archivedartifact.system-name=test",
        "jeap.processarchive.backfill.command.topic=backfill-create-artifact-command",
        "jeap.messaging.kafka.publish-without-contract-allowed=true",
        "jeap.messaging.kafka.consume-without-contract-allowed=true"})
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

    private JeapAuthenticationToken createAuthenticationForUserRoles() {
        SemanticApplicationRole writeRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("backfilljob")
                .operation("write")
                .build();
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(writeRole).build();
    }
}
