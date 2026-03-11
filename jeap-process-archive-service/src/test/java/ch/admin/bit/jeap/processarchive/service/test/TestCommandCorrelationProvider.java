package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;

public class TestCommandCorrelationProvider implements MessageCorrelationProvider<Message> {
    @Override
    public String getOriginProcessId(Message message) {
        return message.getOptionalProcessId().orElseThrow();
    }
}
