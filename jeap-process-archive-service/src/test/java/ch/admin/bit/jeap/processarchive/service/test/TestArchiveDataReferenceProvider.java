package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReferenceProvider;
import ch.admin.bit.jeap.processcontext.event.test4.TestDomain4Event;

public class TestArchiveDataReferenceProvider implements ArchiveDataReferenceProvider<TestDomain4Event> {

    @Override
    public ArchiveDataReference getReference(TestDomain4Event message) {
        return ArchiveDataReference.builder()
                .id(message.getPayload().getOtherCustomId())
                .build();
    }
}
