package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArchiveTypeId {
    String system;
    String name;
    int version;
}
