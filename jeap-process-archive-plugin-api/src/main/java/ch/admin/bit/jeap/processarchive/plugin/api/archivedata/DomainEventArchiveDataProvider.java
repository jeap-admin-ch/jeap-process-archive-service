package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * @deprecated Use {@link MessageArchiveDataProvider} instead
 */
@Deprecated(since = "10.20.0")
@SuppressWarnings("java:S1133") // deprecated interface kept for backward compatibility
public interface DomainEventArchiveDataProvider<E extends Message> extends MessageArchiveDataProvider<E> {

    /**
     * @return ArchiveData extracted from message payload, or null if no data should be archived for this message
     */
    ArchiveData getArchiveData(E payload);

}
