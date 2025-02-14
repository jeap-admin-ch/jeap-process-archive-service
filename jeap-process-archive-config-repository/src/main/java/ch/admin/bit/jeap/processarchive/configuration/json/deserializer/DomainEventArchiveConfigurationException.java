package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

class DomainEventArchiveConfigurationException extends RuntimeException {

    private DomainEventArchiveConfigurationException(String message) {
        super(message);
    }

    private DomainEventArchiveConfigurationException(String message, Exception cause) {
        super(message, cause);
    }

    static DomainEventArchiveConfigurationException emptyEventName() {
        return new DomainEventArchiveConfigurationException("Missing mandatory event property 'eventName'");
    }

    static DomainEventArchiveConfigurationException emptyTopicName(String eventName) {
        return new DomainEventArchiveConfigurationException("Missing mandatory event property 'topicName' for event " + eventName);
    }

    static DomainEventArchiveConfigurationException noExtractor(String eventName) {
        return new DomainEventArchiveConfigurationException("No extractor found. One extractor has to be defined for event " + eventName);
    }

    static DomainEventArchiveConfigurationException tooManyExtractors(String eventName) {
        return new DomainEventArchiveConfigurationException("Two extractors found. Only one extractor has to be defined for event " + eventName);
    }

    static DomainEventArchiveConfigurationException emptyDataReaderEndpoint(String eventName) {
        return new DomainEventArchiveConfigurationException("Missing mandatory event property 'dataReaderEndpoint' for event " + eventName);
    }

    static DomainEventArchiveConfigurationException notEmptyDataReaderEndpoint(String eventName) {
        return new DomainEventArchiveConfigurationException("dataReaderEndpoint set but not needed for event " + eventName);
    }

    public static DomainEventArchiveConfigurationException errorWhileCreatingInstance(String conditionClassName, Exception cause) {
        return new DomainEventArchiveConfigurationException("Error while creating instance of " + conditionClassName, cause);
    }

}
