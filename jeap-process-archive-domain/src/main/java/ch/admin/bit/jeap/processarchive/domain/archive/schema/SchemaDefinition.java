package ch.admin.bit.jeap.processarchive.domain.archive.schema;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchemaDefinition {
    byte[] definition;
    String fileExtension;
}
