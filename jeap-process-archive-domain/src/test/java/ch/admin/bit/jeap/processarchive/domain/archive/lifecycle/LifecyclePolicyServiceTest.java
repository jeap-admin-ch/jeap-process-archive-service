package ch.admin.bit.jeap.processarchive.domain.archive.lifecycle;

import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeInfo;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeNotFoundException;
import ch.admin.bit.jeap.processarchive.domain.archive.type.ArchiveTypeRepository;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifecyclePolicyServiceTest {

    private static final ArchiveTypeInfo TYPE_A = ArchiveTypeInfo.builder()
            .system("SysA").name("TypeA").version(1)
            .referenceIdType("ref.A").expirationDays(30)
            .build();

    private static final ArchiveTypeInfo TYPE_B = ArchiveTypeInfo.builder()
            .system("SysB").name("TypeB").version(1)
            .referenceIdType("ref.B").expirationDays(90)
            .build();

    @Test
    void getLifecyclePolicy_returnsCorrectPolicy() {
        ArchiveTypeRepository repo = mock(ArchiveTypeRepository.class);
        when(repo.requireType("SysA", "TypeA", 1)).thenReturn(TYPE_A);
        var service = new LifecyclePolicyService(repo);

        LifecyclePolicy result = service.getLifecyclePolicy(createArchiveData("SysA", "TypeA", 1));

        assertThat(result.getSystemName()).isEqualTo("SysA");
        assertThat(result.getArchiveTypeName()).isEqualTo("TypeA");
        assertThat(result.getCurrentVersionExpirationDays()).isEqualTo(30);
        assertThat(result.getPreviousVersionExpirationDays()).isEqualTo(30);
        assertThat(result.getRetainDays()).isEqualTo(30);
    }

    @Test
    void getLifecyclePolicy_unknownType_throws() {
        ArchiveTypeRepository repo = mock(ArchiveTypeRepository.class);
        when(repo.requireType("SysA", "Unknown", 1))
                .thenThrow(ArchiveTypeNotFoundException.forType("SysA", "Unknown", 1));
        var service = new LifecyclePolicyService(repo);
        ArchiveData archiveData = createArchiveData("SysA", "Unknown", 1);

        assertThatThrownBy(() -> service.getLifecyclePolicy(archiveData))
                .isInstanceOf(ArchiveTypeNotFoundException.class);
    }

    @Test
    void getAllLifecyclePolicies_returnsAll() {
        ArchiveTypeRepository repo = mock(ArchiveTypeRepository.class);
        when(repo.getAllTypes()).thenReturn(List.of(TYPE_A, TYPE_B));
        var service = new LifecyclePolicyService(repo);

        List<LifecyclePolicy> result = service.getAllLifecyclePolicies();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LifecyclePolicy::getArchiveTypeName)
                .containsExactly("TypeA", "TypeB");
    }

    private static ArchiveData createArchiveData(String system, String schema, int version) {
        return ArchiveData.builder()
                .contentType("test/type")
                .system(system)
                .schema(schema)
                .schemaVersion(version)
                .referenceId(UUID.randomUUID().toString())
                .payload("test".getBytes(StandardCharsets.UTF_8))
                .metadata(emptyList())
                .build();
    }
}
