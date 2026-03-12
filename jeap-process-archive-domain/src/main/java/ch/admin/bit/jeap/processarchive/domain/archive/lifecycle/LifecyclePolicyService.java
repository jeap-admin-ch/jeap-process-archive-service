package ch.admin.bit.jeap.processarchive.domain.archive.lifecycle;

import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeRepository;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Derives lifecycle policies from the unified archive type repository.
 * <p>
 * Note: The PAS abstracts the details of S3 lifecycle policies. An archive type has a single expiration days value.
 * The PAS currently sets the current and noncurrent expiration days as well as the retention period to the same value.
 * This means that archive data is expired X days either after being saved as current version, or X days after being
 * transitioned to noncurrent. At the same time, archive data will be protected from deletion during a period of X days.
 */
@Component
@RequiredArgsConstructor
public class LifecyclePolicyService {

    private final ArchiveTypeRepository archiveTypeRepository;

    public LifecyclePolicy getLifecyclePolicy(ArchiveData archiveData) {
        ArchiveTypeInfo typeInfo = archiveTypeRepository.requireType(
                archiveData.getSystem(), archiveData.getSchema(), archiveData.getSchemaVersion());
        return toLifecyclePolicy(typeInfo);
    }

    public List<LifecyclePolicy> getAllLifecyclePolicies() {
        return archiveTypeRepository.getAllTypes().stream()
                .map(this::toLifecyclePolicy)
                .distinct()
                .toList();
    }

    private LifecyclePolicy toLifecyclePolicy(ArchiveTypeInfo typeInfo) {
        return LifecyclePolicy.builder()
                .systemName(typeInfo.getSystem())
                .archiveTypeName(typeInfo.getName())
                .currentVersionExpirationDays(typeInfo.getExpirationDays())
                .previousVersionExpirationDays(typeInfo.getExpirationDays())
                .retainDays(typeInfo.getExpirationDays())
                .build();
    }
}
