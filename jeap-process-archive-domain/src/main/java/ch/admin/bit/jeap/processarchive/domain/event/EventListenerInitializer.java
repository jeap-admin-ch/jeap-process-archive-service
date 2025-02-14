package ch.admin.bit.jeap.processarchive.domain.event;


import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventListenerInitializer {
    private final DomainEventListenerAdapter domainEventListenerAdapter;
    private final DomainEventArchiveConfigurationRepository configurationRepository;

    @EventListener
    public void onAppStarted(ApplicationStartedEvent event) {
        domainEventListenerAdapter.start(configurationRepository.getAll());
    }
}
