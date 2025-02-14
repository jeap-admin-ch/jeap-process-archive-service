package ch.admin.bit.jeap.processarchive.registry.verifier.common;

import org.apache.avro.Protocol;

import java.util.Comparator;

record ArchiveTypeVersion(
        int version,
        CompatibilityMode compatibilityMode,
        int compatibleVersion,
        Protocol avroSchema) implements Comparable<ArchiveTypeVersion> {

    @Override
    public int compareTo(ArchiveTypeVersion archiveTypeVersion) {
        return Comparator.comparing(ArchiveTypeVersion::version)
                .compare(this, archiveTypeVersion);
    }
}
