package ch.admin.bit.jeap.processarchive.kafka;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.springframework.stereotype.Component;

@Component
public class IntegrationTestContractsProvider implements ContractsValidator {

    @Override
    public void ensurePublisherContract(MessageType messageType, String topic) {
        // Some integration tests publish events
    }

    @Override
    public void ensureConsumerContract(String messageTypeName, String topic) {
        // Disabled for test events
    }

    @Override
    public void ensureConsumerContract(MessageType messageType, String topic) {
        // Disabled for test events
    }
}
