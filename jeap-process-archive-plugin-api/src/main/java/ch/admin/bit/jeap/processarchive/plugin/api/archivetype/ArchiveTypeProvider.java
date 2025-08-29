package ch.admin.bit.jeap.processarchive.plugin.api.archivetype;

import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

public interface ArchiveTypeProvider {

    /**
     * Get all supported archive type versions for this Process Archive Service instance.
     *
     * @return List of SpecificData classes representing all known archive type versions.
     */
    List<Class<? extends SpecificRecordBase>> getArchiveTypeVersions();
}
