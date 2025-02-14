package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageCorrelationProvider;
import ch.admin.bit.jeap.processcontext.event.test3.TestDomain3Event;

public class TestCorrelationProvider implements MessageCorrelationProvider<TestDomain3Event> {
    @Override
    public String getOriginProcessId(TestDomain3Event message) {
        return message.getPayload().getOtherCustomId();
    }
}
