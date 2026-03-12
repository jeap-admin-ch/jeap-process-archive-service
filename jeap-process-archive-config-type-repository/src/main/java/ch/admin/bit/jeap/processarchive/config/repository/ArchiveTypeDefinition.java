package ch.admin.bit.jeap.processarchive.config.repository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArchiveTypeDefinition {
    private String archiveType;
    private int version;
    private String system;
    private int expirationDays;
    private String referenceIdType;
    private String encryptionKey;
}
