package ch.admin.bit.jeap.processarchive.plugin.api.archivedata.schema;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class EncryptionKeyReference {
    @NonNull
    String secretEnginePath;
    @NonNull
    String keyName;
}
