package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * Provides a data reference used to invoke the remote data reader endpoint (i.e. a microservice REST API providing the data
 * to be archived): <pre>${dataReaderEndpoint}/{referenceId}</pre>
 */
public interface MessageProvider<E extends Message> {

    /**
     * @param message {@link Message} of the received message to extract the archive data reference (id, version) from
     * @return ArchiveDataReference with id (+ optionally version) of the archived data. If null, no data will be archived for the message.
     */
    ArchiveDataReference getReference(E message);

}
