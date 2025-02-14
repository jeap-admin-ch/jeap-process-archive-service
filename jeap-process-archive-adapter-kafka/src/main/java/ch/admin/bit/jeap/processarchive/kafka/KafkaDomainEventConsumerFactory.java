package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventReceiver;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class KafkaDomainEventConsumerFactory {

    private final DomainEventReceiver domainEventReceiver;
    private final ContractsValidator contractsValidator;
    @Getter
    private final List<ConcurrentMessageListenerContainer<?, ?>> containers = new CopyOnWriteArrayList<>();

    private final ContractsProvider contractsProvider;

    private final KafkaProperties kafkaProperties;

    private final BeanFactory beanFactory;

    private final JeapKafkaBeanNames jeapKafkaBeanNames;


    public KafkaDomainEventConsumerFactory(DomainEventReceiver domainEventReceiver,
                                           ContractsValidator contractsValidator,
                                           ContractsProvider contractsProvider,
                                           KafkaProperties kafkaProperties,
                                           BeanFactory beanFactory) {
        this.domainEventReceiver = domainEventReceiver;
        this.contractsValidator = contractsValidator;
        this.contractsProvider = contractsProvider;
        this.kafkaProperties = kafkaProperties;
        this.beanFactory = beanFactory;
        this.jeapKafkaBeanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
    }



    void startConsumer(String topicName, Set<String> eventNames, String clusterName) {
        if (!StringUtils.hasText(clusterName)) {
            clusterName = kafkaProperties.getDefaultClusterName();
        }

        log.info("Starting domain event listener for event(s) '{}' on topic '{}' on cluster '{}'", eventNames, topicName, clusterName);

        eventNames.forEach(eventName -> ensureConsumerContract(topicName, eventName));
        KafkaMessageListener listener = new KafkaMessageListener(eventNames, domainEventReceiver);
        startConsumer(topicName, clusterName, listener);
    }

    private void ensureConsumerContract(String topicName, String eventName) {
        //V2 Set the eventVersion from the contract files and check the consumer contract for each version
        final List<String> eventVersions = contractsProvider.getContracts().stream()
                .filter(contract -> contract.getMessageTypeName().equals(eventName) && contract.getRole().equalsIgnoreCase("consumer"))
                .map(Contract::getMessageTypeVersion)
                .toList();
        eventVersions.forEach(version -> ensureConsumerContract(eventName, version, topicName));
    }

    private void ensureConsumerContract(String eventName, String eventVersion, String topicName){
        AvroMessageType type = new AvroMessageType();
        type.setName(eventName);
        type.setVersion(eventVersion);
        contractsValidator.ensureConsumerContract(type, topicName);
    }

    private void startConsumer(String topicName, String clusterName, AcknowledgingMessageListener<AvroMessageKey, AvroDomainEvent> messageListener) {
        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container = getKafkaListenerContainerFactory(clusterName).createContainer(topicName);
        container.setupMessageListener(messageListener);
        container.start();
        containers.add(container);
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
