package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ArchiveTypeEncryption {
    @NonNull
    String secretEnginePath;
    @NonNull
    String keyName;
}
