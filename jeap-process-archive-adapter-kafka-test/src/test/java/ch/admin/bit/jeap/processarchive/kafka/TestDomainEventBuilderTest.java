package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.processarchive.event.test.TestDomainEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestDomainEventBuilderTest {

    @BeforeAll
    static void installAvroClassWhitelist() {
        // No Spring context installs the Avro class whitelist for this test, and Avro rejects every class resolved
        // from a schema that is not trusted. All types used here are below ch.admin.bit.jeap, which the default
        // whitelist trusts.
        AvroClassSecurity.installDefaultIfMissing();
    }


    @Test
    void build_defaultValues() {
        TestDomainEvent event = TestDomainEventBuilder.builder()
                .idempotenceId("idempotence-1")
                .build();

        assertNotNull(event.getIdentity());
        assertNotNull(event.getIdentity().getEventId());
        assertEquals("idempotence-1", event.getIdentity().getIdempotenceId());
        assertEquals("TestDomainEvent", event.getType().getName());
        assertEquals("1.0.0", event.getType().getVersion());
        assertEquals("test", event.getPublisher().getSystem());
        assertEquals("test", event.getPublisher().getService());
        assertEquals("test", event.getProcessId());
        assertNotNull(event.getReferences());
        assertNotNull(event.getReferences().getDataReference());
        assertEquals("na", event.getReferences().getDataReference().getDataId());
        assertEquals("data", event.getReferences().getDataReference().getType());
        assertEquals("1", event.getReferences().getDataReference().getVersion());
    }

    @Test
    void build_customDataId() {
        TestDomainEvent event = TestDomainEventBuilder.builder()
                .idempotenceId("idempotence-2")
                .dataId("custom-data-id")
                .build();

        assertEquals("custom-data-id", event.getReferences().getDataReference().getDataId());
    }
}
