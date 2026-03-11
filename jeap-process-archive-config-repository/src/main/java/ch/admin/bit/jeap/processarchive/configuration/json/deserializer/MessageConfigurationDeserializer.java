package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class MessageConfigurationDeserializer {

    private final SpringExpressionEvaluator springExpressionEvaluator;
    private final RemoteArchiveDataProvider remoteArchiveDataProvider;
    private final MeterRegistry meterRegistry;

    public List<MessageArchiveConfiguration> toConfiguration(List<ch.admin.bit.jeap.processarchive.configuration.json.model.MessageArchiveConfiguration> configs) {
        return configs.stream().map(this::toMessageArchiveConfiguration).toList();
    }

    private MessageArchiveConfiguration toMessageArchiveConfiguration(ch.admin.bit.jeap.processarchive.configuration.json.model.MessageArchiveConfiguration config) {
        String messageName = config.getMessageName();
        String topicName = springExpressionEvaluator.evaluateExpression(config.getTopicName());

        String messageArchiveDataReader = config.getMessageArchiveDataProvider();
        String referenceProvider = config.getReferenceProvider();
        String archiveDataReferenceProvider = config.getArchiveDataReferenceProvider();
        String condition = config.getCondition();
        String correlationProvider = config.getCorrelationProvider();
        String dataReaderEndpoint = springExpressionEvaluator.evaluateExpression(config.getUri());
        String oauthClientId = springExpressionEvaluator.evaluateExpression(config.getOauthClientId());

        if (!hasText(messageName)) {
            throw MessageArchiveConfigurationException.emptyMessageName();
        }
        if (!hasText(topicName)) {
            throw MessageArchiveConfigurationException.emptyTopicName(messageName);
        }
        if (!hasText(messageArchiveDataReader) && !hasText(referenceProvider) && !hasText(archiveDataReferenceProvider)) {
            throw MessageArchiveConfigurationException.noExtractor(messageName);
        }
        if (hasText(referenceProvider) && hasText(archiveDataReferenceProvider)) {
            throw MessageArchiveConfigurationException.tooManyExtractors(messageName);
        }
        if (hasText(messageArchiveDataReader) && (hasText(referenceProvider) || hasText(archiveDataReferenceProvider))) {
            throw MessageArchiveConfigurationException.tooManyExtractors(messageName);
        }
        if (hasText(messageArchiveDataReader) && hasText(referenceProvider) && hasText(archiveDataReferenceProvider)) {
            throw MessageArchiveConfigurationException.tooManyExtractors(messageName);
        }

        if (hasText(messageArchiveDataReader)) {
            if (hasText(dataReaderEndpoint)) {
                throw MessageArchiveConfigurationException.notEmptyDataReaderEndpoint(messageName);
            }

            return PayloadDataMessageArchiveConfiguration.builder()
                    .messageName(messageName)
                    .topicName(topicName)
                    .clusterName(config.getClusterName())
                    .messageArchiveDataProvider(Instances.newInstance(messageArchiveDataReader))
                    .archiveDataCondition(Instances.newInstance(condition))
                    .correlationProvider(Instances.newInstance(correlationProvider))
                    .featureFlag(config.getFeatureFlag())
                    .build();
        }

        if (!hasText(dataReaderEndpoint)) {
            throw MessageArchiveConfigurationException.emptyDataReaderEndpoint(messageName);
        }

        return RemoteDataMessageArchiveConfiguration.builder()
                .remoteArchiveDataProvider(remoteArchiveDataProvider)
                .messageName(messageName)
                .topicName(topicName)
                .clusterName(config.getClusterName())
                .archiveDataCondition(Instances.newInstance(condition))
                .correlationProvider(Instances.newInstance(correlationProvider))
                .referenceProvider(Instances.newInstance(referenceProvider))
                .archiveDataReferenceProvider(Instances.newInstance(archiveDataReferenceProvider))
                .dataReaderEndpoint(dataReaderEndpoint)
                .oauthClientId(oauthClientId)
                .meterRegistry(meterRegistry)
                .featureFlag(config.getFeatureFlag())
                .build();
    }

}
