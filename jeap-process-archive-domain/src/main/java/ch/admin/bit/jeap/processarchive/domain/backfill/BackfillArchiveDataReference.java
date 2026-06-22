package ch.admin.bit.jeap.processarchive.domain.backfill;

import org.springframework.util.StringUtils;

import java.util.Comparator;

public record BackfillArchiveDataReference(String id, Integer version) implements Comparable<BackfillArchiveDataReference> {

    public BackfillArchiveDataReference {
        if (!StringUtils.hasText(id)) {
            throw BackfillJobException.invalidRequest("archiveDataReferences[].id must not be empty");
        }
        if (version != null && version < 1) {
            throw BackfillJobException.invalidRequest("archiveDataReferences[].version must be positive when provided");
        }
    }

    @Override
    public int compareTo(BackfillArchiveDataReference other) {
        return Comparator.comparing(BackfillArchiveDataReference::id)
                .thenComparing(BackfillArchiveDataReference::version, Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(this, other);
    }
}
