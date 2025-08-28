package ch.admin.bit.jeap.processarchive.avro.plugin.registry;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.metadata.ArchiveTypeMetadata;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.File;
import java.util.Map;
import java.util.Optional;

@Builder
@Value
public class ArchiveTypeSchema {
    @NonNull
    File schema;
    @NonNull
    Map<String, File> importPath;

    TypeReference typeReference;

    ArchiveTypeMetadata archiveTypeMetadata;

    public Optional<ArchiveTypeMetadata> getArchiveTypeMetadata() {
        return Optional.ofNullable(archiveTypeMetadata);
    }

    public Optional<TypeReference> getTypeReference() {
        return Optional.ofNullable(typeReference);
    }
}
