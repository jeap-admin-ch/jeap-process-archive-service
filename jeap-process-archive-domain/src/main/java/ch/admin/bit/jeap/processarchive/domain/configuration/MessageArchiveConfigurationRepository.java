package ch.admin.bit.jeap.processarchive.domain.configuration;

import java.util.List;

public interface MessageArchiveConfigurationRepository {

    List<MessageArchiveConfiguration> getAll();

    /**
     * @return all archive configurations registered for the given message name. Multiple configurations may be
     * registered for the same message (and topic) to archive multiple artifacts per message. Empty if none.
     */
    List<MessageArchiveConfiguration> findByName(String messageName);
}
