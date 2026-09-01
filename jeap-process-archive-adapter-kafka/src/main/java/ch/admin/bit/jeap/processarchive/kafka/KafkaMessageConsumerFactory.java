package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.processarchive.domain.event.MessageReceiver;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.adapter.KafkaMessageHandlerMethodFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaMessageConsumerFactory {

    private static final Method MESSAGE_LISTENER_METHOD = messageListenerMethod();

    private final MessageReceiver messageReceiver;
    private final ContractsValidator contractsValidator;
    @Getter
    private final List<ConcurrentMessageListenerContainer<?, ?>> containers = new CopyOnWriteArrayList<>();

    private final ContractsProvider contractsProvider;

    private final KafkaProperties kafkaProperties;

    private final BeanFactory beanFactory;

    private final JeapKafkaBeanNames jeapKafkaBeanNames;

    private final KafkaMessageHandlerMethodFactory messageHandlerMethodFactory;

    public KafkaMessageConsumerFactory(MessageReceiver messageReceiver,
                                       ContractsValidator contractsValidator,
                                       ContractsProvider contractsProvider,
                                       KafkaProperties kafkaProperties,
                                       BeanFactory beanFactory) {
        this.messageReceiver = messageReceiver;
        this.contractsValidator = contractsValidator;
        this.contractsProvider = contractsProvider;
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
        this.messageHandlerMethodFactory = createMessageHandlerMethodFactory(beanFactory);
    }


    void startConsumer(String topicName, Set<String> messageNames, String clusterName) {
        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }

        log.info("Starting message listener for message(s) '{}' on topic '{}' on cluster '{}'", messageNames, topicName, clusterName);

        messageNames.forEach(messageName -> ensureConsumerContract(topicName, messageName));
        KafkaMessageListener listener = new KafkaMessageListener(messageNames, messageReceiver);
        startConsumer(topicName, clusterName, listener);
    }

    private void ensureConsumerContract(String topicName, String messageName) {
        //V2 Set the eventVersion from the contract files and check the consumer contract for each version
        final List<String> eventVersions = contractsProvider.getContracts().stream()
                .filter(contract -> contract.getMessageTypeName().equals(messageName) && contract.getRole().equalsIgnoreCase("consumer"))
                .map(Contract::getMessageTypeVersion)
                .toList();
        eventVersions.forEach(version -> ensureConsumerContract(messageName, version, topicName));
    }

    private void ensureConsumerContract(String messageName, String eventVersion, String topicName) {
        AvroMessageType type = new AvroMessageType();
        type.setName(messageName);
        type.setVersion(eventVersion);
        contractsValidator.ensureConsumerContract(type, topicName);
    }

    private void startConsumer(String topicName, String clusterName, AcknowledgingMessageListener<AvroMessageKey, AvroMessage> messageListener) {
        MethodKafkaListenerEndpoint<AvroMessageKey, AvroMessage> endpoint = new MethodKafkaListenerEndpoint<>();
        endpoint.setTopics(topicName);
        endpoint.setBean(messageListener);
        endpoint.setMethod(MESSAGE_LISTENER_METHOD);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);

        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container =
                getKafkaListenerContainerFactory(clusterName).createListenerContainer(endpoint);
        container.start();
        containers.add(container);
    }

    private static Method messageListenerMethod() {
        try {
            return KafkaMessageListener.class.getMethod("onMessage", ConsumerRecord.class, Acknowledgment.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Kafka message listener method not found", e);
        }
    }

    private static KafkaMessageHandlerMethodFactory createMessageHandlerMethodFactory(BeanFactory beanFactory) {
        KafkaMessageHandlerMethodFactory methodFactory = new KafkaMessageHandlerMethodFactory();
        methodFactory.setBeanFactory(beanFactory);
        methodFactory.setMessageConverter(new GenericMessageConverter());
        methodFactory.afterPropertiesSet();
        return methodFactory;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> getKafkaListenerContainerFactory(String clusterName) {
        try {
            return (ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage>) beanFactory.getBean(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(clusterName));
        } catch (NoSuchBeanDefinitionException exception) {
            log.error("No kafkaListenerContainerFactory found for cluster with name '{}'", clusterName);
            throw new IllegalStateException("No kafkaListenerContainerFactory found for cluster with name " + clusterName);
        }
    }


    @PreDestroy
    void stop() {
        log.info("Stopping all domain event listener containers...");
        containers.forEach(concurrentMessageListenerContainer -> concurrentMessageListenerContainer.stop(true));
    }
}
