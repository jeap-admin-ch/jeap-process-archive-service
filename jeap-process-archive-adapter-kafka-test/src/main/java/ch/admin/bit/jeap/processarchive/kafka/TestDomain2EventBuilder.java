package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2EventPayload;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2EventReferences;

public class TestDomain2EventBuilder extends AvroDomainEventBuilder<TestDomain2EventBuilder, TestDomain2Event> {

    private String payloadData = "na";

    private TestDomain2EventBuilder() {
        super(TestDomain2Event::new);
    }

    public TestDomain2EventBuilder payloadData(String payloadData) {
        this.payloadData = payloadData;
        return this;
    }

    public static TestDomain2EventBuilder builder() {
        return new TestDomain2EventBuilder();
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
    protected TestDomain2EventBuilder self() {
        return this;
    }

    @Override
    public TestDomain2Event build() {
        setReferences(new TestDomain2EventReferences());
        setPayload(TestDomain2EventPayload.newBuilder()
                .setData(payloadData)
                .build());
        setProcessId("test");
        return super.build();
    }
}
