package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.configuration.DomainEventArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventArchiveService {

    private final List<ArtifactArchivedListener> artifactArchivedListener;
    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final ArchiveDataSchemaValidationService validationService;

    @Timed(value = "jeap_pas_archive_domain_event", description = "Archive DomainEvent from fetch to commit")
    public void archive(DomainEventArchiveConfiguration configuration, DomainEvent event) {
        String processId = readOriginProcessId(configuration, event);

        if (!configuration.acceptsMessage(event)) {
            log.debug("Condition prevented archiving data for event {}", event.getIdentity().getId());
            return;
        }

        ArchiveData archiveData = readArchiveData(configuration, event);

        if (archiveData == null) {
            log.info("No data to archive for event {}", event.getIdentity().getId());
            return;
        }

        log.info("Extracted archive data from event {}: {} {} {}",
                keyValue("eventType", event.getType().getName()),
                keyValue("processId", processId),
                keyValue("referenceId", archiveData.getReferenceId()),
                keyValue("version", archiveData.getVersion()));

        ArchiveDataSchema schema = validationService.validateArchiveDataSchema(archiveData);

        ArchivedArtifact archivedArtifact = archiveArtifact(archiveData, schema, processId, createArchiveArtifactIdempotenceId(event));

        artifactArchivedListener.forEach(listener -> listener.onArtifactArchived(archivedArtifact));
    }

    private ArchivedArtifact archiveArtifact(ArchiveData archiveData, ArchiveDataSchema schema, String processId, String idempotenceId) {
        ArchiveDataStorageInfo archiveDataStorageInfo = archiveDataObjectStore.store(archiveData, schema);
        return ArchivedArtifact.builder()
                .archiveData(archiveData)
                .idempotenceId(idempotenceId)
                .referenceIdType(schema.getReferenceIdType())
                .processId(processId)
                .storageObjectBucket(archiveDataStorageInfo.getBucket())
                .storageObjectKey(archiveDataStorageInfo.getKey())
                .storageObjectId(archiveDataStorageInfo.getName())
                .storageObjectVersionId(archiveDataStorageInfo.getVersionId())
                .expirationDays(schema.getExpirationDays())
                .build();
    }

    private ArchiveData readArchiveData(DomainEventArchiveConfiguration configuration, DomainEvent event) {
        try {
            return configuration.getArchiveDataFactory().createArchiveData(event);
        } catch (Exception ex) {
            throw ProcessArchiveException.failedToReadData(event, ex);
        }
    }

    /**
     * Create the idempotenceId for the archive with the name of the type and the idempotenceId of the event.
     * The idempotenceId of an event is only unique within the event type.
     *
     * @param event the event with the information to archive
     * @return the idempotenceId for the archive
     */
    private String createArchiveArtifactIdempotenceId(DomainEvent event) {
        return event.getType().getName() + "_" + event.getIdentity().getIdempotenceId();
    }

    private String readOriginProcessId(DomainEventArchiveConfiguration configuration, DomainEvent event) {
        if (configuration.getCorrelationProvider() != null) {
            String processId = configuration.getCorrelationProvider().getOriginProcessId(event);
            if (processId == null) {
                throw ProcessArchiveException.processIdFromCorrelationProviderMissing(event, configuration.getCorrelationProvider());
            }
            log.debug("Found processId '{}' from correlationProvider", processId);
            return processId;
        } else {
            return event.getOptionalProcessId().orElseThrow(ProcessArchiveException.mandatoryProcessIdMissing(event));
        }
    }
}
