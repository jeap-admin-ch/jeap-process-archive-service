package ch.admin.bit.jeap.processarchive.domain.archive.type;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchiveTypeRepositoryTest {

    private static final ArchiveTypeInfo TYPE_A = ArchiveTypeInfo.builder()
            .system("Sys").name("TypeA").version(1)
            .referenceIdType("ref.A").expirationDays(30)
            .build();

    private static final ArchiveTypeInfo TYPE_B = ArchiveTypeInfo.builder()
            .system("Sys").name("TypeB").version(1)
            .referenceIdType("ref.B").expirationDays(90)
            .build();

    @Test
    void requireType_returnsType() {
        var repo = new ArchiveTypeRepository(List.of(() -> List.of(TYPE_A, TYPE_B)));

        ArchiveTypeInfo result = repo.requireType("Sys", "TypeA", 1);

        assertThat(result).isSameAs(TYPE_A);
    }

    @Test
    void requireType_unknownType_throws() {
        var repo = new ArchiveTypeRepository(List.of(() -> List.of(TYPE_A)));

        assertThatThrownBy(() -> repo.requireType("Sys", "Unknown", 1))
                .isInstanceOf(ArchiveTypeNotFoundException.class)
                .hasMessageContaining("Unknown");
    }

    @Test
    void getAllTypes_returnsAllTypesSorted() {
        var repo = new ArchiveTypeRepository(List.of(() -> List.of(TYPE_B, TYPE_A)));

        List<ArchiveTypeInfo> result = repo.getAllTypes();

        assertThat(result).containsExactly(TYPE_A, TYPE_B);
    }

    @Test
    void aggregatesMultipleProviders() {
        ArchiveTypeInfoProvider provider1 = () -> List.of(TYPE_A);
        ArchiveTypeInfoProvider provider2 = () -> List.of(TYPE_B);

        var repo = new ArchiveTypeRepository(List.of(provider1, provider2));

        assertThat(repo.getAllTypes()).hasSize(2);
        assertThat(repo.requireType("Sys", "TypeA", 1)).isSameAs(TYPE_A);
        assertThat(repo.requireType("Sys", "TypeB", 1)).isSameAs(TYPE_B);
    }

    @Test
    void duplicateType_keepsFirst() {
        ArchiveTypeInfo typeADuplicate = ArchiveTypeInfo.builder()
                .system("Sys").name("TypeA").version(1)
                .referenceIdType("ref.A.duplicate").expirationDays(999)
                .build();

        ArchiveTypeInfoProvider provider1 = () -> List.of(TYPE_A);
        ArchiveTypeInfoProvider provider2 = () -> List.of(typeADuplicate);

        var repo = new ArchiveTypeRepository(List.of(provider1, provider2));

        assertThat(repo.requireType("Sys", "TypeA", 1).getReferenceIdType()).isEqualTo("ref.A");
    }
}
