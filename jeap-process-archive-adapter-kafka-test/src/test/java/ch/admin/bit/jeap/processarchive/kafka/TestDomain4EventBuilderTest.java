package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.event.test4.TestDomain4Event;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestDomain4EventBuilderTest {

    @Test
    void build_withPayloadData() {
        TestDomain4Event event = TestDomain4EventBuilder.builder()
                .idempotenceId("idempotence-1")
                .payloadData("data", "customId")
                .build();

        assertNotNull(event.getIdentity());
        assertEquals("TestDomain4Event", event.getType().getName());
        assertEquals("1.0.0", event.getType().getVersion());
        assertEquals("test", event.getPublisher().getSystem());
        assertEquals("test", event.getPublisher().getService());
        assertEquals("test", event.getProcessId());
        assertNotNull(event.getPayload());
        assertEquals("data", event.getPayload().getData());
        assertEquals("customId", event.getPayload().getOtherCustomId());
    }

    @Test
    void build_customPayloadData() {
        TestDomain4Event event = TestDomain4EventBuilder.builder()
                .idempotenceId("idempotence-2")
                .payloadData("custom-data", "custom-id")
                .build();

        assertEquals("custom-data", event.getPayload().getData());
        assertEquals("custom-id", event.getPayload().getOtherCustomId());
    }
}
