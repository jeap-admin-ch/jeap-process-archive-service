package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import lombok.NonNull;
import lombok.Value;

@Value
public class ArchiveTypeReference {
    @NonNull
    String system;
    @NonNull
    String name;
    @NonNull
    Integer version;
}
