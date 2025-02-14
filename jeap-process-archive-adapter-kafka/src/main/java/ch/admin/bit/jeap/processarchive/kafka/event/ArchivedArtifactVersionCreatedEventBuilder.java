package ch.admin.bit.jeap.processarchive.kafka.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.event.shared.processarchive.archivedartifactversioncreated.*;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ArchivedArtifactVersionCreatedEventBuilder extends AvroDomainEventBuilder<ArchivedArtifactVersionCreatedEventBuilder, SharedArchivedArtifactVersionCreatedEvent> {

    private String serviceName;
    private String systemName;
    private ArchivedArtifact archivedArtifact;
    private String referenceIdType;
    private int expirationDays;

    public static ArchivedArtifactVersionCreatedEventBuilder builder() {
        return new ArchivedArtifactVersionCreatedEventBuilder();
    }

    private ArchivedArtifactVersionCreatedEventBuilder() {
        super(SharedArchivedArtifactVersionCreatedEvent::new);
    }

    @Override
    public SharedArchivedArtifactVersionCreatedEvent build() {
        setReferences(buildReferences());
        setPayload(buildPayload());
        return super.build();
    }

    private MessageReferences buildReferences() {
        String referenceIdHash = Hashes.hashReferenceId(archivedArtifact.getArchiveData().getReferenceId(), archivedArtifact.getReferenceIdType());
        String referenceIdTypeHash = Hashes.hashReferenceIdType(archivedArtifact.getReferenceIdType());
        return ArchivedArtifactVersionReferences.newBuilder()
                .setArchivedArtifact(ArchivedArtifactReference.newBuilder()
                        .setReferenceId(archivedArtifact.getArchiveData().getReferenceId())
                        .setVersion(Optional.ofNullable(archivedArtifact.getArchiveData().getVersion()).map(Object::toString).orElse(null))
                        .setReferenceIdHash(referenceIdHash)
                        .setReferenceIdType(referenceIdType)
                        .setReferenceIdTypeHash(referenceIdTypeHash)
                        .build())
                .setArchivedArtifactType(ArchivedArtifactTypeReference.newBuilder()
                        .setContentType(archivedArtifact.getArchiveData().getContentType())
                        .setSystem(archivedArtifact.getArchiveData().getSystem())
                        .setDataSchemaType(archivedArtifact.getArchiveData().getSchema())
                        .setDataSchemaVersion(archivedArtifact.getArchiveData().getSchemaVersion())
                        .build())
                .setStorageObject(StorageObjectReference.newBuilder()
                        .setStorageObjectBucket(archivedArtifact.getStorageObjectBucket())
                        .setStorageObjectKey(archivedArtifact.getStorageObjectKey())
                        .setStorageObjectVersionId(archivedArtifact.getStorageObjectVersionId())
                        .build())
                .build();
    }

    @Override
    protected String getServiceName() {
        return serviceName;
    }

    @Override
    protected String getSystemName() {
        return systemName;
    }

    @Override
    protected ArchivedArtifactVersionCreatedEventBuilder self() {
        return this;
    }

    public ArchivedArtifactVersionCreatedEventBuilder sender(String systemName, String serviceName) {
        this.systemName = systemName;
        this.serviceName = serviceName;
        return this;
    }

    public ArchivedArtifactVersionCreatedEventBuilder archivedArtifact(ArchivedArtifact archivedArtifact) {
        setProcessId(archivedArtifact.getProcessId());
        this.idempotenceId = archivedArtifact.getIdempotenceId() + "-event";
        this.archivedArtifact = archivedArtifact;
        this.expirationDays = archivedArtifact.getExpirationDays();
        return this;
    }

    public ArchivedArtifactVersionCreatedEventBuilder referenceIdType(String referenceIdType) {
        this.referenceIdType = referenceIdType;
        return this;
    }

    private MessagePayload buildPayload() {
        return ArchivedArtifactVersionCreatedPayload.newBuilder()
                .setMetadata(buildMetadata())
                .setExpiration(Expiration.newBuilder()
                        .setDays(expirationDays)
                        .build())
                .build();
    }

    private List<ArchivedArtifactMetadata> buildMetadata() {
        return archivedArtifact.getArchiveData().getMetadata().stream()
                .map(metadata -> ArchivedArtifactMetadata.newBuilder()
                        .setName(metadata.getName())
                        .setValue(metadata.getValue())
                        .build())
                .collect(toList());
    }


    public static AvroMessageType messageType() {
        AvroMessageType type = new AvroMessageType();
        type.setName(SharedArchivedArtifactVersionCreatedEvent.getClassSchema().getName());
        type.setVersion(SharedArchivedArtifactVersionCreatedEvent.MESSAGE_TYPE_VERSION$);
        return type;
    }
}
