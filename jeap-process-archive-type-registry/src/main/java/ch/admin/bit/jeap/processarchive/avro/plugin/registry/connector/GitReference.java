package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GitReference {
    String branch;
    String commit;

    public static GitReference ofBranch(String branch) {
        return new GitReference(branch, null);
    }

    public static GitReference ofCommit(String commit) {
        return new GitReference(null, commit);
    }

    public boolean isCommit() {
        return commit != null;
    }
}
