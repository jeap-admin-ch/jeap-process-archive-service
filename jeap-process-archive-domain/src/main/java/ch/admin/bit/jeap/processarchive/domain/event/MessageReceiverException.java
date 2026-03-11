package ch.admin.bit.jeap.processarchive.domain.event;

import ch.admin.bit.jeap.messaging.model.Message;

import java.util.function.Supplier;

class MessageReceiverException extends RuntimeException {

    private MessageReceiverException(String message) {
        super(message);
    }

    static Supplier<MessageReceiverException> unexpectedMessage(Message message) {
        return () -> new MessageReceiverException(
                "Received unexpected event (missing archive configuration for message): " + message.getType().getName());
    }
}
