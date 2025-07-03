package ch.admin.bit.jeap.processarchive.configuration.json.model;

import lombok.Data;

@Data
public class DomainEventArchiveConfiguration {

    private String eventName;
    private String topicName;
    private String clusterName;
    private String domainEventArchiveDataProvider;
    private String referenceProvider;
    private String condition;
    private String uri;
    private String oauthClientId;
    private String correlationProvider;
    private String featureFlag;

}
