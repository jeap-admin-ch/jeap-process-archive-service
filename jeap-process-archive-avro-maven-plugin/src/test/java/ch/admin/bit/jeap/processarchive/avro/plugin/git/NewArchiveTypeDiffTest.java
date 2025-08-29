package ch.admin.bit.jeap.processarchive.avro.plugin.git;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeDescriptor;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewArchiveTypeDiffTest {

    @Test
    void diffArchiveTypes() {
        ArchiveTypeDescriptor baseTypeDescriptor = createTypeDescriptor(1, 2);
        ArchiveTypeDescriptor newTypeDescriptor = createTypeDescriptor(2, 3);
        Path descriptorPath = Path.of("archive-types/sys/foo/Foo.json");

        Path sourceDir = Path.of(".");
        Set<NewArchiveTypeVersionDto> dto = NewArchiveTypeDiff
                .diffArchiveTypes(sourceDir, descriptorPath, newTypeDescriptor, baseTypeDescriptor);

        Path expectedPath = sourceDir.resolve(descriptorPath);
        assertThat(dto)
                .containsOnly(new NewArchiveTypeVersionDto("sys", expectedPath, newTypeDescriptor, 3));
    }

    private static ArchiveTypeDescriptor createTypeDescriptor(Integer... versionNumbers) {
        List<ArchiveTypeVersion> versions = Arrays.stream(versionNumbers)
                .map(v -> new ArchiveTypeVersion(v, "schema.avdl", null, null))
                .toList();
        return new ArchiveTypeDescriptor("system", "type", "ref", versions, 22, null, null);
    }
}
