package ch.admin.bit.jeap.processarchive.avro.plugin.registry;

import lombok.NonNull;
import lombok.Value;

@Value
public class TypeReference {
    @NonNull
    String system;
    @NonNull
    String name;
    @NonNull
    Integer version;
}
