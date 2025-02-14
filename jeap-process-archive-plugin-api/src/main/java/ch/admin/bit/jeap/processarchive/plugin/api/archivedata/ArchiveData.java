package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Value
@Builder
public class ArchiveData {
    /**
     * Content Type, a.k.a. Internet Media Type / MIME-Type (i.e. avro/binary)
     */
    @NonNull
    String contentType;
    /**
     * System that defines the schema
     */
    @NonNull
    String system;
    /**
     * Schema of the data (i.e. the specific schema of the content, ex. a specific Avro schema)
     */
    @NonNull
    String schema;
    /**
     * Version of the schema
     */
    int schemaVersion;
    /**
     * Archive data ID
     */
    @NonNull
    String referenceId;

    /**
     * Version of the data, if applicable
     */
    Integer version;

    /**
     * The data
     */
    @NonNull
    byte[] payload;
    /**
     * Optional: Storage prefix (~folder) where the data is stored (i.e. my-report/2021/12)
     */
    @NonNull
    @Builder.Default
    Optional<String> storagePrefix = Optional.empty();
    /**
     * Optional: Storage bucket where the data is stored
     */
    @NonNull
    @Builder.Default
    Optional<String> storageBucket = Optional.empty();
    @NonNull
    @Builder.Default
    List<Metadata> metadata = List.of();
}
