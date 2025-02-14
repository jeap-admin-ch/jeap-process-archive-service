package ch.admin.bit.jeap.test.processarchive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;
import ch.admin.bit.jeap.processcontext.event.test2.TestDomain2Event;

public class TestCondition implements ArchiveDataCondition<TestDomain2Event> {
    @Override
    public boolean isArchiveDataForMessage(TestDomain2Event message) {
        return !message.getPayload().getData().contains("ignored");
    }
}
