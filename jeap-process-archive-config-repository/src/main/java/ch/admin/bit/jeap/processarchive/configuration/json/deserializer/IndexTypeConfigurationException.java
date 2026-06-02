package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

class IndexTypeConfigurationException extends RuntimeException {

    private IndexTypeConfigurationException(String message) {
        super(message);
    }

    static IndexTypeConfigurationException emptyIndexType() {
        return new IndexTypeConfigurationException("Missing mandatory property 'indexType'");
    }

    static IndexTypeConfigurationException emptyArchiveType(String indexType) {
        return new IndexTypeConfigurationException("Missing mandatory property 'archiveType' for indexType " + indexType);
    }

    static IndexTypeConfigurationException emptyArchiveTypeToSearchItemConverter(String indexType) {
        return new IndexTypeConfigurationException("Missing mandatory property 'archiveTypeToSearchItemConverter' for indexType " + indexType);
    }

}
