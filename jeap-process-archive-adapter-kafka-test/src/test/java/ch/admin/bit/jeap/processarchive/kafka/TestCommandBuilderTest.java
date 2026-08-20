package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.processarchive.command.test.TestCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestCommandBuilderTest {

    @BeforeAll
    static void installAvroClassWhitelist() {
        // No Spring context installs the Avro class whitelist for this test, and Avro rejects every class resolved
        // from a schema that is not trusted. All types used here are below ch.admin.bit.jeap, which the default
        // whitelist trusts.
        AvroClassSecurity.installDefaultIfMissing();
    }


    @Test
    void build_defaultValues() {
        TestCommand command = TestCommandBuilder.builder()
                .idempotenceId("idempotence-1")
                .build();

        assertNotNull(command.getIdentity());
        assertNotNull(command.getIdentity().getId());
        assertEquals("idempotence-1", command.getIdentity().getIdempotenceId());
        assertEquals("TestCommand", command.getType().getName());
        assertEquals("1.0.0", command.getType().getVersion());
        assertEquals("test", command.getPublisher().getSystem());
        assertEquals("test", command.getPublisher().getService());
        assertEquals("test", command.getProcessId());
        assertNotNull(command.getReferences());
        assertNotNull(command.getPayload());
        assertEquals("na", command.getPayload().getMessage());
    }

    @Test
    void build_customMessage() {
        TestCommand command = TestCommandBuilder.builder()
                .idempotenceId("idempotence-2")
                .message("custom-message")
                .build();

        assertEquals("custom-message", command.getPayload().getMessage());
    }
}
