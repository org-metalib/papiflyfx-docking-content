package org.metalib.papifly.fx.github.ribbon;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.github.FxTestUtil;
import org.metalib.papifly.fx.github.api.GitHubRibbonActions;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubRibbonProviderFxTest {

    private RibbonManager ribbonManager;

    @Start
    void start(Stage stage) {
        ribbonManager = new RibbonManager(List.of(new GitHubRibbonProvider()));
        stage.setScene(new Scene(new Ribbon(ribbonManager), 1100, 300));
        stage.show();
        settle();
    }

    @Test
    void commandEnablementTracksCapabilityContextRefreshes() {
        assertTrue(FxTestUtil.callFx(() -> ribbonManager.hasTab("github")));
        RibbonCommand fetch = command("github.ribbon.fetch");
        assertFalse(fetch.enabled().get());

        StubActions actions = new StubActions();
        FxTestUtil.runFx(() -> ribbonManager.setContext(new RibbonContext(
            "dock-github",
            "github:toolbar",
            "github-toolbar",
            Map.of(),
            Map.of(GitHubRibbonActions.class, actions)
        )));
        settle();

        RibbonCommand enabledFetch = command("github.ribbon.fetch");
        assertSame(fetch, enabledFetch);
        assertTrue(enabledFetch.enabled().get());

        FxTestUtil.runFx(enabledFetch::execute);
        assertEquals(1, actions.fetchCount);

        FxTestUtil.runFx(() -> ribbonManager.setContext(RibbonContext.empty()));
        settle();

        assertFalse(command("github.ribbon.fetch").enabled().get());
    }

    private RibbonCommand command(String id) {
        return FxTestUtil.callFx(() -> ribbonManager.getCommandRegistry().find(id).orElseThrow());
    }

    private static void settle() {
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static final class StubActions implements GitHubRibbonActions {
        private int fetchCount;

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
        }

        @Override
        public void push() {
        }

        @Override
        public void fetch() {
            fetchCount++;
        }

        @Override
        public void createBranch() {
        }

        @Override
        public void merge() {
        }

        @Override
        public void rebase() {
        }

        @Override
        public void pullRequest() {
        }

        @Override
        public void issues() {
        }

        @Override
        public void commit() {
        }

        @Override
        public void stage() {
        }

        @Override
        public void discard() {
        }
    }
}
