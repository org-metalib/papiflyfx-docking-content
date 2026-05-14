package org.metalib.papifly.fx.github.ribbon;

import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.github.api.GitHubRibbonActions;

import java.util.List;
import java.util.Optional;

/**
 * Ribbon contribution that maps GitHub workflows into command groups.
 */
public final class GitHubRibbonProvider implements RibbonProvider {

    /**
     * Collapse-order constants for GitHub groups. Higher values stay visible
     * longer under adaptive layout (see {@code RibbonGroupSpec#collapseOrder}).
     */
    private static final int COLLAPSE_LATE = 30;
    private static final int COLLAPSE_MID = 20;
    private static final int COLLAPSE_EARLY = 10;

    @Override
    public String id() {
        return "github.ribbon.provider";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public List<RibbonTabSpec> getTabs(RibbonContext context) {
        Optional<GitHubRibbonActions> actions = resolveActions(context);
        return List.of(new RibbonTabSpec(
            "github",
            "GitHub",
            40,
            false,
            ribbonContext -> true,
            List.of(
                syncGroup(actions),
                branchesGroup(actions),
                collaborateGroup(actions),
                stateGroup(actions)
            )
        ));
    }

    private static RibbonGroupSpec syncGroup(Optional<GitHubRibbonActions> actions) {
        return new RibbonGroupSpec(
            "github-sync",
            "Sync",
            0,
            COLLAPSE_LATE,
            null,
            List.of(
                new RibbonButtonSpec(command("pull", "Pull", "Pull and update from remote", "repo-pull", actions, GitHubRibbonActions::canPull, GitHubRibbonActions::pull)),
                new RibbonButtonSpec(command("push", "Push", "Push current branch to remote", "upload", actions, GitHubRibbonActions::canPush, GitHubRibbonActions::push)),
                new RibbonButtonSpec(command("fetch", "Fetch", "Fetch remote references", "sync", actions, GitHubRibbonActions::canFetch, GitHubRibbonActions::fetch))
            )
        );
    }

    private static RibbonGroupSpec branchesGroup(Optional<GitHubRibbonActions> actions) {
        return new RibbonGroupSpec(
            "github-branches",
            "Branches",
            10,
            COLLAPSE_MID,
            null,
            List.of(
                new RibbonButtonSpec(command("new-branch", "New Branch", "Create and checkout a branch", "git-branch", actions, GitHubRibbonActions::canCreateBranch, GitHubRibbonActions::createBranch)),
                new RibbonButtonSpec(command("merge", "Merge", "Merge workflow (coming soon)", "git-merge", actions, GitHubRibbonActions::canMerge, GitHubRibbonActions::merge)),
                new RibbonButtonSpec(command("rebase", "Rebase", "Rebase workflow (coming soon)", "git-merge", actions, GitHubRibbonActions::canRebase, GitHubRibbonActions::rebase))
            )
        );
    }

    private static RibbonGroupSpec collaborateGroup(Optional<GitHubRibbonActions> actions) {
        return new RibbonGroupSpec(
            "github-collaborate",
            "Collaborate",
            20,
            COLLAPSE_EARLY,
            null,
            List.of(
                new RibbonButtonSpec(command("pull-request", "Pull Request", "Create a pull request", "git-pull-request", actions, GitHubRibbonActions::canPullRequest, GitHubRibbonActions::pullRequest)),
                new RibbonButtonSpec(command("issues", "Issues", "Open repository issues in browser", "issue-opened", actions, GitHubRibbonActions::canOpenIssues, GitHubRibbonActions::issues))
            )
        );
    }

    private static RibbonGroupSpec stateGroup(Optional<GitHubRibbonActions> actions) {
        return new RibbonGroupSpec(
            "github-state",
            "State",
            30,
            COLLAPSE_LATE,
            null,
            List.of(
                new RibbonButtonSpec(command("commit", "Commit", "Create a commit", "git-commit", actions, GitHubRibbonActions::canCommit, GitHubRibbonActions::commit)),
                new RibbonButtonSpec(command("stage", "Stage", "Stage workflow (coming soon)", "diff", actions, GitHubRibbonActions::canStage, GitHubRibbonActions::stage)),
                new RibbonButtonSpec(command("discard", "Discard", "Rollback/discard changes", "trash", actions, GitHubRibbonActions::canDiscard, GitHubRibbonActions::discard))
            )
        );
    }

    private static RibbonCommand command(
        String id,
        String label,
        String tooltip,
        String octiconName,
        Optional<GitHubRibbonActions> actions,
        java.util.function.Predicate<GitHubRibbonActions> canRun,
        java.util.function.Consumer<GitHubRibbonActions> run
    ) {
        boolean enabled = actions.map(canRun::test).orElse(false);
        Runnable action = () -> actions.ifPresent(run);
        return RibbonCommand.of(
            "github.ribbon." + id,
            label,
            tooltip,
            icon(octiconName),
            icon(octiconName),
            RibbonBooleanState.mutable(enabled),
            action
        );
    }

    private static RibbonIconHandle icon(String name) {
        return RibbonIconHandle.of("octicon:" + name);
    }

    private static Optional<GitHubRibbonActions> resolveActions(RibbonContext context) {
        if (context == null) {
            return Optional.empty();
        }
        return context.capability(GitHubRibbonActions.class);
    }
}
