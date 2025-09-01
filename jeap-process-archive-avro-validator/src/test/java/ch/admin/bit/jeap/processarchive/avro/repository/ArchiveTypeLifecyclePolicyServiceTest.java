package ch.admin.bit.jeap.processarchive.avro.repository;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ArchiveTypeLifecyclePolicyServiceTest {

    private ArchiveTypeLifecyclePolicyService lifecyclePolicyService;

    @BeforeEach
    void beforeEach() throws IOException {
        ArchiveTypeLoader loader = new ArchiveTypeLoader(new TestArchiveTypeProvider());
        ArchiveTypeRepository repo = new ArchiveTypeRepository(loader, mock(ArchiveCryptoService.class));
        repo.initialize();
        lifecyclePolicyService = new ArchiveTypeLifecyclePolicyService(repo);
    }

    @Test
    void getLifecyclePolicy() {
        ArchiveData archiveData = createArchiveData();
        LifecyclePolicy lifecyclePolicy = lifecyclePolicyService.getLifecyclePolicy(archiveData);

        assertEquals("Decree", lifecyclePolicy.getArchiveTypeName());
        assertEquals("JME", lifecyclePolicy.getSystemName());
        assertEquals(30, lifecyclePolicy.getCurrentVersionExpirationDays());
        assertEquals(30, lifecyclePolicy.getPreviousVersionExpirationDays());
        assertEquals(30, lifecyclePolicy.getRetainDays());
    }

    @Test
    void getAllLifecyclePolicies() {
        List<LifecyclePolicy> allPolicies = lifecyclePolicyService.getAllLifecyclePolicies();
        assertEquals(2, allPolicies.size());
        assertEquals("Decree", allPolicies.get(0).getArchiveTypeName());
        assertEquals("DecreeDocument", allPolicies.get(1).getArchiveTypeName());
    }

    private ArchiveData createArchiveData() {
        return ArchiveData.builder()
                .contentType("avro/binary")
                .system("JME")
                .schema("Decree")
                .schemaVersion(2)
                .referenceId(UUID.randomUUID().toString())
                .payload(new byte[0])
                .metadata(emptyList())
                .build();
    }
}
