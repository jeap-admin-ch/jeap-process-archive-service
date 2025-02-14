package ch.admin.bit.jeap.processarchive.domain.archive;

import lombok.Value;

@Value(staticConstructor = "of")
public class ArchiveMetadata {

    String name;
    String value;
}
