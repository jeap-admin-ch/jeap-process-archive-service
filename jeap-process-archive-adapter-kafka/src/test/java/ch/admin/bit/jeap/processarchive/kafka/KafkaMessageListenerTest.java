package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException;
import ch.admin.bit.jeap.processarchive.domain.archive.ProcessArchiveException;
import ch.admin.bit.jeap.processarchive.domain.event.MessageReceiver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

    @Mock
    private MessageReceiver messageReceiver;

    @Mock
    private Acknowledgment acknowledgment;

    private KafkaMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaMessageListener(Set.of("TestEvent", "OtherEvent"), messageReceiver);
    }

    @Test
    void onMessage_matchingType_receivesAndAcknowledges() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("TestEvent");

        listener.onMessage(consumerRecord, acknowledgment);

        verify(messageReceiver).messageReceived(consumerRecord.value());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_nonMatchingType_ignoresAndAcknowledges() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("UnknownEvent");

        listener.onMessage(consumerRecord, acknowledgment);

        verifyNoInteractions(messageReceiver);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void onMessage_kafkaException_throwsTemporaryMessageHandlerException() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("TestEvent");
        KafkaException kafkaException = KafkaException.interrupted(new InterruptedException("interrupted"));
        doThrow(kafkaException).when(messageReceiver).messageReceived(any());

        MessageHandlerException thrown = assertThrows(MessageHandlerException.class,
                () -> listener.onMessage(consumerRecord, acknowledgment));

        assertEquals("TEMPORARY", thrown.getTemporality().name());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onMessage_retryableProcessArchiveException_throwsTemporaryMessageHandlerException() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("TestEvent");
        doThrow(createProcessArchiveException(true)).when(messageReceiver).messageReceived(any());

        MessageHandlerException thrown = assertThrows(MessageHandlerException.class,
                () -> listener.onMessage(consumerRecord, acknowledgment));

        assertEquals("TEMPORARY", thrown.getTemporality().name());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onMessage_nonRetryableProcessArchiveException_throwsPermanentMessageHandlerException() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("TestEvent");
        doThrow(createProcessArchiveException(false)).when(messageReceiver).messageReceived(any());

        MessageHandlerException thrown = assertThrows(MessageHandlerException.class,
                () -> listener.onMessage(consumerRecord, acknowledgment));

        assertEquals("PERMANENT", thrown.getTemporality().name());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void onMessage_unexpectedException_throwsTemporaryMessageHandlerException() {
        ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord = createRecord("TestEvent");
        doThrow(new RuntimeException("unexpected")).when(messageReceiver).messageReceived(any());

        MessageHandlerException thrown = assertThrows(MessageHandlerException.class,
                () -> listener.onMessage(consumerRecord, acknowledgment));

        assertEquals("TEMPORARY", thrown.getTemporality().name());
        verify(acknowledgment, never()).acknowledge();
    }

    private ConsumerRecord<AvroMessageKey, AvroMessage> createRecord(String typeName) {
        AvroMessage message = mock(AvroMessage.class);
        AvroMessageType messageType = AvroMessageType.newBuilder()
                .setName(typeName)
                .setVersion("1.0.0")
                .build();
        when(message.getType()).thenReturn(messageType);

        return new ConsumerRecord<>("test-topic", 0, 0, null, message);
    }

    private ProcessArchiveException createProcessArchiveException(boolean retryable) {
        try {
            var constructor = ProcessArchiveException.class.getDeclaredConstructor(String.class, Throwable.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance("test error", null, retryable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
