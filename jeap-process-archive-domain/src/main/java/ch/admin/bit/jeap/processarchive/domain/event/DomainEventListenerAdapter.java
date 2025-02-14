package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;

import java.util.List;

public interface DomainEventListenerAdapter {

    void start(List<DomainEventArchiveConfiguration> configurations);
}
