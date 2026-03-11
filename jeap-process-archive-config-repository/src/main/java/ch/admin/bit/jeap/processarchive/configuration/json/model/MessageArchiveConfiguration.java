package ch.admin.bit.jeap.processarchive.configuration.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MessageArchiveConfiguration {

    @JsonAlias("eventName")
    private String messageName;
    private String topicName;
    private String clusterName;
    @JsonAlias("domainEventArchiveDataProvider")
    private String messageArchiveDataProvider;
    private String referenceProvider;
    private String archiveDataReferenceProvider;
    private String condition;
    private String uri;
    private String oauthClientId;
    private String correlationProvider;
    private String featureFlag;

}
