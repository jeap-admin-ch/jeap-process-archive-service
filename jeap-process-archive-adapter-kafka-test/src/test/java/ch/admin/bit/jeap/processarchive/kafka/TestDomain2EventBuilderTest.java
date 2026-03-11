package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestDomain2EventBuilderTest {

    @Test
    void build_defaultValues() {
        TestDomain2Event event = TestDomain2EventBuilder.builder()
                .idempotenceId("idempotence-1")
                .build();

        assertNotNull(event.getIdentity());
        assertEquals("TestDomain2Event", event.getType().getName());
        assertEquals("1.0.0", event.getType().getVersion());
        assertEquals("test", event.getPublisher().getSystem());
        assertEquals("test", event.getPublisher().getService());
        assertEquals("test", event.getProcessId());
        assertNotNull(event.getReferences());
        assertNotNull(event.getPayload());
        assertEquals("na", event.getPayload().getData());
    }

    @Test
    void build_customPayloadData() {
        TestDomain2Event event = TestDomain2EventBuilder.builder()
                .idempotenceId("idempotence-2")
                .payloadData("custom-data")
                .build();

        assertEquals("custom-data", event.getPayload().getData());
    }
}
