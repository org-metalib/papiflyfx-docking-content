package org.metalib.papifly.fx.github.api;

import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.git.JGitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RefPopupEntry;
import org.metalib.papifly.fx.github.model.SecondaryChip;
import org.metalib.papifly.fx.github.model.StatusMessage;
import org.metalib.papifly.fx.github.ui.CommandRunner;
import org.metalib.papifly.fx.github.ui.GitHubToolbarViewModel;
import org.metalib.papifly.fx.github.ui.dialog.CommitDialog;
import org.metalib.papifly.fx.github.ui.dialog.DirtyCheckoutAlert;
import org.metalib.papifly.fx.github.ui.dialog.NewBranchDialog;
import org.metalib.papifly.fx.github.ui.dialog.PullRequestDialog;
import org.metalib.papifly.fx.github.ui.dialog.RollbackDialog;
import org.metalib.papifly.fx.github.ui.dialog.TokenDialog;
import org.metalib.papifly.fx.github.ui.popup.GitRefPopup;
import org.metalib.papifly.fx.github.ui.popup.GitRefPopupController;
import org.metalib.papifly.fx.github.ui.theme.GitHubThemeSupport;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarTheme;
import org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper;
import org.metalib.papifly.fx.github.ui.toolbar.RefPill;
import org.metalib.papifly.fx.ui.UiChipLabel;
import org.metalib.papifly.fx.ui.UiChipVariant;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiPillButton;
import org.metalib.papifly.fx.ui.UiStatusSlot;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GitHubToolbar extends HBox implements GitHubRibbonActions, AutoCloseable {

    public static final String FACTORY_ID = "github-toolbar";
    private static final PseudoClass SHOWING_PSEUDO_CLASS = PseudoClass.getPseudoClass("showing");

    private final GitHubToolbarViewModel viewModel;

    private final Label brandBadge;
    private final Button repoPill;
    private final RefPill refPill;
    private final HBox chipStrip;
    private final HBox actionBar;
    private final Button overflowButton;
    private final ContextMenu overflowMenu;
    private final HBox statusSlot;
    private final ProgressIndicator busyIndicator;
    private final Label statusLabel;
    private final Label errorLabel;
    private final GitRefPopup refPopup;
    private final GitRefPopupController refPopupController;
    private final PauseTransition successClearPause;

    private ObjectProperty<Theme> themeProperty;
    private ChangeListener<Theme> themeListener;
    private Theme currentBaseTheme;
    private GitHubToolbarTheme currentTheme;

    public GitHubToolbar(GitHubRepoContext context) {
        this(context, new PatCredentialStore(), null);
    }

    public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore) {
        this(context, credentialStore, null);
    }

    public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore, Theme initialTheme) {
        this(buildViewModel(context, credentialStore), initialTheme);
    }

    public GitHubToolbar(GitHubToolbarViewModel viewModel) {
        this(viewModel, null);
    }

    public GitHubToolbar(GitHubToolbarViewModel viewModel, Theme initialTheme) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");

        GitHubThemeSupport.ensureStylesheetLoaded(this, GitHubThemeSupport.TOOLBAR_STYLESHEET);
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        getStyleClass().add("pf-github-toolbar");
        setId("github-toolbar");
        setAlignment(Pos.CENTER_LEFT);

        brandBadge = new UiChipLabel("GitHub", UiChipVariant.ACCENT);
        brandBadge.setId("github-brand-badge");
        brandBadge.getStyleClass().add("pf-github-brand-badge");

        repoPill = new UiPillButton(viewModel.context().owner() + "/" + viewModel.context().repo());
        repoPill.setId("github-repo-pill");
        repoPill.getStyleClass().add("pf-github-repo-pill");
        repoPill.setMnemonicParsing(false);
        repoPill.setOnAction(event -> openBrowser(viewModel.context().remoteUrl()));

        refPill = new RefPill();

        chipStrip = new HBox();
        chipStrip.setId("github-chip-strip");
        chipStrip.getStyleClass().add("pf-github-chip-strip");
        chipStrip.setAlignment(Pos.CENTER_LEFT);
        bindManagedToVisible(chipStrip);

        overflowButton = createActionButton("...", "github-overflow-button");
        overflowButton.getStyleClass().add("pf-github-overflow-button");
        overflowButton.setOnAction(event -> toggleOverflowMenu());

        overflowMenu = new ContextMenu();
        overflowMenu.getStyleClass().add("pf-github-overflow-menu");
        overflowMenu.setOnShowing(event -> {
            overflowButton.pseudoClassStateChanged(SHOWING_PSEUDO_CLASS, true);
            refreshOverflowMenu();
        });
        overflowMenu.setOnShown(event -> applyOverflowMenuTheme());
        overflowMenu.setOnHidden(event -> overflowButton.pseudoClassStateChanged(SHOWING_PSEUDO_CLASS, false));

        actionBar = new HBox(overflowButton);
        actionBar.setId("github-action-bar");
        actionBar.getStyleClass().add("pf-github-action-bar");
        actionBar.setAlignment(Pos.CENTER_LEFT);

        busyIndicator = new ProgressIndicator();
        busyIndicator.setId("github-busy-indicator");
        busyIndicator.getStyleClass().add("pf-github-busy-indicator");
        busyIndicator.setPrefSize(UiMetrics.SPACE_4, UiMetrics.SPACE_4);
        busyIndicator.setMaxSize(UiMetrics.SPACE_4, UiMetrics.SPACE_4);

        statusLabel = new Label();
        statusLabel.setId("github-status-text");
        statusLabel.getStyleClass().addAll("pf-github-status-label", "pf-ui-status-text");
        statusLabel.textProperty().bind(viewModel.statusTextProperty());

        errorLabel = new UiChipLabel("", UiChipVariant.DANGER);
        errorLabel.setId("github-error-text");
        errorLabel.getStyleClass().add("pf-github-error-label");
        errorLabel.textProperty().bind(viewModel.errorTextProperty());

        statusSlot = new UiStatusSlot(busyIndicator, statusLabel, errorLabel);
        statusSlot.setId("github-status-slot");
        statusSlot.getStyleClass().add("pf-github-status-slot");
        bindManagedToVisible(statusSlot);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().setAll(brandBadge, repoPill, refPill, chipStrip, spacer, actionBar, statusSlot);

        refPopup = new GitRefPopup();
        refPopupController = new GitRefPopupController(viewModel, refPopup, this::onPopupAction, this::onPopupRefActivated);

        successClearPause = new PauseTransition(Duration.seconds(2));
        successClearPause.setOnFinished(event -> viewModel.clearStatusMessage());

        overflowButton.disableProperty().bind(viewModel.busyProperty());

        refPill.setOnAction(event -> refPopupController.toggle(refPill));
        refPill.addEventFilter(KeyEvent.KEY_PRESSED, this::handleRefPillKeyPressed);

        viewModel.currentRefStateProperty().addListener((obs, oldValue, newValue) -> refreshRefPill());
        viewModel.secondaryChipsProperty().addListener((ListChangeListener<SecondaryChip>) change -> refreshChips());
        viewModel.statusMessageProperty().addListener((obs, oldValue, newValue) -> refreshStatusSlot());
        viewModel.localAvailableProperty().addListener((obs, oldValue, newValue) -> refreshOverflowMenu());
        viewModel.authenticatedProperty().addListener((obs, oldValue, newValue) -> refreshOverflowMenu());

        applyTheme(initialTheme == null ? Theme.dark() : initialTheme);
        refreshRefPill();
        refreshChips();
        refreshStatusSlot();
        refreshOverflowMenu();
        viewModel.refresh();
    }

    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        this.themeProperty = themeProperty;
        if (themeProperty == null) {
            return;
        }
        themeListener = (obs, oldTheme, newTheme) -> applyTheme(newTheme);
        themeProperty.addListener(themeListener);
        applyTheme(themeProperty.get());
    }

    public void unbindThemeProperty() {
        if (themeProperty != null && themeListener != null) {
            themeProperty.removeListener(themeListener);
        }
        themeProperty = null;
        themeListener = null;
    }

    public GitHubRepoContext context() {
        return viewModel.context();
    }

    public Map<String, Object> captureState() {
        String localPath = context().localClonePath() == null ? "" : context().localClonePath().toString();
        return Map.of(
            "remoteUrl", context().remoteUrl().toString(),
            "localClonePath", localPath,
            "owner", context().owner(),
            "repo", context().repo()
        );
    }

    @Override
    public boolean canPull() {
        return !viewModel.updateDisabledProperty().get();
    }

    @Override
    public boolean canPush() {
        return !viewModel.pushDisabledProperty().get();
    }

    @Override
    public boolean canFetch() {
        return canPull();
    }

    @Override
    public boolean canCreateBranch() {
        return viewModel.localAvailableProperty().get() && !viewModel.busyProperty().get();
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
        return !viewModel.pullRequestDisabledProperty().get();
    }

    @Override
    public boolean canOpenIssues() {
        return true;
    }

    @Override
    public boolean canCommit() {
        return !viewModel.commitDisabledProperty().get();
    }

    @Override
    public boolean canStage() {
        return false;
    }

    @Override
    public boolean canDiscard() {
        return viewModel.localAvailableProperty().get() && !viewModel.busyProperty().get();
    }

    @Override
    public void pull() {
        viewModel.updateRepository();
    }

    @Override
    public void push() {
        viewModel.push();
    }

    @Override
    public void fetch() {
        viewModel.updateRepository();
    }

    @Override
    public void createBranch() {
        onCreateBranch();
    }

    @Override
    public void merge() {
        viewModel.publishError("Merge is not available in the current GitHub workflow");
    }

    @Override
    public void rebase() {
        viewModel.publishError("Rebase is not available in the current GitHub workflow");
    }

    @Override
    public void pullRequest() {
        onCreatePullRequest();
    }

    @Override
    public void issues() {
        String remote = context().remoteUrl().toString();
        String base = remote.endsWith("/") ? remote.substring(0, remote.length() - 1) : remote;
        openBrowser(URI.create(base + "/issues"));
    }

    @Override
    public void commit() {
        onCommit();
    }

    @Override
    public void stage() {
        viewModel.publishError("Staging is not exposed yet in the GitHub workflow");
    }

    @Override
    public void discard() {
        onRollback();
    }

    @Override
    public void close() {
        successClearPause.stop();
        overflowMenu.hide();
        refPopupController.hide();
        unbindThemeProperty();
        viewModel.close();
    }

    private void onCommit() {
        CommitDialog dialog = new CommitDialog(activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::commit);
    }

    private void onRollback() {
        RollbackDialog dialog = new RollbackDialog(viewModel.isHeadPushed(), activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::rollback);
    }

    private void onCreateBranch() {
        NewBranchDialog dialog = new NewBranchDialog(
            viewModel.branchesProperty(),
            viewModel.currentBranchProperty().get(),
            activeTheme()
        );
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(result -> viewModel.createBranch(result.name(), result.startPoint()));
    }

    private void onCreatePullRequest() {
        PullRequestDialog dialog = new PullRequestDialog(
            viewModel.currentBranchProperty().get(),
            viewModel.defaultBranchProperty().get(),
            activeTheme()
        );
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(draft ->
            viewModel.createPullRequest(draft, result -> onPullRequestCreated(result, draft.openInBrowser())));
    }

    private void onPullRequestCreated(PullRequestResult result, boolean openInBrowser) {
        if (openInBrowser) {
            openBrowser(result.url());
        }
    }

    private void onToken() {
        TokenDialog dialog = new TokenDialog(activeTheme());
        dialog.initOwner(getScene() == null ? null : getScene().getWindow());
        dialog.showAndWait().ifPresent(viewModel::saveToken);
    }

    private void onPopupAction(RefPopupEntry.Action action) {
        switch (action.command()) {
            case UPDATE -> viewModel.updateRepository();
            case COMMIT -> onCommit();
            case PUSH -> viewModel.push();
            case NEW_BRANCH -> onCreateBranch();
            case ROLLBACK -> onRollback();
            case TOKEN -> onToken();
            case CHECKOUT, CHECKOUT_AND_TRACK -> {
                if (action.targetRefKind() != null && !action.targetRefName().isBlank()) {
                    requestRefCheckout(action.targetRefName(), action.targetRefKind());
                }
            }
            case CHECKOUT_REVISION, OPEN_COMMIT, COMPARE_WITH_WORKING_TREE, SHOW_DIFF_WITH_WORKING_TREE, RENAME, DELETE_LOCAL_BRANCH -> {
            }
        }
    }

    private void onPopupRefActivated(RefPopupEntry.Ref ref) {
        requestRefCheckout(ref.fullRefName(), ref.refKind());
    }

    private void requestRefCheckout(String targetRefName, GitRefKind kind) {
        CurrentRefState currentRef = viewModel.currentRefStateProperty().get();
        if (currentRef != null && Objects.equals(currentRef.fullName(), targetRefName)) {
            return;
        }
        boolean force = false;
        if (viewModel.dirtyProperty().get()) {
            force = DirtyCheckoutAlert.confirm(displayRefName(targetRefName), activeTheme());
            if (!force) {
                return;
            }
        }
        viewModel.switchRef(targetRefName, kind, force);
    }

    private void refreshRefPill() {
        CurrentRefState refState = viewModel.currentRefStateProperty().get();
        if (refState != null) {
            refPill.update(refState);
        }
    }

    private void refreshChips() {
        chipStrip.getChildren().clear();
        for (SecondaryChip chip : viewModel.secondaryChipsProperty()) {
            chipStrip.getChildren().add(createChip(chip));
        }
        chipStrip.setVisible(!chipStrip.getChildren().isEmpty());
    }

    private void refreshOverflowMenu() {
        List<MenuItem> items = new java.util.ArrayList<>();
        boolean localAvailable = viewModel.localAvailableProperty().get();

        if (localAvailable) {
            items.add(createOverflowItem("Update", viewModel::updateRepository, viewModel.updateDisabledProperty().get()));
            items.add(createOverflowItem("Commit...", this::onCommit, viewModel.commitDisabledProperty().get()));
            items.add(createOverflowItem("Push", viewModel::push, viewModel.pushDisabledProperty().get()));
        }

        items.add(createOverflowItem("Pull Request...", this::onCreatePullRequest, viewModel.pullRequestDisabledProperty().get()));

        if (localAvailable) {
            items.add(new SeparatorMenuItem());
            items.add(createOverflowItem("New Branch...", this::onCreateBranch, viewModel.busyProperty().get()));
            items.add(createOverflowItem("Checkout Tag or Revision...", () -> {
            }, true));
            items.add(new SeparatorMenuItem());
            items.add(createOverflowItem("Rollback...", this::onRollback, viewModel.busyProperty().get()));
        }

        items.add(createOverflowItem(
            viewModel.authenticatedProperty().get() ? "Update Token" : "Set Token",
            this::onToken,
            viewModel.busyProperty().get()
        ));

        overflowMenu.getItems().setAll(items);
    }

    private void toggleOverflowMenu() {
        if (overflowMenu.isShowing()) {
            overflowMenu.hide();
            return;
        }
        refreshOverflowMenu();
        applyOverflowMenuTheme();
        overflowMenu.show(overflowButton, Side.BOTTOM, 0, 4);
    }

    private void refreshStatusSlot() {
        StatusMessage message = viewModel.statusMessageProperty().get();
        StatusMessage.Kind kind = message == null ? StatusMessage.Kind.IDLE : message.kind();

        busyIndicator.setVisible(kind == StatusMessage.Kind.BUSY);
        busyIndicator.setManaged(kind == StatusMessage.Kind.BUSY);

        boolean showStatusText = kind == StatusMessage.Kind.BUSY || kind == StatusMessage.Kind.SUCCESS;
        statusLabel.setVisible(showStatusText && !viewModel.statusTextProperty().get().isBlank());
        statusLabel.setManaged(statusLabel.isVisible());

        boolean showError = kind == StatusMessage.Kind.ERROR && !viewModel.errorTextProperty().get().isBlank();
        errorLabel.setVisible(showError);
        errorLabel.setManaged(showError);

        statusSlot.setVisible(kind != StatusMessage.Kind.IDLE && (busyIndicator.isVisible() || statusLabel.isVisible() || errorLabel.isVisible()));

        if (kind == StatusMessage.Kind.SUCCESS) {
            successClearPause.playFromStart();
        } else {
            successClearPause.stop();
        }
    }

    private void handleRefPillKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN && event.isAltDown()) {
            refPopupController.show(refPill);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ESCAPE && refPopupController.isShowing()) {
            refPopupController.hide();
            event.consume();
        }
    }

    private void openBrowser(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            viewModel.publishError("Desktop browser integration is not available");
            return;
        }
        try {
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            viewModel.publishError("Failed to open browser: " + ex.getMessage());
        }
    }

    private void applyTheme(Theme theme) {
        currentBaseTheme = theme == null ? Theme.dark() : theme;
        currentTheme = GitHubToolbarThemeMapper.map(currentBaseTheme);

        String rootStyle = buildRootStyle(currentBaseTheme, currentTheme);
        setStyle(rootStyle);
        refPopup.setStyle(rootStyle);
        applyOverflowMenuTheme();
        setSpacing(currentTheme.groupGap());
        setPadding(currentTheme.contentPadding());
        setMinHeight(currentTheme.toolbarHeight());
        setPrefHeight(currentTheme.toolbarHeight());

        double buttonHeight = currentTheme.buttonHeight();
        for (Node node : List.of(repoPill, refPill, overflowButton)) {
            if (node instanceof javafx.scene.control.Control control) {
                control.setMinHeight(buttonHeight);
                control.setPrefHeight(buttonHeight);
                control.setMaxHeight(buttonHeight);
            }
        }

        for (Labeled labeled : List.of(brandBadge, repoPill, overflowButton, statusLabel, errorLabel)) {
            labeled.setFont(currentBaseTheme.contentFont());
        }
    }

    private Theme activeTheme() {
        return currentBaseTheme == null ? Theme.dark() : currentBaseTheme;
    }

    private void applyOverflowMenuTheme() {
        String rootStyle = buildRootStyle(activeTheme(), currentTheme == null ? GitHubToolbarThemeMapper.map(activeTheme()) : currentTheme);
        overflowMenu.setStyle(rootStyle);
        if (overflowMenu.getScene() != null && overflowMenu.getScene().getRoot() instanceof javafx.scene.Parent parent) {
            GitHubThemeSupport.ensureStylesheetLoaded(parent, GitHubThemeSupport.TOOLBAR_STYLESHEET);
            parent.setStyle(rootStyle);
        }
    }

    private static Button createActionButton(String text, String id) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("pf-github-action-button");
        button.setMnemonicParsing(false);
        return button;
    }

    private static MenuItem createOverflowItem(String text, Runnable action, boolean disabled) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        item.setDisable(disabled);
        return item;
    }

    private static Label createChip(SecondaryChip chip) {
        UiChipLabel label = new UiChipLabel(chip.text(), mapVariant(chip.variant()));
        label.getStyleClass().add("pf-github-chip");
        return label;
    }

    private static UiChipVariant mapVariant(SecondaryChip.Variant variant) {
        return switch (variant) {
            case ACCENT -> UiChipVariant.ACCENT;
            case SUCCESS -> UiChipVariant.SUCCESS;
            case WARNING -> UiChipVariant.WARNING;
            case DANGER -> UiChipVariant.DANGER;
            case MUTED -> UiChipVariant.MUTED;
        };
    }

    private static void bindManagedToVisible(Node node) {
        node.managedProperty().bind(node.visibleProperty());
    }

    private static String displayRefName(String refName) {
        if (refName.startsWith("refs/heads/")) {
            return refName.substring("refs/heads/".length());
        }
        if (refName.startsWith("refs/remotes/")) {
            String value = refName.substring("refs/remotes/".length());
            int slashIndex = value.indexOf('/');
            return slashIndex >= 0 ? value.substring(slashIndex + 1) : value;
        }
        if (refName.startsWith("refs/tags/")) {
            return refName.substring("refs/tags/".length());
        }
        return refName;
    }

    private static String buildRootStyle(Theme baseTheme, GitHubToolbarTheme toolbarTheme) {
        return GitHubThemeSupport.themeVariables(toolbarTheme)
            + UiStyleSupport.metricVariables()
            + UiStyleSupport.fontVariables(baseTheme.contentFont())
            + String.format(Locale.ROOT, "-fx-font-family: \"%s\";-fx-font-size: %.1fpx;",
            baseTheme.contentFont() == null ? "System" : baseTheme.contentFont().getFamily().replace("\"", "\\\""),
            baseTheme.contentFont() == null ? 12.0 : baseTheme.contentFont().getSize());
    }

    private static GitHubToolbarViewModel buildViewModel(GitHubRepoContext context, CredentialStore credentialStore) {
        GitRepository gitRepository = null;
        if (context.hasLocalClone()) {
            gitRepository = new JGitRepository(context.localClonePath(), credentialStore::toJGitCredentials);
        }
        GitHubApiService apiService = new GitHubApiService(java.net.http.HttpClient.newHttpClient(), credentialStore::getToken);
        return new GitHubToolbarViewModel(context, credentialStore, gitRepository, apiService, new CommandRunner());
    }

    public static GitHubToolbar fromState(Map<String, Object> state) {
        return fromState(state, null);
    }

    public static GitHubToolbar fromState(Map<String, Object> state, Theme initialTheme) {
        String remoteUrl = readString(state, "remoteUrl");
        String localClonePath = readString(state, "localClonePath");
        try {
            GitHubRepoContext context = (localClonePath == null || localClonePath.isBlank())
                ? GitHubRepoContext.remoteOnly(new URI(remoteUrl))
                : GitHubRepoContext.of(new URI(remoteUrl), java.nio.file.Path.of(localClonePath));
            return new GitHubToolbar(context, new PatCredentialStore(), initialTheme);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid persisted remote URL", ex);
        }
    }

    private static String readString(Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (value instanceof String text) {
            return text;
        }
        return "";
    }
}
