package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.processarchive.domain.archive.MessageArchiveService;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageReceiverTest {

    @Mock
    private MessageArchiveConfigurationRepository configurationRepository;

    @Mock
    private MessageArchiveService messageArchiveService;

    @InjectMocks
    private MessageReceiver messageReceiver;

    @Test
    void messageReceived_configurationFound_archives() {
        Message message = createMessage("TestEvent");
        MessageArchiveConfiguration configuration = mock(MessageArchiveConfiguration.class);
        when(configurationRepository.findByName("TestEvent")).thenReturn(Optional.of(configuration));

        messageReceiver.messageReceived(message);

        verify(messageArchiveService).archive(configuration, message);
    }

    @Test
    void messageReceived_configurationNotFound_throws() {
        Message message = createMessage("UnknownEvent");
        when(configurationRepository.findByName("UnknownEvent")).thenReturn(Optional.empty());

        assertThrows(MessageReceiverException.class, () -> messageReceiver.messageReceived(message));

        verifyNoInteractions(messageArchiveService);
    }

    private Message createMessage(String typeName) {
        Message message = mock(Message.class);
        MessageType messageType = mock(MessageType.class);
        when(messageType.getName()).thenReturn(typeName);
        when(message.getType()).thenReturn(messageType);
        return message;
    }
}
