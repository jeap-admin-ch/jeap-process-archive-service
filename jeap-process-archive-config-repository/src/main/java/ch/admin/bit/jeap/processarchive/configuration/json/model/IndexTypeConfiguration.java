package ch.admin.bit.jeap.processarchive.configuration.json.model;

import lombok.Data;

@Data
public class IndexTypeConfiguration {

    private String indexType;
    private String archiveType;
    private String archiveTypeToSearchItemConverter;

}
