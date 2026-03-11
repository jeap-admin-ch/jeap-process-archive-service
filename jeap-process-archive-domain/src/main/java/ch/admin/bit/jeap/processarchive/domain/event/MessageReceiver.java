package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.MessageArchiveService;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReceiver {

    private final MessageArchiveConfigurationRepository configurationRepository;
    private final MessageArchiveService messageArchiveService;

    public void messageReceived(Message message) {
        MessageArchiveConfiguration configuration = configurationRepository.findByName(message.getType().getName())
                .orElseThrow(MessageReceiverException.unexpectedMessage(message));

        messageArchiveService.archive(configuration, message);
    }
}
