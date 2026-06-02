package ch.admin.bit.jeap.processarchive.configuration.json;

import ch.admin.bit.jeap.processarchive.configuration.json.deserializer.IndexTypeConfigurationDeserializer;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestIndexTypeConverter;
import ch.admin.bit.jeap.processarchive.configuration.json.test.TestMessage;
import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {IndexTypeConfigurationDeserializer.class})
class JsonIndexTypeConfigurationRepositoryTest {

    @Autowired
    private IndexTypeConfigurationDeserializer indexTypeConfigurationDeserializer;

    private JsonIndexTypeConfigurationRepository jsonIndexTypeConfigurationRepository;

    @BeforeEach
    void setUp() throws IOException {
        jsonIndexTypeConfigurationRepository = new JsonIndexTypeConfigurationRepository(indexTypeConfigurationDeserializer);
        jsonIndexTypeConfigurationRepository.loadTemplates();
    }

    @Test
    void getAllDomainEventReferenceDefinitions_eventsFound() {
        List<IndexTypeConfiguration> allIndexTypes = jsonIndexTypeConfigurationRepository.getAll();
        assertThat(allIndexTypes).hasSize(2);
    }

    @Test
    void findByName_indexTypeNotFound() {
        Optional<IndexTypeConfiguration> dummy = jsonIndexTypeConfigurationRepository.findByName("dummy");
        assertFalse(dummy.isPresent());
    }

    @Test
    void findByName_indexTypeFound() {
        Optional<IndexTypeConfiguration> indexType = jsonIndexTypeConfigurationRepository.findByName("TestIndexTypeV1");
        assertTrue(indexType.isPresent());
        assertSame(TestMessage.class, indexType.get().archiveType());
        assertSame(TestIndexTypeConverter.class, indexType.get().archiveTypeToSearchItemConverter().getClass());

    }
}
