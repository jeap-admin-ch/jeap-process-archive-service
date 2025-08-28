package ch.admin.bit.jeap.processarchive.avro.plugin.git;

import java.util.Set;
import java.util.stream.Collectors;

public record GitDiffDto(
        Set<NewArchiveTypeVersionDto> newArchiveTypeVersions) {

    public Set<String> systems() {
        return newArchiveTypeVersions.stream()
                .map(NewArchiveTypeVersionDto::systemName)
                .collect(Collectors.toSet());
    }

    public boolean hasChanges() {
        return !newArchiveTypeVersions.isEmpty();
    }
}
