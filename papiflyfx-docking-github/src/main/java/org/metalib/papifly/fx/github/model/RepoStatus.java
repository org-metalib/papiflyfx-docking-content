package org.metalib.papifly.fx.github.model;

import java.util.Objects;
import java.util.Set;

public record RepoStatus(
    String currentBranch,
    String defaultBranch,
    boolean detachedHead,
    int aheadCount,
    int behindCount,
    Set<String> added,
    Set<String> changed,
    Set<String> removed,
    Set<String> missing,
    Set<String> modified,
    Set<String> untracked
) {

    public RepoStatus {
        Objects.requireNonNull(currentBranch, "currentBranch");
        Objects.requireNonNull(defaultBranch, "defaultBranch");
        Objects.requireNonNull(added, "added");
        Objects.requireNonNull(changed, "changed");
        Objects.requireNonNull(removed, "removed");
        Objects.requireNonNull(missing, "missing");
        Objects.requireNonNull(modified, "modified");
        Objects.requireNonNull(untracked, "untracked");
    }

    public boolean dirty() {
        return !(added.isEmpty()
            && changed.isEmpty()
            && removed.isEmpty()
            && missing.isEmpty()
            && modified.isEmpty()
            && untracked.isEmpty());
    }
}
