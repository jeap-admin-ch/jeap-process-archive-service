package ch.admin.bit.jeap.processarchive.kafka.event;

import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.SharedArchivedArtifactVersionCreatedEvent;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchivedArtifactVersionCreatedEventFactoryTest {

    private ArchivedArtifactVersionCreatedEventFactory factory;

    @BeforeEach
    void createFactory() {
        ArchivedArtifactEventProperties properties = new ArchivedArtifactEventProperties();
        properties.setServiceName("service");
        properties.setSystemName("system");
        factory = new ArchivedArtifactVersionCreatedEventFactory(properties);
    }

    @Test
    void createEvent() {
        String processId = "processId";
        String idempotenceId = "idempotenceId";
        String system = "system";
        String schema = "schema";
        String referenceIdHash = "017b56f3d6f105b27e4ed89c0fa3fa7dcc4b3e355d9db66c3b4b626deeb7fb01";
        String referenceIdType = "referenceIdType";
        String referenceIdTypeHash = "328490f6c80261cc93add400c191e5030c53f7d73c11e924bedabcbea75f56fb";
        int schemaVersion = 1;
        String referenceId = "referenceId";
        Integer dataVersion = 1;
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        String bucket = "bucket";
        String key = "key";
        String versionId = "versionId";
        String name = "name";
        String storageObjectId = "storageObjectId";
        String value = "value";
        int days = 5;
        String contentType = "application/pdf";
        ArchivedArtifact archivedArtifact = ArchivedArtifact.builder()
                .referenceIdType(referenceIdType)
                .processId(processId)
                .idempotenceId(idempotenceId)
                .archiveData(ArchiveData.builder()
                        .payload(payload)
                        .contentType(contentType)
                        .system(system)
                        .schema(schema)
                        .schemaVersion(schemaVersion)
                        .referenceId(referenceId)
                        .version(dataVersion)
                        .storageBucket(Optional.of("bucket"))
                        .metadata(List.of(Metadata.of(name, value)))
                        .build())
                .storageObjectBucket(bucket)
                .storageObjectKey(key)
                .storageObjectVersionId(versionId)
                .storageObjectId(storageObjectId)
                .expirationDays(days)
                .build();

        SharedArchivedArtifactVersionCreatedEvent event = factory.createEvent(archivedArtifact);

        assertEquals(processId, event.getProcessId());
        assertEquals(idempotenceId + "-event", event.getIdentity().getIdempotenceId());
        assertEquals(contentType, event.getReferences().getArchivedArtifactType().getContentType());
        assertEquals(system, event.getReferences().getArchivedArtifactType().getSystem());
        assertEquals(schema, event.getReferences().getArchivedArtifactType().getDataSchemaType());
        assertEquals(referenceIdType, event.getReferences().getArchivedArtifact().getReferenceIdType());
        assertEquals(referenceIdTypeHash, event.getReferences().getArchivedArtifact().getReferenceIdTypeHash());
        assertEquals(schemaVersion, event.getReferences().getArchivedArtifactType().getDataSchemaVersion());
        assertEquals(days, event.getPayload().getExpiration().getDays());
        assertEquals(referenceId, event.getReferences().getArchivedArtifact().getReferenceId());
        assertEquals(referenceIdHash, event.getReferences().getArchivedArtifact().getReferenceIdHash());
        assertEquals(dataVersion, Integer.valueOf(event.getReferences().getArchivedArtifact().getVersion()));
        assertEquals(bucket, event.getReferences().getStorageObject().getStorageObjectBucket());
        assertEquals(key, event.getReferences().getStorageObject().getStorageObjectKey());
        assertEquals(versionId, event.getReferences().getStorageObject().getStorageObjectVersionId());
        assertEquals(1, event.getPayload().getMetadata().size());
        assertEquals(name, event.getPayload().getMetadata().get(0).getName());
        assertEquals(value, event.getPayload().getMetadata().get(0).getValue());
    }
}
