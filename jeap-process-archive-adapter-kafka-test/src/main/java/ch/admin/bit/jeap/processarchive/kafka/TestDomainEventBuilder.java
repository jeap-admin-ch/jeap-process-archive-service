package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.processcontext.event.test.DataReference;
import ch.admin.bit.jeap.processcontext.event.test.TestDomainEvent;
import ch.admin.bit.jeap.processcontext.event.test.TestDomainEventReferences;

public class TestDomainEventBuilder extends AvroDomainEventBuilder<TestDomainEventBuilder, TestDomainEvent> {

    private String dataId = "na";

    private TestDomainEventBuilder() {
        super(TestDomainEvent::new);
    }

    public TestDomainEventBuilder dataId(String dataId) {
        this.dataId = dataId;
        return this;
    }

    public static TestDomainEventBuilder builder() {
        return new TestDomainEventBuilder();
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
    protected TestDomainEventBuilder self() {
        return this;
    }

    @Override
    public TestDomainEvent build() {
        TestDomainEventReferences references = new TestDomainEventReferences();
        references.setDataReference(DataReference.newBuilder()
                .setDataId(dataId)
                .build());
        setReferences(references);
        setProcessId("test");
        return super.build();
    }
}
