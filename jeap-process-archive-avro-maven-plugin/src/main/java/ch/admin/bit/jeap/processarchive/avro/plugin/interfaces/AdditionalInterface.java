package ch.admin.bit.jeap.processarchive.avro.plugin.interfaces;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
class AdditionalInterface {
    @NonNull
    String javaInterface;
    @NonNull
    List<String> schemas;
}
