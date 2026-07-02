package ch.admin.bit.jeap.processarchive.domain.backfill;

import ch.admin.bit.jeap.processarchive.domain.configuration.MessageArchiveConfiguration;
import ch.admin.bit.jeap.processarchive.domain.configuration.RemoteDataMessageArchiveConfiguration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Resolves the unique remote-data archive configuration a backfill job targets, by message name and optional
 * config-id. Shared by job submission and command processing so both apply exactly the same rule.
 */
public final class BackfillArchiveConfigurationResolver {

    private BackfillArchiveConfigurationResolver() {
    }

    /**
     * @param messageName    the message name of the backfill job
     * @param configId       the optional config-id selecting a specific configuration
     * @param configurations all archive configurations registered for the message name
     * @return the unique remote-data configuration to use
     * @throws BackfillJobException if no matching remote-data configuration exists or the selection is ambiguous
     */
    public static RemoteDataMessageArchiveConfiguration resolve(String messageName, String configId,
                                                                List<MessageArchiveConfiguration> configurations) {
        if (configurations.isEmpty()) {
            throw BackfillJobException.configurationNotFound(messageName);
        }

        List<RemoteDataMessageArchiveConfiguration> remoteDataConfigurations = configurations.stream()
                .filter(RemoteDataMessageArchiveConfiguration.class::isInstance)
                .map(RemoteDataMessageArchiveConfiguration.class::cast)
                .toList();
        if (remoteDataConfigurations.isEmpty()) {
            throw BackfillJobException.configurationNotRemoteData(messageName);
        }

        if (StringUtils.hasText(configId)) {
            List<RemoteDataMessageArchiveConfiguration> matching = remoteDataConfigurations.stream()
                    .filter(config -> configId.equals(config.getId()))
                    .toList();
            if (matching.isEmpty()) {
                throw BackfillJobException.configurationConfigIdNotFound(messageName, configId);
            }
            if (matching.size() > 1) {
                throw BackfillJobException.configurationAmbiguous(messageName);
            }
            return matching.getFirst();
        }

        if (remoteDataConfigurations.size() > 1) {
            throw BackfillJobException.configurationAmbiguous(messageName);
        }
        return remoteDataConfigurations.getFirst();
    }
}
