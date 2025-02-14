package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Value
@Builder
public class DownloadResult {
    @NonNull
    @Singular
    List<DownloadedSchema> schemas;

    @NonNull
    @Singular
    List<ArchiveTypeDescriptor> archiveTypeDescriptors;

    static DownloadResult merge(DownloadResult result1, DownloadResult result2) {
        List<DownloadedSchema> schemas = Stream.of(result1.getSchemas(), result2.getSchemas())
                .flatMap(Collection::stream)
                .collect(toList());
        List<ArchiveTypeDescriptor> descriptors = Stream.of(result1.getArchiveTypeDescriptors(), result2.getArchiveTypeDescriptors())
                .flatMap(Collection::stream)
                .collect(toList());
        return new DownloadResult(schemas, descriptors);
    }

    static DownloadResult empty() {
        return new DownloadResult(Collections.emptyList(), Collections.emptyList());
    }
}
