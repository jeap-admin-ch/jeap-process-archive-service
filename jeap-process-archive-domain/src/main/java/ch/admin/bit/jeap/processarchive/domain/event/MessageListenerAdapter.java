package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;

import java.util.List;

public interface MessageListenerAdapter {

    void start(List<MessageArchiveConfiguration> configurations);
}
