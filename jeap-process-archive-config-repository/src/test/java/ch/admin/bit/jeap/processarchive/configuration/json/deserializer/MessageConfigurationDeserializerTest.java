package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import ch.admin.bit.jeap.processarchive.configuration.json.model.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageConfigurationDeserializerTest {

    @Mock
    private SpringExpressionEvaluator springExpressionEvaluator;

    @Mock
    private RemoteArchiveDataProvider remoteArchiveDataProvider;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private MessageConfigurationDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new MessageConfigurationDeserializer(springExpressionEvaluator, remoteArchiveDataProvider, meterRegistry);
        when(springExpressionEvaluator.evaluateExpression(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void toConfiguration_emptyMessageTypeName_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName(null);
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("messageName"));
    }

    @Test
    void toConfiguration_blankMessageTypeName_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("  ");
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("messageName"));
    }

    @Test
    void toConfiguration_emptyTopicName_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName(null);
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("topicName"));
    }

    @Test
    void toConfiguration_blankTopicName_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName("  ");
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("topicName"));
    }

    @Test
    void toConfiguration_noExtractor_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName("test-topic");
        config.setMessageArchiveDataProvider(null);
        config.setReferenceProvider(null);
        config.setArchiveDataReferenceProvider(null);
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("No extractor found"));
    }

    @Test
    void toConfiguration_noExtractor_blankValues_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName("test-topic");
        config.setMessageArchiveDataProvider("  ");
        config.setReferenceProvider("");
        config.setArchiveDataReferenceProvider(null);
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("No extractor found"));
    }

    @Test
    void toConfiguration_emptyDataReaderEndpoint_forRemoteProvider_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName("test-topic");
        config.setReferenceProvider("ch.admin.bit.jeap.processarchive.configuration.json.test.TestReferenceProvider");
        config.setUri(null);
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("dataReaderEndpoint"));
    }

    @Test
    void toConfiguration_blankDataReaderEndpoint_forRemoteProvider_throws() {
        MessageArchiveConfiguration config = new MessageArchiveConfiguration();
        config.setMessageName("TestEvent");
        config.setTopicName("test-topic");
        config.setReferenceProvider("ch.admin.bit.jeap.processarchive.configuration.json.test.TestReferenceProvider");
        config.setUri("  ");
        List<MessageArchiveConfiguration> configs = List.of(config);

        MessageArchiveConfigurationException exception = assertThrows(MessageArchiveConfigurationException.class,
                () -> deserializer.toConfiguration(configs));

        assertTrue(exception.getMessage().contains("dataReaderEndpoint"));
    }
}
