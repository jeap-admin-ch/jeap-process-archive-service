package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageArchiveDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerAdapterTest {

    @Mock
    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    @Test
    void start() {

        //given
        KafkaMessageListenerAdapter kafkaDomainEventListenerAdapter = new KafkaMessageListenerAdapter(kafkaMessageConsumerFactory);
        List<MessageArchiveConfiguration> eventConfigs = List.of(
                createDomainEventArchiveConfiguration("topicNameCluster1", "clusterName1", "eventNameCluster1"),
                createDomainEventArchiveConfiguration("topicNameCluster1", "clusterName1", "eventNameCluster2"),
                createDomainEventArchiveConfiguration("topicNameCluster3", "clusterName2", "eventNameCluster3"),
                createDomainEventArchiveConfiguration("topicName1", null, "eventName1"),
                createDomainEventArchiveConfiguration("topicName1", null, "eventName2")
        );

        //when
        kafkaDomainEventListenerAdapter.start(eventConfigs);

        //then
        verify(kafkaMessageConsumerFactory)
                .startConsumer("topicNameCluster1", Set.of("eventNameCluster1", "eventNameCluster2"), "clusterName1");
        verify(kafkaMessageConsumerFactory)
                .startConsumer("topicNameCluster3", Set.of("eventNameCluster3"), "clusterName2");
        verify(kafkaMessageConsumerFactory)
                .startConsumer("topicName1", Set.of("eventName1", "eventName2"), null);
    }

    @SuppressWarnings("unchecked")
    private MessageArchiveConfiguration createDomainEventArchiveConfiguration(String topicName, String clusterName, String messageName) {
        return PayloadDataMessageArchiveConfiguration.builder()
                .topicName(topicName)
                .clusterName(clusterName)
                .messageName(messageName)
                .messageArchiveDataProvider(mock(MessageArchiveDataProvider.class))
                .build();

    }

}
