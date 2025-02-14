package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.domain.archive.DomainEventArchiveService;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventReceiver {

    private final DomainEventArchiveConfigurationRepository configurationRepository;
    private final DomainEventArchiveService domainEventArchiveService;

    public void domainEventReceived(DomainEvent event) {
        DomainEventArchiveConfiguration configuration = configurationRepository.findByName(event.getType().getName())
                .orElseThrow(DomainEventReceiverException.unexpectedEvent(event));

        domainEventArchiveService.archive(configuration, event);
    }
}
