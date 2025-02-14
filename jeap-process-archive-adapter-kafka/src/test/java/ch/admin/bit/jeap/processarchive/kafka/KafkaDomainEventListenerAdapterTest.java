package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.DomainEventArchiveDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventListenerAdapterTest {

    @Mock
    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;

    @Test
    void start() {

        //given
        KafkaDomainEventListenerAdapter kafkaDomainEventListenerAdapter = new KafkaDomainEventListenerAdapter(kafkaDomainEventConsumerFactory);
        List<DomainEventArchiveConfiguration> eventConfigs = List.of(
                createDomainEventArchiveConfiguration("topicNameCluster1", "clusterName1", "eventNameCluster1"),
                createDomainEventArchiveConfiguration("topicNameCluster1", "clusterName1", "eventNameCluster2"),
                createDomainEventArchiveConfiguration("topicNameCluster3", "clusterName2", "eventNameCluster3"),
                createDomainEventArchiveConfiguration("topicName1", null, "eventName1"),
                createDomainEventArchiveConfiguration("topicName1", null, "eventName2")
        );

        //when
        kafkaDomainEventListenerAdapter.start(eventConfigs);

        //then
        verify(kafkaDomainEventConsumerFactory)
                .startConsumer("topicNameCluster1", Set.of("eventNameCluster1", "eventNameCluster2"), "clusterName1");
        verify(kafkaDomainEventConsumerFactory)
                .startConsumer("topicNameCluster3", Set.of("eventNameCluster3"), "clusterName2");
        verify(kafkaDomainEventConsumerFactory)
                .startConsumer("topicName1", Set.of("eventName1", "eventName2"), null);
    }

    @SuppressWarnings("unchecked")
    private DomainEventArchiveConfiguration createDomainEventArchiveConfiguration(String topicName, String clusterName, String eventName) {
        return PayloadDataDomainEventArchiveConfiguration.builder()
                .topicName(topicName)
                .clusterName(clusterName)
                .eventName(eventName)
                .domainEventArchiveDataProvider(mock(DomainEventArchiveDataProvider.class))
                .build();

    }

}
