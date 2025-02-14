package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ReferenceProvider;
import ch.admin.bit.jeap.processcontext.event.test.TestDomainEventReferences;

public class TestReferenceProvider implements ReferenceProvider<TestDomainEventReferences> {

    @Override
    public ArchiveDataReference getReference(TestDomainEventReferences references) {
        return ArchiveDataReference.builder()
                .id(references.getDataReference().getDataId())
                .build();
    }
}
