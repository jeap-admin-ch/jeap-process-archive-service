package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.processarchive.command.test.TestCommand;
import ch.admin.bit.jeap.processarchive.command.test.TestCommandPayload;
import ch.admin.bit.jeap.processarchive.command.test.TestCommandReferences;

public class TestCommandBuilder extends AvroCommandBuilder<TestCommandBuilder, TestCommand> {

    private String message = "na";

    private TestCommandBuilder() {
        super(TestCommand::new);
    }

    public TestCommandBuilder message(String message) {
        this.message = message;
        return this;
    }

    public static TestCommandBuilder builder() {
        return new TestCommandBuilder();
    }

    @Override
    protected String getServiceName() {
        return "test";
    }

    @Override
    protected String getSystemName() {
        return "test";
    }

    @Override
    protected String getSpecifiedMessageTypeVersion() {
        return "1.0.0";
    }


    @Override
    protected TestCommandBuilder self() {
        return this;
    }

    @Override
    public TestCommand build() {
        setReferences(new TestCommandReferences());
        setPayload(TestCommandPayload.newBuilder()
                .setMessage(message)
                .build());
        setProcessId("test");
        return super.build();
    }
}
