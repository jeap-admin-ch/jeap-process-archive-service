package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processarchive.domain.archive.schema.ArchiveDataSchemaValidationService;
import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArchivedArtifact;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedartifact.ArtifactArchivedListener;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema.ArchiveDataSchema;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageArchiveService {

    private final List<ArtifactArchivedListener> artifactArchivedListener;
    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final ArchiveDataSchemaValidationService validationService;
    private final FeatureManager featureManager;

    /**
     * Archives all artifacts extracted from a single message. Multiple configurations may be registered for the same
     * message to archive multiple artifacts. The idempotence IDs of all resulting artifacts must be distinct; this is
     * enforced before any artifact is stored so that a misconfiguration fails fast instead of silently dropping events.
     */
    @Timed(value = "jeap_pas_archive_message", description = "Archive Message from fetch to commit")
    public void archive(List<MessageArchiveConfiguration> configurations, Message message) {
        List<PendingArchiveData> pendingArchiveData = extractArchiveData(configurations, message);
        ensureDistinctIdempotenceIds(pendingArchiveData, message);
        pendingArchiveData.forEach(this::storeAndPublish);
    }

    private List<PendingArchiveData> extractArchiveData(List<MessageArchiveConfiguration> configurations, Message message) {
        List<PendingArchiveData> pendingArchiveData = new ArrayList<>();
        for (MessageArchiveConfiguration configuration : configurations) {
            if (configuration.acceptsMessage(message)) {
                addArchiveData(message, configuration, pendingArchiveData);
            } else {
                log.debug("Condition prevented archiving data for message {}", message.getIdentity().getId());
            }
        }
        return pendingArchiveData;
    }

    private void addArchiveData(Message message, MessageArchiveConfiguration configuration, List<PendingArchiveData> pendingArchiveData) {
        ArchiveData archiveData = readArchiveData(configuration, message);
        if (archiveData != null) {
            String processId = readOriginProcessId(configuration, message);
            String idempotenceId = createArchiveArtifactIdempotenceId(message, archiveData);
            pendingArchiveData.add(new PendingArchiveData(configuration, archiveData, processId, idempotenceId));
        } else {
            log.info("No data to archive for message {}", message.getIdentity().getId());
        }
    }

    private void ensureDistinctIdempotenceIds(List<PendingArchiveData> pendingArchiveData, Message message) {
        Set<String> idempotenceIds = new HashSet<>();
        for (PendingArchiveData pending : pendingArchiveData) {
            if (!idempotenceIds.add(pending.idempotenceId())) {
                throw ProcessArchiveException.duplicateArtifactIdempotenceId(message, pending.idempotenceId());
            }
        }
    }

    private void storeAndPublish(PendingArchiveData pending) {
        ArchiveData archiveData = pending.archiveData();
        log.info("Extracted archive data from message {}: {} {} {} {}",
                keyValue("messageType", pending.configuration().getMessageName()),
                keyValue("configId", pending.configuration().getId()),
                keyValue("processId", pending.processId()),
                keyValue("referenceId", archiveData.getReferenceId()),
                keyValue("version", archiveData.getVersion()));

        ArchiveDataSchema schema = validationService.validateArchiveDataSchema(archiveData);
        ArchivedArtifact archivedArtifact = archiveArtifact(archiveData, schema, pending.processId(), pending.idempotenceId());

        if (isFeatureFlagActive(pending.configuration())) {
            artifactArchivedListener.forEach(listener -> listener.onArtifactArchived(archivedArtifact));
        }
    }

    private record PendingArchiveData(MessageArchiveConfiguration configuration, ArchiveData archiveData,
                                      String processId, String idempotenceId) {
    }

    private boolean isFeatureFlagActive(MessageArchiveConfiguration configuration) {
        if (configuration.getFeatureFlag() != null) {
            boolean active = featureManager.isActive(new NamedFeature(configuration.getFeatureFlag()));
            log.debug("FeatureFlag={} messageName={} state={}", configuration.getFeatureFlag(), configuration.getMessageName(), active);
            return active;
        }
        return true;
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

    private ArchiveData readArchiveData(MessageArchiveConfiguration configuration, Message message) {
        try {
            return configuration.getArchiveDataFactory().createArchiveData(message);
        } catch (Exception ex) {
            throw ProcessArchiveException.failedToReadData(message, ex);
        }
    }

    /**
     * Create the idempotenceId for the archive from the message type, the idempotenceId of the message and a
     * discriminator derived from the artifact, see {@link ArchiveArtifactIdempotenceId}.
     *
     * @param message     the message with the information to archive
     * @param archiveData the extracted archive data of the artifact
     * @return the idempotenceId for the archive
     */
    private String createArchiveArtifactIdempotenceId(Message message, ArchiveData archiveData) {
        return ArchiveArtifactIdempotenceId.create(
                message.getType().getName(), message.getIdentity().getIdempotenceId(), archiveData);
    }

    private String readOriginProcessId(MessageArchiveConfiguration configuration, Message message) {
        if (configuration.getCorrelationProvider() != null) {
            String processId = configuration.getCorrelationProvider().getOriginProcessId(message);
            if (processId == null) {
                throw ProcessArchiveException.processIdFromCorrelationProviderMissing(message, configuration.getCorrelationProvider());
            }
            log.debug("Found processId '{}' from correlationProvider", processId);
            return processId;
        } else {
            return message.getOptionalProcessId().orElseThrow(ProcessArchiveException.mandatoryProcessIdMissing(message));
        }
    }
}
