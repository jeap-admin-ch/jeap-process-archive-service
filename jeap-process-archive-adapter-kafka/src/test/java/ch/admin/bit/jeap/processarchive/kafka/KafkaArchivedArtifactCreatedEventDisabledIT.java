package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.kafka.event.KafkaArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import ch.admin.bit.jeap.test.processarchive.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApp.class,
        properties = {
                "spring.application.name=test",
                "jeap.processarchive.archivedartifact.enabled=false",
                "jeap.messaging.kafka.system-name=system",
                "jeap.messaging.kafka.service-name=service",
                "jeap.messaging.kafka.exposeMessageKeyToConsumer=true"})
@EnableAutoConfiguration(excludeName = {
        "ch.admin.bit.jeap.processarchive.domain.DomainConfiguration",
        "ch.admin.bit.jeap.processarchive.configuration.json.JsonConfigurationRepositoryConfiguration"
})
class KafkaArchivedArtifactCreatedEventDisabledIT extends KafkaIntegrationTestBase {

    private static final String IDEMPOTENCE_ID = "idempotenceId";

    @Autowired
    private ArchivedArtifactCreatedEventProducer eventProducer;

    @Test
    void onArchivedArtifact() {
        // given
        ArchivedArtifact archivedArtifact = createArchivedArtifact();

        // when
        eventProducer.onArchivedArtifact(archivedArtifact);

        // then
        assertThat(eventProducer.getClass())
                .isNotInstanceOf(KafkaArchivedArtifactCreatedEventProducer.class);
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
