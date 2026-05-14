package org.metalib.papifly.fx.github.api;

/**
 * Action contract used by ribbon providers to trigger GitHub workflows without
 * depending on concrete toolbar internals.
 */
public interface GitHubRibbonActions {

    boolean canPull();

    boolean canPush();

    boolean canFetch();

    boolean canCreateBranch();

    boolean canMerge();

    boolean canRebase();

    boolean canPullRequest();

    boolean canOpenIssues();

    boolean canCommit();

    boolean canStage();

    boolean canDiscard();

    void pull();

    void push();

    void fetch();

    void createBranch();

    void merge();

    void rebase();

    void pullRequest();

    void issues();

    void commit();

    void stage();

    void discard();
}
