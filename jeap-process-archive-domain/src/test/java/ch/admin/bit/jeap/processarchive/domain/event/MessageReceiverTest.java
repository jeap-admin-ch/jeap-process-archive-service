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

import java.util.List;

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
        when(configurationRepository.findByName("TestEvent")).thenReturn(List.of(configuration));

        messageReceiver.messageReceived(message);

        verify(messageArchiveService).archive(List.of(configuration), message);
    }

    @Test
    void messageReceived_multipleConfigurationsFound_archivesAll() {
        Message message = createMessage("TestEvent");
        MessageArchiveConfiguration configuration1 = mock(MessageArchiveConfiguration.class);
        MessageArchiveConfiguration configuration2 = mock(MessageArchiveConfiguration.class);
        List<MessageArchiveConfiguration> configurations = List.of(configuration1, configuration2);
        when(configurationRepository.findByName("TestEvent")).thenReturn(configurations);

        messageReceiver.messageReceived(message);

        verify(messageArchiveService).archive(configurations, message);
    }

    @Test
    void messageReceived_configurationNotFound_throws() {
        Message message = createMessage("UnknownEvent");
        when(configurationRepository.findByName("UnknownEvent")).thenReturn(List.of());

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
