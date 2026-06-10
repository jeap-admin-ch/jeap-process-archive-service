package ch.admin.bit.jeap.processarchive.domain.backfill;

import org.springframework.util.StringUtils;

public record BackfillArchiveDataReference(String id, Integer version) implements Comparable<BackfillArchiveDataReference> {

    public BackfillArchiveDataReference {
        if (!StringUtils.hasText(id)) {
            throw BackfillJobException.invalidRequest("archiveDataReferences[].id must not be empty");
        }
        if (version == null) {
            throw BackfillJobException.invalidRequest("archiveDataReferences[].version must not be null");
        }
    }

    @Override
    public int compareTo(BackfillArchiveDataReference other) {
        int idComparison = id.compareTo(other.id);
        if (idComparison != 0) {
            return idComparison;
        }
        return version.compareTo(other.version);
    }
}
