package org.metalib.papifly.fx.github.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.FxTestUtil;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.api.GitHubToolbar;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper;
import org.metalib.papifly.fx.github.ui.dialog.TokenDialog;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubToolbarThemeIntegrationTest {

    private StackPane root;
    private ObjectProperty<Theme> themeProperty;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        themeProperty = new SimpleObjectProperty<>(Theme.dark());
        stage.setScene(new Scene(root, 1100, 140));
        stage.show();
    }

    @Test
    void bindThemePropertyAppliesAndSwitchesToolbarVariables() {
        GitHubToolbar toolbar = createToolbar();
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            toolbar.bindThemeProperty(themeProperty);
        });
        WaitForAsyncUtils.waitForFxEvents();

        String darkStyle = FxTestUtil.callFx(toolbar::getStyle);
        assertTrue(darkStyle.contains("-pf-github-toolbar-bg"));

        FxTestUtil.runFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        String lightStyle = FxTestUtil.callFx(toolbar::getStyle);
        assertNotEquals(darkStyle, lightStyle);
        assertTrue(lightStyle.contains("-pf-github-toolbar-bg"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void unbindStopsThemeUpdates() {
        GitHubToolbar toolbar = createToolbar();
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            toolbar.bindThemeProperty(themeProperty);
        });
        WaitForAsyncUtils.waitForFxEvents();

        String darkStyle = FxTestUtil.callFx(toolbar::getStyle);
        FxTestUtil.runFx(toolbar::unbindThemeProperty);
        FxTestUtil.runFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        String updatedStyle = FxTestUtil.callFx(toolbar::getStyle);
        assertTrue(updatedStyle.contains("-pf-github-toolbar-bg"));
        assertNotEquals("", updatedStyle);
        org.junit.jupiter.api.Assertions.assertEquals(darkStyle, updatedStyle);

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void dialogsReceiveThemeStyles() {
        TokenDialog dialog = FxTestUtil.callFx(() -> new TokenDialog(Theme.light()));

        assertTrue(dialog.getDialogPane().getStyleClass().contains("pf-github-dialog-pane"));
        assertTrue(dialog.getDialogPane().getStyle().contains("-pf-github-toolbar-bg"));
    }

    @Test
    void popupReceivesThemeStyles(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar();
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            toolbar.bindThemeProperty(themeProperty);
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#github-ref-pill");
        WaitForAsyncUtils.waitForFxEvents();

        javafx.scene.Node popup = robot.lookup("#github-ref-popup").query();
        String popupStyle = FxTestUtil.callFx(popup::getStyle);

        assertTrue(popupStyle.contains("-pf-github-toolbar-bg"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void overflowMenuFollowsCurrentTheme(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar();
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            toolbar.bindThemeProperty(themeProperty);
        });
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#github-overflow-button");
        WaitForAsyncUtils.waitForFxEvents();

        Parent overflowRoot = overflowMenuRoot(robot);
        String darkStyle = FxTestUtil.callFx(overflowRoot::getStyle);
        assertTrue(darkStyle.contains("-pf-github-toolbar-bg"));
        assertMenuColors(overflowRoot, Theme.dark());

        FxTestUtil.runFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        String lightStyle = FxTestUtil.callFx(overflowRoot::getStyle);
        assertNotEquals(darkStyle, lightStyle);
        assertTrue(lightStyle.contains("-pf-github-toolbar-bg"));
        assertMenuColors(overflowRoot, Theme.light());

        FxTestUtil.runFx(toolbar::close);
    }

    private GitHubToolbar createToolbar() {
        PatCredentialStore store = new PatCredentialStore();
        store.setToken("token");
        GitHubRepoContext context = GitHubRepoContext.of(URI.create("https://github.com/org/repo"), Path.of("."));
        GitRepository repository = new FakeGitRepository();
        GitHubApiService apiService = new FakeGitHubApiService();
        GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(context, store, repository, apiService, new CommandRunner(true));
        return FxTestUtil.callFx(() -> new GitHubToolbar(viewModel, Theme.dark()));
    }

    private static void assertMenuColors(Parent overflowRoot, Theme theme) {
        Color expectedText = requireColor(GitHubToolbarThemeMapper.map(theme).textPrimary());
        Label itemLabel = FxTestUtil.callFx(() -> (Label) overflowRoot.lookup(".menu-item .label"));
        assertNotNull(itemLabel);
        Color actualText = FxTestUtil.callFx(() -> requireColor(itemLabel.getTextFill()));

        assertColorEquals(expectedText, actualText);
    }

    private static Parent overflowMenuRoot(FxRobot robot) {
        Node item = robot.lookup(".menu-item").query();
        return FxTestUtil.callFx(() -> item.getScene().getRoot());
    }

    private static Color requireColor(Paint paint) {
        assertTrue(paint instanceof Color);
        return (Color) paint;
    }

    private static void assertColorEquals(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 0.01);
        assertEquals(expected.getGreen(), actual.getGreen(), 0.01);
        assertEquals(expected.getBlue(), actual.getBlue(), 0.01);
        assertEquals(expected.getOpacity(), actual.getOpacity(), 0.01);
    }

    private static final class FakeGitHubApiService extends GitHubApiService {
        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            return "main";
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(4, URI.create("https://github.com/org/repo/pull/4"));
        }
    }

    private static final class FakeGitRepository implements GitRepository {
        @Override
        public RepoStatus loadStatus() {
            return new RepoStatus(
                "feature/x",
                "main",
                false,
                1,
                0,
                Set.of("file.txt"),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
        }

        @Override
        public List<BranchRef> listBranches() {
            return List.of(
                new BranchRef("main", "refs/heads/main", true, false, false),
                new BranchRef("feature/x", "refs/heads/feature/x", true, false, true)
            );
        }

        @Override
        public void checkout(String branchName, boolean force) {
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
        }

        @Override
        public CommitInfo commitAll(String message) {
            return new CommitInfo("abcdef123456", "abcdef1", message, "tester", Instant.now());
        }

        @Override
        public CommitInfo getHeadCommit() {
            return new CommitInfo("abcdef123456", "abcdef1", "head", "tester", Instant.now());
        }

        @Override
        public void rollback(RollbackMode mode) {
        }

        @Override
        public void push(String remoteName) {
        }

        @Override
        public void update() {
        }

        @Override
        public boolean isHeadPushed() {
            return false;
        }

        @Override
        public String detectDefaultBranch() {
            return "main";
        }

        @Override
        public void close() {
        }
    }
}
