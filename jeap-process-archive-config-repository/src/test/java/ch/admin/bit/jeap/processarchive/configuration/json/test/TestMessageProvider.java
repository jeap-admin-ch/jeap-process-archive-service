package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.MessageProvider;

public class TestMessageProvider implements MessageProvider<Message> {
    @Override
    public ArchiveDataReference getReference(Message message) {
        throw new UnsupportedOperationException();
    }
}
