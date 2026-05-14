package org.metalib.papifly.fx.github.git;

import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.TagRef;

import java.util.List;

public interface GitRepository extends AutoCloseable {

    RepoStatus loadStatus();

    List<BranchRef> listBranches();

    default List<TagRef> listTags() {
        return List.of();
    }

    default CurrentRefState loadCurrentRef() {
        RepoStatus status = loadStatus();
        GitRefKind kind = status.detachedHead() ? GitRefKind.DETACHED_COMMIT : GitRefKind.LOCAL_BRANCH;
        CurrentRefState.StatusDotState dotState = status.dirty()
            ? CurrentRefState.StatusDotState.DIRTY
            : CurrentRefState.StatusDotState.CLEAN;
        return new CurrentRefState(
            status.currentBranch(),
            status.currentBranch(),
            kind,
            "",
            dotState,
            status.currentBranch().equals(status.defaultBranch()),
            status.detachedHead(),
            false
        );
    }

    void checkout(String branchName, boolean force);

    default void checkoutRef(String refName, GitRefKind kind, boolean force) {
        checkout(refName, force);
    }

    void createAndCheckout(String branchName, String startPoint);

    CommitInfo commitAll(String message);

    CommitInfo getHeadCommit();

    void rollback(RollbackMode mode);

    void push(String remoteName);

    void update();

    boolean isHeadPushed();

    String detectDefaultBranch();

    @Override
    void close();
}
