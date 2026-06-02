package ch.admin.bit.jeap.processarchive.domain.archive.objectsstorage;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class StorageObjectProperties {
    String versionId;
    Map<String, String> metadata;
}
