package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.processarchive.domain.event.DomainEventReceiver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaDomainEventConsumerFactoryTest {

    @Mock
    private DomainEventReceiver domainEventReceiver;

    @Mock
    private ContractsProvider contractsProvider;

    @Mock
    private ContractsValidator contractsValidator;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private BeanFactory beanFactory;

    @Mock
    private JeapKafkaBeanNames jeapKafkaBeanNames;

    private KafkaDomainEventConsumerFactory kafkaDomainEventConsumerFactory;

    private final String topicName = "topicName";

    private final String defaultClusterName = "default";

    @BeforeEach
    void setup(){
        when(kafkaProperties.getDefaultClusterName()).thenReturn(defaultClusterName);
        kafkaDomainEventConsumerFactory = new KafkaDomainEventConsumerFactory(domainEventReceiver, contractsValidator, contractsProvider, kafkaProperties, beanFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void startConsumer_defaultCluster(){

        ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> kafkaListenerContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        when(kafkaListenerContainerFactory.createContainer(anyString())).thenReturn(mock(ConcurrentMessageListenerContainer.class));
        when(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(defaultClusterName)).thenReturn("test");
        when(beanFactory.getBean("test")).thenReturn(kafkaListenerContainerFactory);

        ReflectionTestUtils.setField(kafkaDomainEventConsumerFactory, "jeapKafkaBeanNames", jeapKafkaBeanNames);

        kafkaDomainEventConsumerFactory.startConsumer(topicName, Set.of("eventName"), null);

        verify(jeapKafkaBeanNames).getListenerContainerFactoryBeanName(defaultClusterName);
    }

    @Test
    @SuppressWarnings("unchecked")
    void startConsumer_definedCluster(){

        final String clusterName = "myClusterName";
        ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> kafkaListenerContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        when(kafkaListenerContainerFactory.createContainer(anyString())).thenReturn(mock(ConcurrentMessageListenerContainer.class));
        when(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(clusterName)).thenReturn("test");
        when(beanFactory.getBean("test")).thenReturn(kafkaListenerContainerFactory);

        ReflectionTestUtils.setField(kafkaDomainEventConsumerFactory, "jeapKafkaBeanNames", jeapKafkaBeanNames);

        kafkaDomainEventConsumerFactory.startConsumer(topicName, Set.of("eventName"), clusterName);

        verify(jeapKafkaBeanNames).getListenerContainerFactoryBeanName(clusterName);
    }

    @Test
    void startConsumer_withUndefinedCluster_throwsException() {
        when(beanFactory.getBean(anyString())).thenThrow(new NoSuchBeanDefinitionException("name"));

        assertThrows(IllegalStateException.class, () -> kafkaDomainEventConsumerFactory.startConsumer("topicName", Set.of("eventName"), "clusterNotDefined"));
    }


    @Test
    void ensureConsumerContract_contractV2(){

        when(contractsProvider.getContracts()).thenReturn(List.of(
                getContract("eventName", "1.2.3"),
                getContract("otherEvent", "4.5.6"),
                getContract("eventName", "9")

        ));

        ReflectionTestUtils.invokeMethod(kafkaDomainEventConsumerFactory, KafkaDomainEventConsumerFactory.class, "ensureConsumerContract", topicName, "eventName");
        verify(contractsValidator, times(1)).ensureConsumerContract(getAvroMessageType("eventName", "1.2.3"), topicName);
        verify(contractsValidator, times(1)).ensureConsumerContract(getAvroMessageType("eventName", "9"), topicName);
        verify(contractsValidator, never()).ensureConsumerContract(getAvroMessageType("otherEvent", "4.5.6"), topicName);
    }

    private AvroMessageType getAvroMessageType(String name, String version){
        AvroMessageType type = new AvroMessageType();
        type.setName(name);
        type.setVersion(version);
        return type;
    }

    private Contract getContract(String eventName, String version){
        return Contract.builder()
                .messageTypeName(eventName)
                .messageTypeVersion(version)
                .role("consumer")
                .build();
    }

}
