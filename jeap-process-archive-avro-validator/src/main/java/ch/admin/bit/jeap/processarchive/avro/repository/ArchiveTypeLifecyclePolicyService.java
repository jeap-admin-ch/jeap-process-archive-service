package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class ArchiveTypeLifecyclePolicyService implements LifecyclePolicyService {

    private final ArchiveTypeRepository archiveTypeRepository;

    @Override
    public LifecyclePolicy getLifecyclePolicy(ArchiveData archiveData) {
        String archiveDataTypeName = archiveData.getSchema();
        String systemName = archiveData.getSystem();
        ArchiveTypeId archiveTypeId = ArchiveTypeId.builder()
                .system(systemName)
                .name(archiveDataTypeName)
                .version(archiveData.getSchemaVersion())
                .build();
        ArchiveType archiveType = archiveTypeRepository.requireArchiveType(archiveTypeId);

        return createLifecyclePolicy(archiveType);
    }

    @Override
    public List<LifecyclePolicy> getAllLifecyclePolicies() {
        return archiveTypeRepository.findAll().stream()
                .map(this::createLifecyclePolicy)
                .distinct()
                .collect(toList());
    }

    /**
     * Creates a {@link LifecyclePolicy} from an archive type definition.
     * Note: The PAS abstracts the details of S3 lifecycle policies. An archive type has a single expiration days value.
     * The PAS currently sets the current and noncurrent expiration days as well as the retention period to the same value.
     * This means that archive data is expired X days either after being saved as current version, or X days after being
     * transitioned to noncurrent. At the same time, archive data will be protected from deletion during a period of X days.
     */
    private LifecyclePolicy createLifecyclePolicy(ArchiveType archiveType) {
        return LifecyclePolicy.builder()
                .systemName(archiveType.getSystem())
                .archiveTypeName(archiveType.getName())
                .currentVersionExpirationDays(archiveType.getExpirationDays())
                .previousVersionExpirationDays(archiveType.getExpirationDays())
                .retainDays(archiveType.getExpirationDays())
                .build();
    }
}
