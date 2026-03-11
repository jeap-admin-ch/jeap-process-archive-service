package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

class MessageArchiveConfigurationException extends RuntimeException {

    private MessageArchiveConfigurationException(String message) {
        super(message);
    }

    private MessageArchiveConfigurationException(String message, Exception cause) {
        super(message, cause);
    }

    static MessageArchiveConfigurationException emptyMessageName() {
        return new MessageArchiveConfigurationException("Missing mandatory property 'messageName'");
    }

    static MessageArchiveConfigurationException emptyTopicName(String messageType) {
        return new MessageArchiveConfigurationException("Missing mandatory property 'topicName' for message " + messageType);
    }

    static MessageArchiveConfigurationException noExtractor(String messageType) {
        return new MessageArchiveConfigurationException("No extractor found. One extractor has to be defined for message " + messageType);
    }

    static MessageArchiveConfigurationException tooManyExtractors(String messageType) {
        return new MessageArchiveConfigurationException("Two extractors found. Only one extractor has to be defined for message " + messageType);
    }

    static MessageArchiveConfigurationException emptyDataReaderEndpoint(String messageType) {
        return new MessageArchiveConfigurationException("Missing mandatory property 'dataReaderEndpoint' for message " + messageType);
    }

    static MessageArchiveConfigurationException notEmptyDataReaderEndpoint(String messageType) {
        return new MessageArchiveConfigurationException("dataReaderEndpoint set but not needed for message " + messageType);
    }

    public static MessageArchiveConfigurationException errorWhileCreatingInstance(String conditionClassName, Exception cause) {
        return new MessageArchiveConfigurationException("Error while creating instance of " + conditionClassName, cause);
    }

}
