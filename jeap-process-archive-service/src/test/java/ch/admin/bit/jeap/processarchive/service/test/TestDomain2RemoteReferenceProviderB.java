package ch.admin.bit.jeap.processarchive.service.test;

import ch.admin.bit.jeap.processarchive.event.test2.TestDomain2Event;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReferenceProvider;

/**
 * Reference provider for the second remote-data configuration of {@code TestDomain2Event}. Produces a distinct
 * reference id (suffix {@value #REFERENCE_ID_SUFFIX}) so its artifact differs from the other configurations
 * registered for the same message.
 */
public class TestDomain2RemoteReferenceProviderB implements ArchiveDataReferenceProvider<TestDomain2Event> {

    public static final String REFERENCE_ID_SUFFIX = "-remoteB";

    @Override
    public ArchiveDataReference getReference(TestDomain2Event message) {
        return ArchiveDataReference.builder()
                .id(message.getIdentity().getEventId() + REFERENCE_ID_SUFFIX)
                .build();
    }
}
