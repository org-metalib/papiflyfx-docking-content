package org.metalib.papifly.fx.github.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RefPopupEntry;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.TagRef;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GitHubToolbarFxTest {

    private StackPane root;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        stage.setScene(new Scene(root, 1280, 180));
        stage.show();
    }

    @Test
    void rendersToolbarPillsAndContextualChips() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            true,
            2,
            1
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-repo-pill")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-ref-pill")));
        assertNotNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-overflow-button")));
        assertNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-update-button")));
        assertNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-commit-button")));
        assertNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-push-button")));
        assertNull(FxTestUtil.callFx(() -> toolbar.lookup("#github-pr-button")));
        assertEquals("feature/x", refText(toolbar).getText());
        assertTrue(chipTexts(toolbar).contains("Ahead 2"));
        assertTrue(chipTexts(toolbar).contains("Behind 1"));
        assertTrue(refDot(toolbar).getStyleClass().contains("pf-github-ref-dot-dirty"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void toolbarUsesSharedPillChipAndStatusMetrics() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            true,
            2,
            1
        ));
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            root.applyCss();
            root.layout();
        });

        Region repoPill = FxTestUtil.callFx(() -> (Region) toolbar.lookup("#github-repo-pill"));
        Region refPill = FxTestUtil.callFx(() -> (Region) toolbar.lookup("#github-ref-pill"));
        Region firstChip = FxTestUtil.callFx(() -> (Region) ((HBox) toolbar.lookup("#github-chip-strip")).getChildren().getFirst());
        HBox statusSlot = FxTestUtil.callFx(() -> (HBox) toolbar.lookup("#github-status-slot"));
        HBox chipStrip = FxTestUtil.callFx(() -> (HBox) toolbar.lookup("#github-chip-strip"));
        HBox actionBar = FxTestUtil.callFx(() -> (HBox) toolbar.lookup("#github-action-bar"));

        assertTrue(FxTestUtil.callFx(toolbar::getHeight) >= UiMetrics.TOOLBAR_HEIGHT);
        assertEquals(UiMetrics.CONTROL_HEIGHT_REGULAR, FxTestUtil.callFx(repoPill::getHeight), 1.0);
        assertEquals(UiMetrics.CONTROL_HEIGHT_REGULAR, FxTestUtil.callFx(refPill::getHeight), 1.0);
        assertEquals(UiMetrics.CONTROL_HEIGHT_COMPACT, FxTestUtil.callFx(firstChip::getHeight), 1.0);
        assertEquals(UiMetrics.SPACE_2, FxTestUtil.callFx(statusSlot::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_2, FxTestUtil.callFx(chipStrip::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_2, FxTestUtil.callFx(actionBar::getSpacing), 0.01);

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void sharedComponentsRemainStableAcrossCompressedAndWideLayouts() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            true,
            2,
            1
        ));
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            root.getScene().getWindow().setWidth(720.0);
            root.applyCss();
            root.layout();
        });

        Region repoPill = FxTestUtil.callFx(() -> (Region) toolbar.lookup("#github-repo-pill"));
        Region refPill = FxTestUtil.callFx(() -> (Region) toolbar.lookup("#github-ref-pill"));
        HBox chipStrip = FxTestUtil.callFx(() -> (HBox) toolbar.lookup("#github-chip-strip"));
        assertTrue(FxTestUtil.callFx(repoPill::isVisible));
        assertTrue(FxTestUtil.callFx(refPill::isVisible));
        assertTrue(FxTestUtil.callFx(chipStrip::isVisible));

        FxTestUtil.runFx(() -> {
            root.getScene().getWindow().setWidth(1440.0);
            root.applyCss();
            root.layout();
        });

        assertEquals(UiMetrics.CONTROL_HEIGHT_REGULAR, FxTestUtil.callFx(repoPill::getHeight), 1.0);
        assertEquals(UiMetrics.CONTROL_HEIGHT_REGULAR, FxTestUtil.callFx(refPill::getHeight), 1.0);
        assertTrue(FxTestUtil.callFx(chipStrip::getWidth) > 0.0);

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void repoPillPressedStateUsesSharedPressedBackground() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ), Theme.dark());
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(toolbar);
            root.applyCss();
            root.layout();
        });

        Region repoPill = FxTestUtil.callFx(() -> (Region) toolbar.lookup("#github-repo-pill"));
        Color defaultBackground = FxTestUtil.callFx(() -> backgroundColor(repoPill));

        FxTestUtil.runFx(() -> {
            ((javafx.scene.control.Button) repoPill).arm();
            repoPill.applyCss();
            repoPill.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Color pressedBackground = FxTestUtil.callFx(() -> backgroundColor(repoPill));
        Color expectedPressed = (Color) GitHubToolbarThemeMapper.map(Theme.dark()).controlBackgroundPressed();

        assertNotNull(defaultBackground);
        assertNotNull(pressedBackground);
        assertFalse(colorsClose(defaultBackground, pressedBackground, 0.01));
        assertTrue(colorsClose(expectedPressed, pressedBackground, 0.03));

        FxTestUtil.runFx(() -> {
            ((javafx.scene.control.Button) repoPill).disarm();
            toolbar.close();
        });
    }

    @Test
    void remoteOnlyHidesLocalMenuActionsAndShowsRemoteOnlyChip(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            true,
            "main",
            GitRefKind.REMOTE_BRANCH,
            "main",
            false,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-overflow-button");
        WaitForAsyncUtils.waitForFxEvents();

        List<String> overflowItems = overflowItemTexts(robot);

        assertFalse(overflowItems.contains("Update"));
        assertFalse(overflowItems.contains("Commit..."));
        assertFalse(overflowItems.contains("Push"));
        assertFalse(overflowItems.contains("New Branch..."));
        assertFalse(overflowItems.contains("Rollback..."));
        assertTrue(overflowItems.contains("Pull Request..."));
        assertTrue(overflowItems.contains("Set Token"));
        assertTrue(chipTexts(toolbar).contains("Remote only"));
        assertTrue(chipTexts(toolbar).contains("No token"));
        assertTrue(refDot(toolbar).getStyleClass().contains("pf-github-ref-dot-neutral"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void defaultBranchDisablesCommitInLowerFrequencyActionsAndShowsDefaultChip(FxRobot robot) {
        GitHubToolbarViewModel viewModel = createViewModel(new ToolbarState(
            false,
            "main",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        GitHubToolbar toolbar = FxTestUtil.callFx(() -> new GitHubToolbar(viewModel));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-overflow-button");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(viewModel.commitDisabledProperty().get());
        assertTrue(chipTexts(toolbar).contains("Default"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void detachedTagRenderingShowsDetachedChip() {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "v0.9.0",
            GitRefKind.TAG,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        assertEquals("v0.9.0", refText(toolbar).getText());
        assertTrue(chipTexts(toolbar).contains("Detached"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void pushAndPullRequestDisabledUntilTokenProvided(FxRobot robot) {
        GitHubToolbarViewModel viewModel = createViewModel(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            false,
            false,
            0,
            0
        ));
        GitHubToolbar toolbar = FxTestUtil.callFx(() -> new GitHubToolbar(viewModel));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-overflow-button");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(overflowItemTexts(robot).contains("Push"));
        assertTrue(overflowItemTexts(robot).contains("Pull Request..."));
        assertTrue(viewModel.pushDisabledProperty().get());
        assertTrue(viewModel.pullRequestDisabledProperty().get());
        assertTrue(chipTexts(toolbar).contains("No token"));

        FxTestUtil.runFx(() -> viewModel.saveToken("token"));
        WaitForAsyncUtils.waitForFxEvents();
        robot.type(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#github-overflow-button");
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(viewModel.pushDisabledProperty().get());
        assertFalse(viewModel.pullRequestDisabledProperty().get());
        assertFalse(chipTexts(toolbar).contains("No token"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void refPillKeyboardShortcutOpensAndClosesPopup(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        FxTestUtil.runFx(() -> toolbar.lookup("#github-ref-pill").requestFocus());
        robot.press(KeyCode.ALT);
        robot.type(KeyCode.DOWN);
        robot.release(KeyCode.ALT);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(robot.lookup("#github-ref-popup").tryQuery().isPresent());
        assertTrue(robot.lookup("#github-ref-popup-search").queryAs(javafx.scene.control.TextField.class).isFocused());

        robot.type(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(robot.lookup("#github-ref-popup").tryQuery().isPresent());
        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void popupSearchFilteringAndRemoteSubmenu(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-ref-pill");
        WaitForAsyncUtils.waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<Object> listView = robot.lookup("#github-ref-popup-list").queryAs(ListView.class);
        int initialSize = FxTestUtil.callFx(() -> listView.getItems().size());

        robot.clickOn("#github-ref-popup-search");
        robot.write("release");
        WaitForAsyncUtils.waitForFxEvents();

        int filteredSize = FxTestUtil.callFx(() -> listView.getItems().size());
        assertTrue(filteredSize < initialSize);
        assertTrue(primaryTexts(robot).contains("release"));

        FxTestUtil.runFx(listView::requestFocus);
        robot.type(KeyCode.RIGHT);
        WaitForAsyncUtils.waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<RefPopupEntry.Action> submenuList = robot.lookup("#github-ref-submenu-list").queryAs(ListView.class);
        assertEquals("Checkout and Track", FxTestUtil.callFx(() -> submenuList.getItems().getFirst().primaryText()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void popupAllowsSingleClickRefActivationWhileSearchIsFocused(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-ref-pill");
        robot.clickOn("v0.9.0");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("v0.9.0", refText(toolbar).getText());
        assertFalse(robot.lookup("#github-ref-popup").tryQuery().isPresent());

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void popupShowsOnlyRefsAndNoCommandRows(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-ref-pill");
        WaitForAsyncUtils.waitForFxEvents();

        List<String> popupEntries = primaryTexts(robot);
        assertFalse(popupEntries.contains("Update"));
        assertFalse(popupEntries.contains("Commit..."));
        assertFalse(popupEntries.contains("Push"));
        assertFalse(popupEntries.contains("New Branch..."));
        assertFalse(popupEntries.contains("Checkout Tag or Revision..."));
        assertTrue(popupEntries.contains("main"));
        assertTrue(popupEntries.contains("release"));
        assertTrue(popupEntries.contains("v0.9.0"));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void overflowAllowsSingleClickActionActivation(FxRobot robot) {
        GitHubToolbar toolbar = createToolbar(new ToolbarState(
            false,
            "feature/x",
            GitRefKind.LOCAL_BRANCH,
            "main",
            true,
            false,
            0,
            0
        ));
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-overflow-button");
        robot.clickOn("Update");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Repository updated", FxTestUtil.callFx(() -> ((Label) toolbar.lookup("#github-status-text")).getText()));

        FxTestUtil.runFx(toolbar::close);
    }

    @Test
    void exportReviewSnapshotsWhenRequested(FxRobot robot) throws IOException {
        if (!Boolean.getBoolean("papiflyfx.review.snapshots")) {
            return;
        }

        Path snapshotDirectory = resolveSnapshotDirectory();
        Files.createDirectories(snapshotDirectory);

        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-local-clean-dark.png"),
            new ToolbarState(false, "feature/x", GitRefKind.LOCAL_BRANCH, "main", true, false, 2, 1),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-local-clean-light.png"),
            new ToolbarState(false, "feature/x", GitRefKind.LOCAL_BRANCH, "main", true, false, 2, 1),
            Theme.light()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-dirty-branch.png"),
            new ToolbarState(false, "feature/x", GitRefKind.LOCAL_BRANCH, "main", true, true, 0, 0),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-default-branch.png"),
            new ToolbarState(false, "main", GitRefKind.LOCAL_BRANCH, "main", true, false, 0, 0),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-detached-tag.png"),
            new ToolbarState(false, "v0.9.0", GitRefKind.TAG, "main", true, false, 0, 0),
            Theme.dark()
        );
        writeSnapshot(
            snapshotDirectory.resolve("github-toolbar-remote-only.png"),
            new ToolbarState(true, "main", GitRefKind.REMOTE_BRANCH, "main", false, false, 0, 0),
            Theme.dark()
        );
        writePopupSnapshot(
            robot,
            snapshotDirectory.resolve("github-toolbar-popup-open.png"),
            new ToolbarState(false, "feature/x", GitRefKind.LOCAL_BRANCH, "main", true, false, 0, 0),
            Theme.dark()
        );
    }

    private GitHubToolbar createToolbar(ToolbarState state) {
        return createToolbar(state, null);
    }

    private GitHubToolbar createToolbar(ToolbarState state, Theme initialTheme) {
        GitHubToolbarViewModel viewModel = createViewModel(state);
        return FxTestUtil.callFx(() -> initialTheme == null ? new GitHubToolbar(viewModel) : new GitHubToolbar(viewModel, initialTheme));
    }

    private GitHubToolbarViewModel createViewModel(ToolbarState state) {
        PatCredentialStore store = new PatCredentialStore();
        if (state.authenticated()) {
            store.setToken("token");
        }

        GitHubRepoContext context = state.remoteOnly()
            ? GitHubRepoContext.remoteOnly(URI.create("https://github.com/org/repo"))
            : GitHubRepoContext.of(URI.create("https://github.com/org/repo"), Path.of("."));

        GitRepository repository = state.remoteOnly() ? null : new FakeGitRepository(state);
        GitHubApiService apiService = new FakeGitHubApiService(state.defaultBranch());

        return new GitHubToolbarViewModel(context, store, repository, apiService, new CommandRunner(true));
    }

    private void writeSnapshot(Path path, ToolbarState state, Theme theme) throws IOException {
        GitHubToolbar toolbar = createToolbar(state, theme);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        WritableImage image = FxTestUtil.callFx(() -> toolbar.snapshot(null, null));
        writeImage(path, image);

        FxTestUtil.runFx(() -> {
            root.getChildren().clear();
            toolbar.close();
        });
    }

    private void writePopupSnapshot(FxRobot robot, Path path, ToolbarState state, Theme theme) throws IOException {
        GitHubToolbar toolbar = createToolbar(state, theme);
        FxTestUtil.runFx(() -> root.getChildren().setAll(toolbar));

        robot.clickOn("#github-ref-pill");
        WaitForAsyncUtils.waitForFxEvents();

        Node popupNode = robot.lookup("#github-ref-popup").query();
        WritableImage image = FxTestUtil.callFx(() -> popupNode.snapshot(null, null));
        writeImage(path, image);

        FxTestUtil.runFx(() -> {
            root.getChildren().clear();
            toolbar.close();
        });
    }

    private static void writeImage(Path path, WritableImage image) throws IOException {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }
        ImageIO.write(bufferedImage, "png", path.toFile());
    }

    private static Path resolveSnapshotDirectory() {
        String configured = System.getProperty("papiflyfx.review.snapshotDir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path base = Path.of("").toAbsolutePath().normalize();
        Path repoRoot = "papiflyfx-docking-github".equals(base.getFileName().toString()) ? base.getParent() : base;
        return repoRoot.resolve("spec/papiflyfx-docking-github/review1-ui");
    }

    private static List<String> chipTexts(GitHubToolbar toolbar) {
        return FxTestUtil.callFx(() -> {
            HBox chipStrip = (HBox) toolbar.lookup("#github-chip-strip");
            List<String> texts = new ArrayList<>();
            for (Node child : chipStrip.getChildren()) {
                if (child instanceof Label label) {
                    texts.add(label.getText());
                }
            }
            return texts;
        });
    }

    private static Circle refDot(GitHubToolbar toolbar) {
        return FxTestUtil.callFx(() -> {
            Node refPill = toolbar.lookup("#github-ref-pill");
            refPill.applyCss();
            return (Circle) refPill.lookup(".pf-github-ref-dot");
        });
    }

    private static Label refText(GitHubToolbar toolbar) {
        return FxTestUtil.callFx(() -> {
            Node refPill = toolbar.lookup("#github-ref-pill");
            refPill.applyCss();
            return (Label) refPill.lookup(".pf-github-ref-text");
        });
    }

    private static List<String> primaryTexts(FxRobot robot) {
        return robot.lookup(".pf-github-ref-popup-primary")
            .queryAllAs(Label.class)
            .stream()
            .map(Label::getText)
            .toList();
    }

    private static Color backgroundColor(Region region) {
        if (region.getBackground() == null || region.getBackground().getFills().isEmpty()) {
            return null;
        }
        if (region.getBackground().getFills().getFirst().getFill() instanceof Color color) {
            return color;
        }
        return null;
    }

    private static boolean colorsClose(Color expected, Color actual, double tolerance) {
        return Math.abs(expected.getRed() - actual.getRed()) <= tolerance
            && Math.abs(expected.getGreen() - actual.getGreen()) <= tolerance
            && Math.abs(expected.getBlue() - actual.getBlue()) <= tolerance
            && Math.abs(expected.getOpacity() - actual.getOpacity()) <= tolerance;
    }

    private static List<String> overflowItemTexts(FxRobot robot) {
        return robot.lookup(".menu-item")
            .queryAll()
            .stream()
            .map(GitHubToolbarFxTest::menuItemText)
            .filter(text -> !text.isBlank())
            .toList();
    }

    private static String menuItemText(Node node) {
        if (!(node instanceof Parent parent)) {
            return "";
        }
        Node labelNode = parent.lookup(".label");
        return labelNode instanceof Label label ? label.getText() : "";
    }

    private record ToolbarState(
        boolean remoteOnly,
        String currentRefName,
        GitRefKind currentRefKind,
        String defaultBranch,
        boolean authenticated,
        boolean dirty,
        int aheadCount,
        int behindCount
    ) {
    }

    private static final class FakeGitHubApiService extends GitHubApiService {

        private final String defaultBranch;

        private FakeGitHubApiService(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }

        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            return defaultBranch;
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(7, URI.create("https://github.com/org/repo/pull/7"));
        }
    }

    private static final class FakeGitRepository implements GitRepository {

        private String currentRefName;
        private GitRefKind currentRefKind;
        private final String defaultBranch;
        private boolean dirty;
        private final int aheadCount;
        private final int behindCount;

        private FakeGitRepository(ToolbarState state) {
            this.currentRefName = state.currentRefName();
            this.currentRefKind = state.currentRefKind();
            this.defaultBranch = state.defaultBranch();
            this.dirty = state.dirty();
            this.aheadCount = state.aheadCount();
            this.behindCount = state.behindCount();
        }

        @Override
        public RepoStatus loadStatus() {
            return new RepoStatus(
                currentRefDisplayName(),
                defaultBranch,
                currentRefKind == GitRefKind.TAG || currentRefKind == GitRefKind.DETACHED_COMMIT,
                aheadCount,
                behindCount,
                dirty ? Set.of("file.txt") : Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
        }

        @Override
        public List<BranchRef> listBranches() {
            List<BranchRef> branches = new ArrayList<>();
            branches.add(localBranch("main"));
            branches.add(localBranch("feature/x"));
            if (currentRefKind == GitRefKind.LOCAL_BRANCH && "release".equals(currentRefName)) {
                branches.add(localBranch("release"));
            }
            branches.add(new BranchRef("release", "refs/remotes/origin/release", false, true, false, "origin", ""));
            return branches;
        }

        @Override
        public List<TagRef> listTags() {
            return List.of(new TagRef("v0.9.0", "refs/tags/v0.9.0", currentRefKind == GitRefKind.TAG && "v0.9.0".equals(currentRefName)));
        }

        @Override
        public CurrentRefState loadCurrentRef() {
            return switch (currentRefKind) {
                case LOCAL_BRANCH -> new CurrentRefState(
                    currentRefName,
                    "refs/heads/" + currentRefName,
                    GitRefKind.LOCAL_BRANCH,
                    "origin/" + currentRefName,
                    dirty ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
                    currentRefName.equals(defaultBranch),
                    false,
                    false
                );
                case TAG -> new CurrentRefState(
                    currentRefName,
                    "refs/tags/" + currentRefName,
                    GitRefKind.TAG,
                    "",
                    dirty ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
                    false,
                    true,
                    false
                );
                case DETACHED_COMMIT -> new CurrentRefState(
                    currentRefName,
                    "abcdef1234567890abcdef1234567890abcdef12",
                    GitRefKind.DETACHED_COMMIT,
                    "",
                    dirty ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
                    false,
                    true,
                    false
                );
                case REMOTE_BRANCH -> new CurrentRefState(
                    currentRefName,
                    "refs/remotes/origin/" + currentRefName,
                    GitRefKind.REMOTE_BRANCH,
                    "",
                    CurrentRefState.StatusDotState.NEUTRAL,
                    currentRefName.equals(defaultBranch),
                    false,
                    false
                );
            };
        }

        @Override
        public void checkoutRef(String refName, GitRefKind kind, boolean force) {
            dirty = false;
            switch (kind) {
                case LOCAL_BRANCH -> {
                    currentRefKind = GitRefKind.LOCAL_BRANCH;
                    currentRefName = shorten(refName, "refs/heads/");
                }
                case REMOTE_BRANCH -> {
                    currentRefKind = GitRefKind.LOCAL_BRANCH;
                    currentRefName = refName.substring(refName.lastIndexOf('/') + 1);
                }
                case TAG -> {
                    currentRefKind = GitRefKind.TAG;
                    currentRefName = shorten(refName, "refs/tags/");
                }
                case DETACHED_COMMIT -> {
                    currentRefKind = GitRefKind.DETACHED_COMMIT;
                    currentRefName = refName.length() > 7 ? refName.substring(0, 7) : refName;
                }
            }
        }

        @Override
        public void checkout(String branchName, boolean force) {
            checkoutRef(branchName, GitRefKind.LOCAL_BRANCH, force);
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
            currentRefKind = GitRefKind.LOCAL_BRANCH;
            currentRefName = branchName;
            dirty = false;
        }

        @Override
        public CommitInfo commitAll(String message) {
            dirty = false;
            return new CommitInfo("abcdef123", "abcdef1", message, "tester", Instant.now());
        }

        @Override
        public CommitInfo getHeadCommit() {
            return new CommitInfo("abcdef123", "abcdef1", "head", "tester", Instant.now());
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
            return defaultBranch;
        }

        @Override
        public void close() {
        }

        private BranchRef localBranch(String name) {
            return new BranchRef(
                name,
                "refs/heads/" + name,
                true,
                false,
                currentRefKind == GitRefKind.LOCAL_BRANCH && name.equals(currentRefName),
                "",
                "origin/" + name
            );
        }

        private String currentRefDisplayName() {
            return currentRefName;
        }

        private static String shorten(String value, String prefix) {
            return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        }
    }
}
