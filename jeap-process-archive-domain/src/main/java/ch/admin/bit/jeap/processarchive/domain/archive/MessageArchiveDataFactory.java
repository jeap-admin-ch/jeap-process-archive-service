package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.configuration.PayloadDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageArchiveDataFactory implements ArchiveDataFactory {

    private final PayloadDataMessageArchiveConfiguration configuration;

    @Override
    public ArchiveData createArchiveData(Message message) {
        return configuration.getMessageArchiveDataProvider().getArchiveData(message);
    }
}
