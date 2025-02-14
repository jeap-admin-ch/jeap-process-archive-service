package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.messaging.model.MessageReferences;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ReferenceProvider;

public class TestReferenceProvider implements ReferenceProvider<MessageReferences> {
    @Override
    public ArchiveDataReference getReference(MessageReferences references) {
        throw new UnsupportedOperationException();
    }
}
