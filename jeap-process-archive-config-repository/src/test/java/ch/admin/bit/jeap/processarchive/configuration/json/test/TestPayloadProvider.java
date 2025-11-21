package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.PayloadProvider;

public class TestPayloadProvider implements PayloadProvider<MessagePayload> {
    @Override
    public ArchiveDataReference getReference(MessagePayload references) {
        throw new UnsupportedOperationException();
    }
}
