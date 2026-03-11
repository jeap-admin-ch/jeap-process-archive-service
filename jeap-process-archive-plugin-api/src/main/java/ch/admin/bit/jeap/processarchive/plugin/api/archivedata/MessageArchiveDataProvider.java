package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.Message;

public interface MessageArchiveDataProvider<E extends Message> {

    /**
     * @return ArchiveData extracted from message payload, or null if no data should be archived for this message
     */
    ArchiveData getArchiveData(E payload);

}
