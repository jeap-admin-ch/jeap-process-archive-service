package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.event.test3.TestDomain3Event;
import ch.admin.bit.jeap.processcontext.event.test3.TestDomain3EventPayload;
import ch.admin.bit.jeap.processcontext.event.test3.TestDomain3EventReferences;

public class TestDomain3EventBuilder extends AvroDomainEventBuilder<TestDomain3EventBuilder, TestDomain3Event> {

    private String payloadData = "na";
    private String otherCustomId;

    private TestDomain3EventBuilder() {
        super(TestDomain3Event::new);
    }

    public TestDomain3EventBuilder payloadData(String payloadData, String otherCustomId) {
        this.payloadData = payloadData;
        this.otherCustomId = otherCustomId;
        return this;
    }

    public static TestDomain3EventBuilder builder() {
        return new TestDomain3EventBuilder();
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
    protected TestDomain3EventBuilder self() {
        return this;
    }

    @Override
    public TestDomain3Event build() {
        setReferences(new TestDomain3EventReferences());
        setPayload(TestDomain3EventPayload.newBuilder()
                .setData(payloadData)
                .setOtherCustomId(otherCustomId)
                .build());
        setProcessId("test");
        return super.build();
    }
}
