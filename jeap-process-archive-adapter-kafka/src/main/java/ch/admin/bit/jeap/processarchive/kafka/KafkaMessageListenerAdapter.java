package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.event.MessageListenerAdapter;
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
class KafkaMessageListenerAdapter implements MessageListenerAdapter {

    private final KafkaMessageConsumerFactory domainEventConsumerFactory;

    @Override
    public void start(List<MessageArchiveConfiguration> configurations) {

        Set<MessageArchiveConfiguration> configWithoutCluster = configurations.stream().filter(config -> config.getClusterName() == null).collect(toSet());
        startConsumer(configWithoutCluster, null);

        Map<String, Set<MessageArchiveConfiguration>> configurationsByCluster = configurations.stream()
                .filter(config -> config.getClusterName() != null)
                .collect(groupingBy(MessageArchiveConfiguration::getClusterName, toSet()));
        for (Map.Entry<String, Set<MessageArchiveConfiguration>> configEntry : configurationsByCluster.entrySet()) {
            startConsumer(configEntry.getValue(), configEntry.getKey());
        }
    }

    private void startConsumer(Set<MessageArchiveConfiguration> configs, String clusterName) {
        Map<String, Set<String>> eventsByTopic = configs.stream().collect(groupingBy(MessageArchiveConfiguration::getTopicName, mapping(MessageArchiveConfiguration::getMessageName, toSet())));
        for (Map.Entry<String, Set<String>> topicConfigEntry : eventsByTopic.entrySet()) {
            domainEventConsumerFactory.startConsumer(topicConfigEntry.getKey(), topicConfigEntry.getValue(), clusterName);
        }
    }

}
