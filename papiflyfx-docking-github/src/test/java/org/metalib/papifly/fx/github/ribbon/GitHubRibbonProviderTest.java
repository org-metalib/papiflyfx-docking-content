package org.metalib.papifly.fx.github.ribbon;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.github.api.GitHubRibbonActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubRibbonProviderTest {

    private static final String LEGACY_ACTIVE_CONTENT_NODE = "activeContentNode";

    @Test
    void mapsGroupsAndRoutesCommandsToActions() {
        StubActions actions = new StubActions();
        RibbonContext context = new RibbonContext(
            "dock-1",
            "github:toolbar",
            "github-toolbar",
            Map.of(),
            Map.of(GitHubRibbonActions.class, actions)
        );
        GitHubRibbonProvider provider = new GitHubRibbonProvider();

        assertFalse(context.attribute(LEGACY_ACTIVE_CONTENT_NODE).isPresent());
        List<RibbonTabSpec> tabs = provider.getTabs(context);
        assertEquals(1, tabs.size());
        RibbonTabSpec tab = tabs.getFirst();
        assertEquals("github", tab.id());
        assertEquals(List.of("Sync", "Branches", "Collaborate", "State"), tab.groups().stream().map(RibbonGroupSpec::label).toList());

        command(tab, "github.ribbon.pull").execute();
        command(tab, "github.ribbon.push").execute();
        command(tab, "github.ribbon.fetch").execute();
        command(tab, "github.ribbon.new-branch").execute();
        command(tab, "github.ribbon.pull-request").execute();
        command(tab, "github.ribbon.issues").execute();
        command(tab, "github.ribbon.commit").execute();
        command(tab, "github.ribbon.discard").execute();

        assertEquals(1, actions.pullCount);
        assertEquals(1, actions.pushCount);
        assertEquals(1, actions.fetchCount);
        assertEquals(1, actions.newBranchCount);
        assertEquals(1, actions.pullRequestCount);
        assertEquals(1, actions.issuesCount);
        assertEquals(1, actions.commitCount);
        assertEquals(1, actions.discardCount);

        assertFalse(command(tab, "github.ribbon.merge").enabled().get());
        assertFalse(command(tab, "github.ribbon.rebase").enabled().get());
        assertFalse(command(tab, "github.ribbon.stage").enabled().get());
    }

    @Test
    void disablesCommandsWithoutActiveGitHubActions() {
        GitHubRibbonProvider provider = new GitHubRibbonProvider();
        RibbonTabSpec tab = provider.getTabs(RibbonContext.empty()).getFirst();
        assertFalse(command(tab, "github.ribbon.pull").enabled().get());
        assertFalse(command(tab, "github.ribbon.push").enabled().get());
        assertFalse(command(tab, "github.ribbon.commit").enabled().get());
    }

    @Test
    void resolvesActionsFromTypedCapabilitiesWithoutLegacyNodeAttribute() {
        StubActions actions = new StubActions();
        RibbonContext context = new RibbonContext(
            "dock-1",
            "github:toolbar",
            "github-toolbar",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "GitHub"),
            Map.of(GitHubRibbonActions.class, actions)
        );
        GitHubRibbonProvider provider = new GitHubRibbonProvider();

        assertFalse(context.attribute(LEGACY_ACTIVE_CONTENT_NODE).isPresent());
        RibbonCommand pull = command(provider.getTabs(context).getFirst(), "github.ribbon.pull");

        assertEquals(actions, context.capability(GitHubRibbonActions.class).orElseThrow());
        assertEquals(actions, context.capability(StubActions.class).orElseThrow());
        assertFalse(context.capability(Runnable.class).isPresent());
        assertTrue(pull.enabled().get());

        pull.execute();

        assertEquals(1, actions.pullCount);
    }

    private static RibbonCommand command(RibbonTabSpec tab, String id) {
        return commands(tab).stream()
            .filter(command -> command.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing command: " + id));
    }

    private static List<RibbonCommand> commands(RibbonTabSpec tab) {
        List<RibbonCommand> commands = new ArrayList<>();
        for (RibbonGroupSpec group : tab.groups()) {
            for (RibbonControlSpec control : group.controls()) {
                switch (control) {
                    case RibbonButtonSpec button -> commands.add(button.command());
                    case RibbonToggleSpec toggle -> commands.add(toggle.command());
                    case RibbonSplitButtonSpec splitButton -> {
                        commands.add(splitButton.primaryCommand());
                        commands.addAll(splitButton.secondaryCommands());
                    }
                    case RibbonMenuSpec menu -> commands.addAll(menu.items());
                    default -> {
                    }
                }
            }
        }
        return commands;
    }

    private static final class StubActions extends StackPane implements GitHubRibbonActions {

        private int pullCount;
        private int pushCount;
        private int fetchCount;
        private int newBranchCount;
        private int pullRequestCount;
        private int issuesCount;
        private int commitCount;
        private int discardCount;

        @Override
        public boolean canPull() {
            return true;
        }

        @Override
        public boolean canPush() {
            return true;
        }

        @Override
        public boolean canFetch() {
            return true;
        }

        @Override
        public boolean canCreateBranch() {
            return true;
        }

        @Override
        public boolean canMerge() {
            return false;
        }

        @Override
        public boolean canRebase() {
            return false;
        }

        @Override
        public boolean canPullRequest() {
            return true;
        }

        @Override
        public boolean canOpenIssues() {
            return true;
        }

        @Override
        public boolean canCommit() {
            return true;
        }

        @Override
        public boolean canStage() {
            return false;
        }

        @Override
        public boolean canDiscard() {
            return true;
        }

        @Override
        public void pull() {
            pullCount++;
        }

        @Override
        public void push() {
            pushCount++;
        }

        @Override
        public void fetch() {
            fetchCount++;
        }

        @Override
        public void createBranch() {
            newBranchCount++;
        }

        @Override
        public void merge() {
            throw new AssertionError("merge should not run when disabled");
        }

        @Override
        public void rebase() {
            throw new AssertionError("rebase should not run when disabled");
        }

        @Override
        public void pullRequest() {
            pullRequestCount++;
        }

        @Override
        public void issues() {
            issuesCount++;
        }

        @Override
        public void commit() {
            commitCount++;
        }

        @Override
        public void stage() {
            throw new AssertionError("stage should not run when disabled");
        }

        @Override
        public void discard() {
            discardCount++;
        }
    }
}
