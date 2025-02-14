package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;

public class TestCorrelationProvider implements MessageCorrelationProvider<Message> {

    @Override
    public String getOriginProcessId(Message message) {
        throw new UnsupportedOperationException();
    }
}
