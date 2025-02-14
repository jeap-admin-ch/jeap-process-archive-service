package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.MessageReferences;

/**
 * Provides a data reference used to invoke the remote data reader endpoint (i.e. a microservice REST API providing the data
 * to be archived): <pre>${dataReaderEndpoint}/{referenceId}</pre>
 */
public interface ReferenceProvider<E extends MessageReferences> {

    /**
     * @param references {@link MessageReferences} of the received message to extract the archive data reference (id, version) from
     * @return ArchiveDataReference with id (+ optionally version) of the archived data. If null, no data will be archived for the message.
     */
    ArchiveDataReference getReference(E references);

}
