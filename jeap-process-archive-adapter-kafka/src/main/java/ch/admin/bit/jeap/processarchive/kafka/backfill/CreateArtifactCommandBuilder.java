package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.processarchive.command.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommand;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommandPayload;
import ch.admin.bit.jeap.processarchive.command.CreateArtifactCommandReferences;
import ch.admin.bit.jeap.processarchive.domain.backfill.CreateArtifactCommandData;

public class CreateArtifactCommandBuilder extends AvroCommandBuilder<CreateArtifactCommandBuilder, CreateArtifactCommand> {

    private static final String MESSAGE_TYPE_VERSION = "1.0.1";

    private final BackfillCommandProperties properties;
    private CreateArtifactCommandData commandData;

    private CreateArtifactCommandBuilder(BackfillCommandProperties properties) {
        super(CreateArtifactCommand::new);
        this.properties = properties;
    }

    public static CreateArtifactCommandBuilder builder(BackfillCommandProperties properties) {
        return new CreateArtifactCommandBuilder(properties);
    }

    public CreateArtifactCommandBuilder commandData(CreateArtifactCommandData commandData) {
        this.commandData = commandData;
        return this;
    }

    @Override
    public CreateArtifactCommand build() {
        idempotenceId = idempotenceId(commandData);
        setReferences(CreateArtifactCommandReferences.newBuilder()
                .setArchiveData(ArchiveDataReference.newBuilder()
                        .setReferenceId(commandData.referenceId())
                        .setReferenceVersion(commandData.referenceVersion())
                        .build())
                .build());
        setPayload(CreateArtifactCommandPayload.newBuilder()
                .setJobId(commandData.jobId().toString())
                .setMessageName(commandData.messageName())
                .setTopicName(commandData.topicName())
                .build());
        setProcessId(commandData.jobId().toString());
        return super.build();
    }

    private String idempotenceId(CreateArtifactCommandData commandData) {
        String referenceVersion = commandData.referenceVersion() == null ? "none" : "v" + commandData.referenceVersion();
        return "%s-%d:%s:%s".formatted(commandData.jobId(), commandData.referenceId().length(),
                commandData.referenceId(), referenceVersion);
    }

    @Override
    protected String getServiceName() {
        return properties.getServiceName();
    }

    @Override
    protected String getSystemName() {
        return properties.getSystemName();
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return MESSAGE_TYPE_VERSION;
    }

    @Override
    protected CreateArtifactCommandBuilder self() {
        return this;
    }

    public static AvroMessageType messageType() {
        AvroMessageType type = new AvroMessageType();
        type.setName(CreateArtifactCommand.getClassSchema().getName());
        type.setVersion(MESSAGE_TYPE_VERSION);
        return type;
    }
}
