package ch.admin.bit.jeap.test.processarchive;

import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataCondition;

public class TestCondition implements ArchiveDataCondition<TestDomain2Event> {
    @Override
    public boolean isArchiveDataForMessage(TestDomain2Event message) {
        return !message.getPayload().getData().contains("ignored");
    }
}
