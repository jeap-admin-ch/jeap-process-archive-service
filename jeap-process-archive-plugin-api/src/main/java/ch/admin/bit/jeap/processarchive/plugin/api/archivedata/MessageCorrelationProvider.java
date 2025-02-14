package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.Message;

public interface MessageCorrelationProvider<M extends Message> {

    /**
     * Maps a message to a process origin ID, which is then set as attribute on the ArchivedArtifact.
     * If no Process ID is returned, the process archive service throws an exception.
     *
     * @param message Message
     * @return Origin Process ID
     */
    String getOriginProcessId(M message);

}
