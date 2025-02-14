package ch.admin.bit.jeap.processarchive.kafka;

import java.util.concurrent.ExecutionException;

public class KafkaException extends RuntimeException {

    private KafkaException(String message, Throwable cause) {
        super(message, cause);
    }

    public static KafkaException interrupted(InterruptedException e) {
        return new KafkaException("Sending of message has been interrupted", e);
    }

    public static KafkaException sendFailed(ExecutionException e) {
        return new KafkaException("Sending of message failed with an exception", e);
    }
}
