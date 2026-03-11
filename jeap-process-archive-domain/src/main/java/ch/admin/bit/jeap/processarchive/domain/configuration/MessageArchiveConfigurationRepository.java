package ch.admin.bit.jeap.processarchive.domain.configuration;

import java.util.List;
import java.util.Optional;

public interface MessageArchiveConfigurationRepository {

    List<MessageArchiveConfiguration> getAll();

    Optional<MessageArchiveConfiguration> findByName(String messageName);
}
