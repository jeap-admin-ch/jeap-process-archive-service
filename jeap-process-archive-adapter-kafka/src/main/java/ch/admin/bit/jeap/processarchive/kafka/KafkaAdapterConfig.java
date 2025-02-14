package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processarchive.domain.archive.event.ArchivedArtifactCreatedEventProducer;
import ch.admin.bit.jeap.processarchive.kafka.event.*;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@ComponentScan
@PropertySource("classpath:process-archive-kafka-defaults.properties")
@EnableConfigurationProperties(ArchivedArtifactEventProperties.class)
@RequiredArgsConstructor
@Slf4j
class KafkaAdapterConfig {

    private final ContractsValidator contractsValidator;
    private final ArchivedArtifactEventProperties eventProperties;

    @PostConstruct
    void validate() {
        if (eventProperties.isEnabled()) {
            // Make sure the PAS instance has a valid messaging contract to publish the SharedArchivedArtifactVersionCreatedEvent.
            // Otherwise, it will fail at runtime when publishing an event.
            contractsValidator.ensurePublisherContract(
                    ArchivedArtifactVersionCreatedEventBuilder.messageType(),
                    eventProperties.getEventTopic());
        }
    }

    @Bean
    @ConditionalOnProperty(
            value = "jeap.processarchive.archivedartifact.enabled",
            havingValue = "true",
            matchIfMissing = true)
    KafkaArchivedArtifactCreatedEventProducer kafkaArchivedArtifactCreatedEventProducer(
            ProcessArchiveEventPublisher eventPublisher, ArchivedArtifactVersionCreatedEventFactory eventFactory) {
        return new KafkaArchivedArtifactCreatedEventProducer(eventPublisher, eventFactory);
    }

    @Bean
    @ConditionalOnProperty(
            value = "jeap.processarchive.archivedartifact.enabled",
            havingValue = "false")
    ArchivedArtifactCreatedEventProducer noOpArchivedArtifactCreatedEventProducer() {
        return new ArchivedArtifactCreatedEventProducer() {
            @Override
            public void onArchivedArtifact(ArchivedArtifact archivedArtifact) {
                // no-op
            }
        };
    }
}
