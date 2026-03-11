package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RemoteArchiveDataFactory implements ArchiveDataFactory {

    private final RemoteArchiveDataProvider remoteArchiveDataProvider;
    private final RemoteDataMessageArchiveConfiguration configuration;

    private final MeterRegistry meterRegistry;

    @Override
    public ArchiveData createArchiveData(Message message) {
        final var reference = configuration.getArchiveDataReferenceProvider() != null
                ? configuration.getArchiveDataReferenceProvider().getReference(message)
                : configuration.getReferenceProvider().getReference(message.getReferences());

        if (reference == null) {
            return null;
        }

        return meterRegistry.timer("jeap_pas_remote_archive_data_factory", "message", message.getType().getName())
                .record(() -> remoteArchiveDataProvider.readArchiveData(
                configuration.getDataReaderEndpoint(),
                configuration.getOauthClientId(),
                reference));
    }
}
