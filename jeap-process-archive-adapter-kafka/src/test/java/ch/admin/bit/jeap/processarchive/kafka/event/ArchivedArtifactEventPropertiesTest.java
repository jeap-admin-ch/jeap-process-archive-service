package ch.admin.bit.jeap.processarchive.kafka.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchivedArtifactEventPropertiesTest {

    @Test
    void validateConfiguration_shouldThrowWhenPropertiesAreEmpty() {

        assertThatThrownBy(() -> new ArchivedArtifactEventProperties().validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eventTopic")
                .hasMessageContaining("serviceName")
                .hasMessageContaining("systemId")
                .hasMessageContaining("systemName");
    }
}
