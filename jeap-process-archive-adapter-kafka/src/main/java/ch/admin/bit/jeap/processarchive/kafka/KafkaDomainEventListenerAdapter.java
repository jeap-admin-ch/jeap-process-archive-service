package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventListenerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

@Component
@RequiredArgsConstructor
@Slf4j
class KafkaDomainEventListenerAdapter implements DomainEventListenerAdapter {

    private final KafkaDomainEventConsumerFactory domainEventConsumerFactory;

    @Override
    public void start(List<DomainEventArchiveConfiguration> configurations) {

        Set<DomainEventArchiveConfiguration> configWithoutCluster = configurations.stream().filter(config -> config.getClusterName() == null).collect(toSet());
        startConsumer(configWithoutCluster, null);

        Map<String, Set<DomainEventArchiveConfiguration>> configurationsByCluster = configurations.stream()
                .filter(config -> config.getClusterName() != null)
                .collect(groupingBy(DomainEventArchiveConfiguration::getClusterName, toSet()));
        for (Map.Entry<String, Set<DomainEventArchiveConfiguration>> configEntry : configurationsByCluster.entrySet()) {
            startConsumer(configEntry.getValue(), configEntry.getKey());
        }
    }

    private void startConsumer(Set<DomainEventArchiveConfiguration> configs, String clusterName) {
        Map<String, Set<String>> eventsByTopic = configs.stream().collect(groupingBy(DomainEventArchiveConfiguration::getTopicName, mapping(DomainEventArchiveConfiguration::getEventName, toSet())));
        for (Map.Entry<String, Set<String>> topicConfigEntry : eventsByTopic.entrySet()) {
            domainEventConsumerFactory.startConsumer(topicConfigEntry.getKey(), topicConfigEntry.getValue(), clusterName);
        }
    }

}
