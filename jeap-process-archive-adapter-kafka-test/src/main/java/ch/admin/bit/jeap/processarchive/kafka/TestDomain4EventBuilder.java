package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.event.test4.TestDomain4Event;
import ch.admin.bit.jeap.processcontext.event.test4.TestDomain4EventPayload;
import ch.admin.bit.jeap.processcontext.event.test4.TestDomain4EventReferences;

public class TestDomain4EventBuilder extends AvroDomainEventBuilder<TestDomain4EventBuilder, TestDomain4Event> {

    private String payloadData = "na";
    private String otherCustomId;

    private TestDomain4EventBuilder() {
        super(TestDomain4Event::new);
    }

    public TestDomain4EventBuilder payloadData(String payloadData, String otherCustomId) {
        this.payloadData = payloadData;
        this.otherCustomId = otherCustomId;
        return this;
    }

    public static TestDomain4EventBuilder builder() {
        return new TestDomain4EventBuilder();
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
    protected TestDomain4EventBuilder self() {
        return this;
    }

    @Override
    public TestDomain4Event build() {
        setReferences(new TestDomain4EventReferences());
        setPayload(TestDomain4EventPayload.newBuilder()
                .setData(payloadData)
                .setOtherCustomId(otherCustomId)
                .build());
        setProcessId("test");
        return super.build();
    }
}
