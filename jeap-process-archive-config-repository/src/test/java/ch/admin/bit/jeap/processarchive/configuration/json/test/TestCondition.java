package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;

public class TestCondition implements ArchiveDataCondition<Message> {
    @Override
    public boolean isArchiveDataForMessage(Message message) {
        return true;
    }
}
