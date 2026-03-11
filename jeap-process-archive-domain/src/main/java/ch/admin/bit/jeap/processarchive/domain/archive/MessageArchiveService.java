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

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageArchiveService {

    private final List<ArtifactArchivedListener> artifactArchivedListener;
    private final ArchiveDataObjectStore archiveDataObjectStore;
    private final ArchiveDataSchemaValidationService validationService;
    private final FeatureManager featureManager;

    @Timed(value = "jeap_pas_archive_message", description = "Archive Message from fetch to commit")
    public void archive(MessageArchiveConfiguration configuration, Message message) {
        String processId = readOriginProcessId(configuration, message);

        if (!configuration.acceptsMessage(message)) {
            log.debug("Condition prevented archiving data for message {}", message.getIdentity().getId());
            return;
        }

        ArchiveData archiveData = readArchiveData(configuration, message);

        if (archiveData == null) {
            log.info("No data to archive for message {}", message.getIdentity().getId());
            return;
        }

        log.info("Extracted archive data from message {}: {} {} {}",
                keyValue("messageType", message.getType().getName()),
                keyValue("processId", processId),
                keyValue("referenceId", archiveData.getReferenceId()),
                keyValue("version", archiveData.getVersion()));

        ArchiveDataSchema schema = validationService.validateArchiveDataSchema(archiveData);

        ArchivedArtifact archivedArtifact = archiveArtifact(archiveData, schema, processId, createArchiveArtifactIdempotenceId(message));

        if (isFeatureFlagActive(configuration)) {
            artifactArchivedListener.forEach(listener -> listener.onArtifactArchived(archivedArtifact));
        }

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
     * Create the idempotenceId for the archive with the name of the type and the idempotenceId of the message.
     * The idempotenceId of a message is only unique within the message type.
     *
     * @param message the message with the information to archive
     * @return the idempotenceId for the archive
     */
    private String createArchiveArtifactIdempotenceId(Message message) {
        return message.getType().getName() + "_" + message.getIdentity().getIdempotenceId();
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
