package ch.admin.bit.jeap.processarchive.domain.archive.lifecycle;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LifecyclePolicy {
    String systemName;
    String archiveTypeName;
    int currentVersionExpirationDays;
    int previousVersionExpirationDays;
    int retainDays;
}
