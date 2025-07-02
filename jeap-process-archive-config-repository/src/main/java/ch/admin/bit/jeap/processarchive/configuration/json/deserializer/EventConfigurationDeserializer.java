package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataDomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataDomainEventArchiveConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class EventConfigurationDeserializer {

    private final SpringExpressionEvaluator springExpressionEvaluator;
    private final RemoteArchiveDataProvider remoteArchiveDataProvider;
    private final MeterRegistry meterRegistry;

    public List<DomainEventArchiveConfiguration> toDomainEventReferences(List<ch.admin.bit.jeap.processarchive.configuration.json.model.DomainEventArchiveConfiguration> eventRefDefinitions) {
        return eventRefDefinitions.stream().map(this::toDomainEventReference).toList();
    }

    private DomainEventArchiveConfiguration toDomainEventReference(ch.admin.bit.jeap.processarchive.configuration.json.model.DomainEventArchiveConfiguration eventRefDefinition) {
        String eventName = eventRefDefinition.getEventName();
        String topicName = springExpressionEvaluator.evaluateExpression(eventRefDefinition.getTopicName());

        String domainEventArchiveDataReader = eventRefDefinition.getDomainEventArchiveDataProvider();
        String referenceProvider = eventRefDefinition.getReferenceProvider();
        String condition = eventRefDefinition.getCondition();
        String correlationProvider = eventRefDefinition.getCorrelationProvider();
        String dataReaderEndpoint = springExpressionEvaluator.evaluateExpression(eventRefDefinition.getUri());
        String oauthClientId = springExpressionEvaluator.evaluateExpression(eventRefDefinition.getOauthClientId());

        if (!hasText(eventName)) {
            throw DomainEventArchiveConfigurationException.emptyEventName();
        }
        if (!hasText(topicName)) {
            throw DomainEventArchiveConfigurationException.emptyTopicName(eventName);
        }
        if (!hasText(domainEventArchiveDataReader) && !hasText(referenceProvider)) {
            throw DomainEventArchiveConfigurationException.noExtractor(eventName);
        }
        if (hasText(domainEventArchiveDataReader) && hasText(referenceProvider)) {
            throw DomainEventArchiveConfigurationException.tooManyExtractors(eventName);
        }

        if (hasText(domainEventArchiveDataReader)) {
            if (hasText(dataReaderEndpoint)) {
                throw DomainEventArchiveConfigurationException.notEmptyDataReaderEndpoint(eventName);
            }

            return PayloadDataDomainEventArchiveConfiguration.builder()
                    .eventName(eventName)
                    .topicName(topicName)
                    .clusterName(eventRefDefinition.getClusterName())
                    .domainEventArchiveDataProvider(Instances.newInstance(domainEventArchiveDataReader))
                    .archiveDataCondition(Instances.newInstance(condition))
                    .correlationProvider(Instances.newInstance(correlationProvider))
                    .featureFlag(eventRefDefinition.getFeatureFlag())
                    .build();
        }

        if (!hasText(dataReaderEndpoint)) {
            throw DomainEventArchiveConfigurationException.emptyDataReaderEndpoint(eventName);
        }

        return RemoteDataDomainEventArchiveConfiguration.builder()
                .remoteArchiveDataProvider(remoteArchiveDataProvider)
                .eventName(eventName)
                .topicName(topicName)
                .clusterName(eventRefDefinition.getClusterName())
                .archiveDataCondition(Instances.newInstance(condition))
                .correlationProvider(Instances.newInstance(correlationProvider))
                .referenceProvider(Instances.newInstance(referenceProvider))
                .dataReaderEndpoint(dataReaderEndpoint)
                .oauthClientId(oauthClientId)
                .meterRegistry(meterRegistry)
                .featureFlag(eventRefDefinition.getFeatureFlag())
                .build();
    }

}
