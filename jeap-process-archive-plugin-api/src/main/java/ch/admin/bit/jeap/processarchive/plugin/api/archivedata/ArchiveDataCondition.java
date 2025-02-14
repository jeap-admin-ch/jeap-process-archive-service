package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.Message;

public interface ArchiveDataCondition<M extends Message> {

    /**
     * @param message Received message
     * @return true if data archiving should be done for this message, false otherwise
     */
    boolean isArchiveDataForMessage(M message);

}
