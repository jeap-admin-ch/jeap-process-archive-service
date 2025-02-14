package ch.admin.bit.jeap.processarchive.domain.archive.lifecycle;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;

import java.util.List;

public interface LifecyclePolicyService {

    LifecyclePolicy getLifecyclePolicy(ArchiveData archiveData);

    List<LifecyclePolicy> getAllLifecyclePolicies();
}
