package ch.admin.bit.jeap.processarchive.domain.configuration;

import java.util.List;
import java.util.Optional;

public interface DomainEventArchiveConfigurationRepository {

    List<DomainEventArchiveConfiguration> getAll();

    Optional<DomainEventArchiveConfiguration> findByName(String eventName);
}
