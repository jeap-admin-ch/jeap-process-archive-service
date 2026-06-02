package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import ch.admin.bit.jeap.processarchive.configuration.json.model.IndexTypeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IndexTypeConfigurationDeserializerTest {

    private IndexTypeConfigurationDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new IndexTypeConfigurationDeserializer();
    }

    @Test
    void toConfiguration_emptyIndexType_throws() {
        IndexTypeConfiguration config = new IndexTypeConfiguration();
        config.setIndexType(null);
        List<IndexTypeConfiguration> configs = List.of(config);

        IndexTypeConfigurationException exception = assertThrows(IndexTypeConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertThat(exception.getMessage()).isEqualTo("Missing mandatory property 'indexType'");
    }

    @Test
    void toConfiguration_emptyArchiveType_throws() {
        IndexTypeConfiguration config = new IndexTypeConfiguration();
        config.setIndexType("junit");
        config.setArchiveType(null);
        List<IndexTypeConfiguration> configs = List.of(config);

        IndexTypeConfigurationException exception = assertThrows(IndexTypeConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertThat(exception.getMessage()).isEqualTo("Missing mandatory property 'archiveType' for indexType junit");
    }

    @Test
    void toConfiguration_emptyArchiveTypeToSearchItemConverter_throws() {
        IndexTypeConfiguration config = new IndexTypeConfiguration();
        config.setIndexType("junit");
        config.setArchiveType("test");
        config.setArchiveTypeToSearchItemConverter(null);
        List<IndexTypeConfiguration> configs = List.of(config);

        IndexTypeConfigurationException exception = assertThrows(IndexTypeConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertThat(exception.getMessage()).isEqualTo("Missing mandatory property 'archiveTypeToSearchItemConverter' for indexType junit");
    }


}
