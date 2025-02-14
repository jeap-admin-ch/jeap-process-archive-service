package ch.admin.bit.jeap.processarchive.registry.repository;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class ArchiveTypeDefinitionReferences {
    @NonNull
    String repoUrl;
    String commit;
    String branch;
    @NonNull
    List<String> systems;

    public GitReference getGitReference() {
        if (commit == null && branch == null) {
            throw RepositoryException.missingGitHistoryReference();
        }
        if (commit != null && branch != null) {
            throw RepositoryException.ambiguousGitHistoryReference();
        }
        return commit != null ? GitReference.ofCommit(commit) : GitReference.ofBranch(branch);
    }
}
