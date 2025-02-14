package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.GitReference;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class ReferenceList {
    @NonNull
    String repoUrl;
    String branch;
    String commit;
    @NonNull
    List<ArchiveTypeReference> archiveTypes;

    public GitReference getGitReference() {
        if (commit == null && branch == null) {
            throw RegistryException.missingGitHistoryReference();
        }
        if (commit != null && branch != null) {
            throw RegistryException.ambiguousGitHistoryReference();
        }
        return commit != null ?
                GitReference.ofCommit(commit) :
                GitReference.ofBranch(branch);
    }
}
