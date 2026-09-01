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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageConsumerFactoryTest {

    @Mock
    private MessageReceiver messageReceiver;

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

    private KafkaMessageConsumerFactory kafkaMessageConsumerFactory;

    private final String topicName = "topicName";

    private final String defaultClusterName = "default";

    @BeforeEach
    void setup(){
        when(kafkaProperties.getDefaultClusterName()).thenReturn(defaultClusterName);
        kafkaMessageConsumerFactory = new KafkaMessageConsumerFactory(messageReceiver, contractsValidator, contractsProvider,
                kafkaProperties, beanFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void startConsumer_defaultCluster(){

        ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> kafkaListenerContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        when(kafkaListenerContainerFactory.createListenerContainer(any())).thenReturn(mock(ConcurrentMessageListenerContainer.class));
        when(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(defaultClusterName)).thenReturn("test");
        when(beanFactory.getBean("test")).thenReturn(kafkaListenerContainerFactory);

        ReflectionTestUtils.setField(kafkaMessageConsumerFactory, "jeapKafkaBeanNames", jeapKafkaBeanNames);

        kafkaMessageConsumerFactory.startConsumer(topicName, Set.of("eventName"), null);

        verify(jeapKafkaBeanNames).getListenerContainerFactoryBeanName(defaultClusterName);
    }

    @Test
    @SuppressWarnings("unchecked")
    void startConsumer_definedCluster(){

        final String clusterName = "myClusterName";
        ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> kafkaListenerContainerFactory = mock(ConcurrentKafkaListenerContainerFactory.class);
        when(kafkaListenerContainerFactory.createListenerContainer(any())).thenReturn(mock(ConcurrentMessageListenerContainer.class));
        when(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(clusterName)).thenReturn("test");
        when(beanFactory.getBean("test")).thenReturn(kafkaListenerContainerFactory);

        ReflectionTestUtils.setField(kafkaMessageConsumerFactory, "jeapKafkaBeanNames", jeapKafkaBeanNames);

        kafkaMessageConsumerFactory.startConsumer(topicName, Set.of("eventName"), clusterName);

        verify(jeapKafkaBeanNames).getListenerContainerFactoryBeanName(clusterName);
    }

    @Test
    void startConsumer_withUndefinedCluster_throwsException() {
        when(beanFactory.getBean(anyString())).thenThrow(new NoSuchBeanDefinitionException("name"));
        Set<String> messageNames = Set.of("eventName");

        assertThrows(IllegalStateException.class, () -> kafkaMessageConsumerFactory.startConsumer("topicName", messageNames, "clusterNotDefined"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void startConsumer_usesListenerEndpointSoFactoryCanApplyListenerConfiguration() {
        ConcurrentKafkaListenerContainerFactory<AvroMessageKey, AvroMessage> containerFactory =
                mock(ConcurrentKafkaListenerContainerFactory.class);
        ConcurrentMessageListenerContainer<AvroMessageKey, AvroMessage> container =
                mock(ConcurrentMessageListenerContainer.class);
        when(containerFactory.createListenerContainer(any())).thenReturn(container);
        when(jeapKafkaBeanNames.getListenerContainerFactoryBeanName(defaultClusterName)).thenReturn("test");
        when(beanFactory.getBean("test")).thenReturn(containerFactory);
        ReflectionTestUtils.setField(kafkaMessageConsumerFactory, "jeapKafkaBeanNames", jeapKafkaBeanNames);

        kafkaMessageConsumerFactory.startConsumer(topicName, Set.of("eventName"), null);

        ArgumentCaptor<MethodKafkaListenerEndpoint<AvroMessageKey, AvroMessage>> endpointCaptor =
                ArgumentCaptor.forClass(MethodKafkaListenerEndpoint.class);
        verify(containerFactory).createListenerContainer(endpointCaptor.capture());
        MethodKafkaListenerEndpoint<AvroMessageKey, AvroMessage> endpoint = endpointCaptor.getValue();
        assertThat(endpoint.getTopics()).containsExactly(topicName);
        assertThat(endpoint.getBean()).isInstanceOf(KafkaMessageListener.class);
        assertThat(endpoint.getMethod().getName()).isEqualTo("onMessage");
        verify(container, never()).setupMessageListener(any());
        verify(container).start();
    }

    @Test
    void ensureConsumerContract_contractV2(){

        when(contractsProvider.getContracts()).thenReturn(List.of(
                getContract("eventName", "1.2.3"),
                getContract("otherEvent", "4.5.6"),
                getContract("eventName", "9")

        ));

        ReflectionTestUtils.invokeMethod(kafkaMessageConsumerFactory, KafkaMessageConsumerFactory.class, "ensureConsumerContract", topicName, "eventName");
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
