package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.kafka.event.KafkaArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import ch.admin.bit.jeap.test.processarchive.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=test",
                "spring.flyway.enabled=false",
                "jeap.processarchive.archivedartifact.event-topic=archived-artifact-version-created-event-topic",
                "jeap.processarchive.archivedartifact.system-id=ch.admin.jeap.System",
                "jeap.messaging.kafka.system-name=system",
                "jeap.messaging.kafka.service-name=service",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"})
@EnableAutoConfiguration(excludeName = {
        "ch.admin.bit.jeap.processarchive.domain.DomainConfiguration",
        "ch.admin.bit.jeap.processarchive.configuration.json.JsonConfigurationRepositoryConfiguration",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.jpa.OutboxJpaConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.messaging.OutboxMessagingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.metrics.OutboxMetricsConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.scheduling.OutboxSchedulingConfig",
        "ch.admin.bit.jeap.messaging.transactionaloutbox.transaction.OutboxTransactionConfig"
})
@JeapMessageProducerContract(value = SharedArchivedArtifactVersionCreatedEvent.TypeRef.class,
        topic = "archived-artifact-version-created-event-topic", appName = "test")
class KafkaArchivedArtifactCreatedEventProducerIT extends PostgresTestContainerBase {

    private static final String IDEMPOTENCE_ID = "idempotenceId";

    @Autowired
    private KafkaArchivedArtifactCreatedEventProducer eventProducer;

    @Autowired
    private TestConsumer testConsumer;

    @Test
    void onArchivedArtifact() {
        // given
        ArchivedArtifact archivedArtifact = createArchivedArtifact();

        // when
        eventProducer.onArchivedArtifact(archivedArtifact);

        // then
        waitAtMost(Duration.ofSeconds(30)).until(
                () -> !testConsumer.getEvents().isEmpty());
        assertEquals(IDEMPOTENCE_ID + "-event", testConsumer.getEvents().get(0).getIdentity().getIdempotenceId());
    }

    private ArchivedArtifact createArchivedArtifact() {
        return ArchivedArtifact.builder()
                .referenceIdType("referenceIdType")
                .processId("processId")
                .idempotenceId(IDEMPOTENCE_ID)
                .archiveData(ArchiveData.builder()
                        .payload("payload".getBytes(StandardCharsets.UTF_8))
                        .contentType("application/json")
                        .system("system")
                        .schema("schema")
                        .schemaVersion(1)
                        .referenceId("referenceId")
                        .version(1)
                        .metadata(List.of(Metadata.of("name", "value")))
                        .build())
                .storageObjectBucket("bucket")
                .storageObjectKey("key")
                .storageObjectVersionId("versionId")
                .storageObjectId("storageObjectId")
                .build();
    }
}
