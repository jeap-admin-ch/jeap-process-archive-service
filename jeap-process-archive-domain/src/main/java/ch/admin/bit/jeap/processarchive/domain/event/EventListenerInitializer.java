package ch.admin.bit.jeap.processarchive.domain.event;


import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventListenerInitializer {
    private final MessageListenerAdapter messageListenerAdapter;
    private final MessageArchiveConfigurationRepository configurationRepository;

    @EventListener
    public void onAppStarted(ApplicationStartedEvent event) {
        messageListenerAdapter.start(configurationRepository.getAll());
    }
}
