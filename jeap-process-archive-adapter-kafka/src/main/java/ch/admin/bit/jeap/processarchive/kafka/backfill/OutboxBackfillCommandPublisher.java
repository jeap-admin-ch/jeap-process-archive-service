package ch.admin.bit.jeap.processarchive.kafka.backfill;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bit.jeap.processarchive.domain.backfill.BackfillCommandPublisher;
import ch.admin.bit.jeap.processarchive.domain.backfill.CreateArtifactCommandData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(TransactionalOutbox.class)
@RequiredArgsConstructor
@Slf4j
class OutboxBackfillCommandPublisher implements BackfillCommandPublisher {

    private final TransactionalOutbox transactionalOutbox;
    private final BackfillCommandProperties properties;

    @Override
    public void publish(CreateArtifactCommandData command) {
        log.debug("Publishing CreateArtifactCommand for backfill job '{}' and reference '{}' version '{}'.",
                command.jobId(), command.referenceId(), command.referenceVersion());
        transactionalOutbox.sendMessage(
                CreateArtifactCommandBuilder.builder(properties)
                        .commandData(command)
                        .build(),
                properties.getTopic());
    }
}
