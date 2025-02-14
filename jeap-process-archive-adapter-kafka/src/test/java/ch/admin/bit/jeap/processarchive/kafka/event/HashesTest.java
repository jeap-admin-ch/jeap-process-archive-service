package ch.admin.bit.jeap.processarchive.kafka.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashesTest {

    @Test
    void hashProcessId() {
        assertThat(Hashes.hashReferenceId("1234", "4567"))
                .isEqualTo("146f3963dfbb010e3bd39f036d7e71d01c49bb400e7b360f75505b2e1dbf166b");
    }

    @Test
    void hashReferenceIdType() {
        assertThat(Hashes.hashReferenceIdType("4567"))
                .isEqualTo("bca04ca75edc0f6fb344fc7f259ea663efbf2789ae3294106dc3e44e68f63cb2");
    }
}
