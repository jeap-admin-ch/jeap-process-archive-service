package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.MessageArchiveService;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageReceiver {

    private final MessageArchiveConfigurationRepository configurationRepository;
    private final MessageArchiveService messageArchiveService;

    public void messageReceived(Message message) {
        List<MessageArchiveConfiguration> configurations = configurationRepository.findByName(message.getType().getName());
        if (configurations.isEmpty()) {
            throw MessageReceiverException.unexpectedMessage(message).get();
        }

        messageArchiveService.archive(configurations, message);
    }
}
