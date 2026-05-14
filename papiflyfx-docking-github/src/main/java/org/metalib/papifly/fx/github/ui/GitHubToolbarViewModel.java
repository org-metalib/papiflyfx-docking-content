package org.metalib.papifly.fx.github.ui;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.git.GitOperationException;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiException;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RefPopupEntry;
import org.metalib.papifly.fx.github.model.RefPopupSection;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.SecondaryChip;
import org.metalib.papifly.fx.github.model.StatusMessage;
import org.metalib.papifly.fx.github.model.TagRef;
import org.metalib.papifly.fx.github.ui.state.PreferencesRecentRefStore;
import org.metalib.papifly.fx.github.ui.state.RecentRefStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class GitHubToolbarViewModel implements AutoCloseable {

    private final GitHubRepoContext context;
    private final CredentialStore credentialStore;
    private final GitRepository gitRepository;
    private final GitHubApiService gitHubApiService;
    private final CommandRunner commandRunner;
    private final RecentRefStore recentRefStore;

    private final StringProperty currentBranch;
    private final StringProperty defaultBranch;
    private final BooleanProperty dirty;
    private final BooleanProperty busy;
    private final StringProperty statusText;
    private final StringProperty errorText;
    private final BooleanProperty authenticated;
    private final BooleanProperty localAvailable;
    private final BooleanProperty remoteOnly;
    private final BooleanProperty detachedHead;
    private final BooleanProperty defaultBranchActive;
    private final IntegerProperty aheadCount;
    private final IntegerProperty behindCount;
    private final IntegerProperty dirtyCount;
    private final ObjectProperty<CommitInfo> headCommit;
    private final ListProperty<BranchRef> branches;
    private final ObjectProperty<CurrentRefState> currentRefState;
    private final ListProperty<SecondaryChip> secondaryChips;
    private final ObjectProperty<StatusMessage> statusMessage;
    private final ListProperty<RefPopupSection> refPopupSections;
    private final StringProperty refPopupFilter;

    private final BooleanBinding commitDisabled;
    private final BooleanBinding updateDisabled;
    private final BooleanBinding pushDisabled;
    private final BooleanBinding pullRequestDisabled;

    private ToolbarSnapshot latestSnapshot;
    private List<RecentRefStore.Entry> recentRefs;

    public GitHubToolbarViewModel(
        GitHubRepoContext context,
        CredentialStore credentialStore,
        GitRepository gitRepository,
        GitHubApiService gitHubApiService,
        CommandRunner commandRunner
    ) {
        this(context, credentialStore, gitRepository, gitHubApiService, commandRunner, new PreferencesRecentRefStore());
    }

    public GitHubToolbarViewModel(
        GitHubRepoContext context,
        CredentialStore credentialStore,
        GitRepository gitRepository,
        GitHubApiService gitHubApiService,
        CommandRunner commandRunner,
        RecentRefStore recentRefStore
    ) {
        this.context = Objects.requireNonNull(context, "context");
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
        this.gitRepository = gitRepository;
        this.gitHubApiService = Objects.requireNonNull(gitHubApiService, "gitHubApiService");
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");
        this.recentRefStore = Objects.requireNonNull(recentRefStore, "recentRefStore");

        boolean hasLocalRepository = context.hasLocalClone() && gitRepository != null;
        CurrentRefState initialRefState = hasLocalRepository
            ? new CurrentRefState("", "", GitRefKind.LOCAL_BRANCH, "", CurrentRefState.StatusDotState.CLEAN, false, false, false)
            : new CurrentRefState("main", "main", GitRefKind.REMOTE_BRANCH, "", CurrentRefState.StatusDotState.NEUTRAL, true, false, true);

        this.currentBranch = new SimpleStringProperty(initialRefState.displayName());
        this.defaultBranch = new SimpleStringProperty("main");
        this.dirty = new SimpleBooleanProperty(false);
        this.busy = new SimpleBooleanProperty(false);
        this.statusText = new SimpleStringProperty("");
        this.errorText = new SimpleStringProperty("");
        this.authenticated = new SimpleBooleanProperty(credentialStore.isAuthenticated());
        this.localAvailable = new SimpleBooleanProperty(hasLocalRepository);
        this.remoteOnly = new SimpleBooleanProperty(!hasLocalRepository);
        this.detachedHead = new SimpleBooleanProperty(false);
        this.defaultBranchActive = new SimpleBooleanProperty(initialRefState.defaultBranch());
        this.aheadCount = new SimpleIntegerProperty(0);
        this.behindCount = new SimpleIntegerProperty(0);
        this.dirtyCount = new SimpleIntegerProperty(0);
        this.headCommit = new SimpleObjectProperty<>();
        this.branches = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.currentRefState = new SimpleObjectProperty<>(initialRefState);
        this.secondaryChips = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.statusMessage = new SimpleObjectProperty<>(StatusMessage.IDLE);
        this.refPopupSections = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.refPopupFilter = new SimpleStringProperty("");
        this.recentRefs = List.of();

        this.commitDisabled = busy
            .or(localAvailable.not())
            .or(dirty.not())
            .or(currentBranch.isEqualTo(defaultBranch));
        this.updateDisabled = busy.or(localAvailable.not());
        this.pushDisabled = busy
            .or(localAvailable.not())
            .or(authenticated.not());
        this.pullRequestDisabled = busy
            .or(authenticated.not())
            .or(localAvailable.and(currentBranch.isEqualTo(defaultBranch)));

        this.refPopupFilter.addListener((obs, oldValue, newValue) -> rebuildPopupSections());
    }

    public void refresh() {
        runCommand("Refreshing status...", this::loadToolbarSnapshot, this::applySnapshot, snapshot -> StatusMessage.IDLE);
    }

    public void switchBranch(String branchName, boolean force) {
        switchRef(branchName, GitRefKind.LOCAL_BRANCH, force);
    }

    public void switchRef(String refName, GitRefKind kind, boolean force) {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        if (refName == null || refName.isBlank()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Ref name is required"));
            return;
        }
        runCommand("Checking out " + displayRefName(refName, kind) + "...", () -> {
            gitRepository.checkoutRef(refName, kind, force);
            return new RefCommandResult(loadToolbarSnapshot(), kind);
        }, result -> {
            applySnapshot(result.snapshot());
            recordRecent(result.snapshot().currentRefState());
        }, result -> new StatusMessage(
            StatusMessage.Kind.SUCCESS,
            "Checked out " + result.snapshot().currentRefState().displayName()
        ));
    }

    public void createBranch(String branchName, String startPoint) {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        if (branchName == null || branchName.isBlank()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "New branch name is required"));
            return;
        }
        runCommand("Creating branch " + branchName + "...", () -> {
            gitRepository.createAndCheckout(branchName, startPoint);
            return loadToolbarSnapshot();
        }, snapshot -> {
            applySnapshot(snapshot);
            recordRecent(snapshot.currentRefState());
        }, snapshot -> new StatusMessage(StatusMessage.Kind.SUCCESS, "Created branch " + branchName));
    }

    public void updateRepository() {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        runCommand("Updating repository...", () -> {
            gitRepository.update();
            return loadToolbarSnapshot();
        }, this::applySnapshot, snapshot -> new StatusMessage(StatusMessage.Kind.SUCCESS, "Repository updated"));
    }

    public void commit(String message) {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        if (currentBranch.get().equals(defaultBranch.get())) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Committing to default branch is blocked"));
            return;
        }
        if (!dirty.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Nothing to commit"));
            return;
        }
        runCommand("Creating commit...", () -> {
            CommitInfo commit = gitRepository.commitAll(message);
            return new CommitCommandResult(commit, loadToolbarSnapshot());
        }, result -> {
            headCommit.set(result.commit());
            applySnapshot(result.snapshot());
        }, result -> new StatusMessage(StatusMessage.Kind.SUCCESS, "Committed " + result.commit().shortHash()));
    }

    public void rollback(RollbackMode mode) {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        if (mode != RollbackMode.REVERT && gitRepository.isHeadPushed()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Only revert is allowed for pushed commits"));
            return;
        }
        runCommand("Rolling back commit...", () -> {
            gitRepository.rollback(mode);
            return loadToolbarSnapshot();
        }, this::applySnapshot, snapshot -> new StatusMessage(StatusMessage.Kind.SUCCESS, "Rollback complete"));
    }

    public void push() {
        if (!localAvailable.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Local repository is unavailable"));
            return;
        }
        if (!authenticated.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Authentication required"));
            return;
        }
        runCommand("Pushing changes...", () -> {
            gitRepository.push("origin");
            return loadToolbarSnapshot();
        }, this::applySnapshot, snapshot -> new StatusMessage(StatusMessage.Kind.SUCCESS, "Push completed"));
    }

    public void createPullRequest(PullRequestDraft draft, Consumer<PullRequestResult> onSuccess) {
        if (!authenticated.get()) {
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, "Authentication required"));
            return;
        }
        PullRequestDraft normalized = draft.withDefaults(
            "PR: " + currentBranch.get(),
            currentBranch.get(),
            defaultBranch.get()
        );
        runCommand("Creating pull request...", () ->
                gitHubApiService.createPullRequest(context.owner(), context.repo(), normalized),
            result -> onSuccess.accept(result),
            result -> new StatusMessage(StatusMessage.Kind.SUCCESS, "PR #" + result.number() + " created"));
    }

    public void saveToken(String token) {
        if (token == null || token.isBlank()) {
            credentialStore.clearToken();
            authenticated.set(false);
            rebuildFromCurrentSnapshot();
            setStatusMessage(new StatusMessage(StatusMessage.Kind.SUCCESS, "Token cleared"));
            return;
        }
        credentialStore.setToken(token);
        authenticated.set(credentialStore.isAuthenticated());
        rebuildFromCurrentSnapshot();
        setStatusMessage(new StatusMessage(StatusMessage.Kind.SUCCESS, "Token saved"));
    }

    public void publishError(String message) {
        setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, message == null ? "" : message));
    }

    public void clearStatusMessage() {
        if (statusMessage.get().kind() != StatusMessage.Kind.BUSY) {
            setStatusMessage(StatusMessage.IDLE);
        }
    }

    public void clearRefPopupFilter() {
        refPopupFilter.set("");
    }

    private ToolbarSnapshot loadToolbarSnapshot() {
        RepoStatus repoStatus = null;
        CurrentRefState currentRef = remoteOnlyFallbackCurrentRef(defaultBranch.get());
        List<BranchRef> branchRefs = List.of();
        List<TagRef> tagRefs = List.of();
        CommitInfo commitInfo = null;
        String resolvedDefault = defaultBranch.get();

        if (gitRepository != null) {
            repoStatus = gitRepository.loadStatus();
            currentRef = gitRepository.loadCurrentRef();
            branchRefs = gitRepository.listBranches();
            tagRefs = gitRepository.listTags();
            resolvedDefault = resolveDefaultBranch(repoStatus.defaultBranch());
            try {
                commitInfo = gitRepository.getHeadCommit();
            } catch (GitOperationException ex) {
                commitInfo = null;
            }
        } else {
            resolvedDefault = resolveDefaultBranch(resolvedDefault);
            currentRef = remoteOnlyFallbackCurrentRef(resolvedDefault);
        }

        boolean hasLocalRepository = context.hasLocalClone() && gitRepository != null;
        CurrentRefState resolvedCurrentRef = enrichCurrentRef(currentRef, repoStatus, resolvedDefault, !hasLocalRepository);
        return new ToolbarSnapshot(
            repoStatus,
            resolvedCurrentRef,
            branchRefs,
            tagRefs,
            commitInfo,
            resolvedDefault,
            credentialStore.isAuthenticated(),
            hasLocalRepository
        );
    }

    private String resolveDefaultBranch(String currentDefaultBranch) {
        String resolvedDefault = currentDefaultBranch;
        if (resolvedDefault == null || resolvedDefault.isBlank()) {
            try {
                String apiDefault = gitHubApiService.fetchDefaultBranch(context.owner(), context.repo());
                if (apiDefault != null && !apiDefault.isBlank()) {
                    resolvedDefault = apiDefault;
                }
            } catch (GitHubApiException ignored) {
            }
        }
        if ((resolvedDefault == null || resolvedDefault.isBlank()) && gitRepository != null) {
            resolvedDefault = gitRepository.detectDefaultBranch();
        }
        if (resolvedDefault == null || resolvedDefault.isBlank()) {
            resolvedDefault = "main";
        }
        return resolvedDefault;
    }

    private CurrentRefState remoteOnlyFallbackCurrentRef(String resolvedDefault) {
        String defaultRef = (resolvedDefault == null || resolvedDefault.isBlank()) ? "main" : resolvedDefault;
        return new CurrentRefState(
            defaultRef,
            defaultRef,
            GitRefKind.REMOTE_BRANCH,
            "",
            CurrentRefState.StatusDotState.NEUTRAL,
            true,
            false,
            true
        );
    }

    private CurrentRefState enrichCurrentRef(
        CurrentRefState currentRef,
        RepoStatus repoStatus,
        String resolvedDefault,
        boolean remoteOnlyMode
    ) {
        CurrentRefState.StatusDotState dotState = remoteOnlyMode
            ? CurrentRefState.StatusDotState.NEUTRAL
            : repoStatus != null && repoStatus.dirty()
                ? CurrentRefState.StatusDotState.DIRTY
                : CurrentRefState.StatusDotState.CLEAN;
        boolean defaultRef = Objects.equals(currentRef.displayName(), resolvedDefault)
            && currentRef.kind() != GitRefKind.TAG
            && currentRef.kind() != GitRefKind.DETACHED_COMMIT;
        return new CurrentRefState(
            currentRef.displayName(),
            currentRef.fullName(),
            currentRef.kind(),
            currentRef.trackingLabel(),
            dotState,
            defaultRef,
            currentRef.detached(),
            remoteOnlyMode
        );
    }

    private void applySnapshot(ToolbarSnapshot snapshot) {
        latestSnapshot = snapshot;
        defaultBranch.set(snapshot.defaultBranch());
        currentBranch.set(snapshot.currentRefState().displayName());
        authenticated.set(snapshot.authenticated());
        localAvailable.set(snapshot.localAvailable());
        remoteOnly.set(!snapshot.localAvailable());
        currentRefState.set(snapshot.currentRefState());
        branches.setAll(snapshot.branches());
        headCommit.set(snapshot.headCommit());
        recentRefs = recentRefStore.load(context.remoteUrl());

        if (snapshot.repoStatus() != null) {
            RepoStatus repoStatus = snapshot.repoStatus();
            dirty.set(repoStatus.dirty());
            detachedHead.set(snapshot.currentRefState().detached());
            aheadCount.set(Math.max(0, repoStatus.aheadCount()));
            behindCount.set(Math.max(0, repoStatus.behindCount()));
            dirtyCount.set(countDirtyEntries(repoStatus));
            defaultBranchActive.set(snapshot.currentRefState().defaultBranch());
        } else {
            dirty.set(false);
            detachedHead.set(false);
            aheadCount.set(0);
            behindCount.set(0);
            dirtyCount.set(0);
            defaultBranchActive.set(snapshot.currentRefState().defaultBranch());
        }

        secondaryChips.setAll(buildSecondaryChips(snapshot));
        rebuildPopupSections();
    }

    private void rebuildFromCurrentSnapshot() {
        if (latestSnapshot == null) {
            return;
        }
        ToolbarSnapshot snapshot = new ToolbarSnapshot(
            latestSnapshot.repoStatus(),
            latestSnapshot.currentRefState(),
            latestSnapshot.branches(),
            latestSnapshot.tags(),
            latestSnapshot.headCommit(),
            latestSnapshot.defaultBranch(),
            credentialStore.isAuthenticated(),
            latestSnapshot.localAvailable()
        );
        applySnapshot(snapshot);
    }

    private void rebuildPopupSections() {
        if (latestSnapshot == null) {
            refPopupSections.clear();
            return;
        }
        String normalizedFilter = normalizeFilter(refPopupFilter.get());
        PopupEntries popupEntries = buildPopupEntries(latestSnapshot);
        List<RefPopupSection> sections = new ArrayList<>();

        addSectionIfNotEmpty(sections, "Recent", filterEntries(buildRecentEntries(popupEntries), normalizedFilter));
        addSectionIfNotEmpty(sections, "Local", filterEntries(popupEntries.localEntries(), normalizedFilter));

        for (Map.Entry<String, List<RefPopupEntry.Ref>> remoteGroup : popupEntries.remoteEntries().entrySet()) {
            String title = popupEntries.remoteEntries().size() == 1
                ? "Remote"
                : "Remote: " + remoteGroup.getKey();
            addSectionIfNotEmpty(sections, title, filterEntries(remoteGroup.getValue(), normalizedFilter));
        }

        addSectionIfNotEmpty(sections, "Tags", filterEntries(popupEntries.tagEntries(), normalizedFilter));
        refPopupSections.setAll(sections);
    }

    private PopupEntries buildPopupEntries(ToolbarSnapshot snapshot) {
        List<RefPopupEntry.Ref> localEntries = new ArrayList<>();
        Map<String, List<RefPopupEntry.Ref>> remoteEntries = new LinkedHashMap<>();
        List<RefPopupEntry.Ref> tagEntries = new ArrayList<>();
        Map<String, RefPopupEntry.Ref> entriesByFullName = new LinkedHashMap<>();

        for (BranchRef branch : snapshot.branches()) {
            if (branch.local()) {
                RefPopupEntry.Ref entry = new RefPopupEntry.Ref(
                    "local-" + branch.fullName(),
                    branch.name(),
                    branch.trackingTarget(),
                    GitRefKind.LOCAL_BRANCH,
                    branch.fullName(),
                    branch.current(),
                    true,
                    List.of()
                );
                localEntries.add(entry);
                entriesByFullName.put(branch.fullName(), entry);
            } else if (branch.remote()) {
                RefPopupEntry.Ref entry = new RefPopupEntry.Ref(
                    "remote-" + branch.fullName(),
                    branch.name(),
                    branch.remoteName(),
                    GitRefKind.REMOTE_BRANCH,
                    branch.fullName(),
                    false,
                    true,
                    remoteSubmenu(branch)
                );
                remoteEntries.computeIfAbsent(branch.remoteName(), ignored -> new ArrayList<>()).add(entry);
                entriesByFullName.put(branch.fullName(), entry);
            }
        }

        for (TagRef tag : snapshot.tags()) {
            RefPopupEntry.Ref entry = new RefPopupEntry.Ref(
                "tag-" + tag.fullName(),
                tag.name(),
                "",
                GitRefKind.TAG,
                tag.fullName(),
                tag.current(),
                true,
                tagSubmenu(tag)
            );
            tagEntries.add(entry);
            entriesByFullName.put(tag.fullName(), entry);
        }

        if (snapshot.currentRefState().kind() == GitRefKind.DETACHED_COMMIT) {
            entriesByFullName.put(snapshot.currentRefState().fullName(), new RefPopupEntry.Ref(
                "detached-" + snapshot.currentRefState().fullName(),
                snapshot.currentRefState().displayName(),
                "",
                GitRefKind.DETACHED_COMMIT,
                snapshot.currentRefState().fullName(),
                true,
                true,
                detachedCommitSubmenu(snapshot.currentRefState())
            ));
        }

        return new PopupEntries(
            List.copyOf(localEntries),
            copyRemoteGroups(remoteEntries),
            List.copyOf(tagEntries),
            Map.copyOf(entriesByFullName)
        );
    }

    private List<RefPopupEntry> buildRecentEntries(PopupEntries popupEntries) {
        if (recentRefs.isEmpty()) {
            return List.of();
        }
        List<RefPopupEntry> entries = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RecentRefStore.Entry recentRef : recentRefs) {
            RefPopupEntry.Ref entry = popupEntries.entriesByFullName().get(recentRef.fullRefName());
            if (entry != null && seen.add(recentRef.fullRefName())) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    private List<SecondaryChip> buildSecondaryChips(ToolbarSnapshot snapshot) {
        List<SecondaryChip> chips = new ArrayList<>();
        if (snapshot.currentRefState().defaultBranch() && snapshot.localAvailable()) {
            chips.add(new SecondaryChip("Default", SecondaryChip.Variant.WARNING));
        }
        if (snapshot.currentRefState().detached()) {
            chips.add(new SecondaryChip("Detached", SecondaryChip.Variant.DANGER));
        }
        if (!snapshot.localAvailable()) {
            chips.add(new SecondaryChip("Remote only", SecondaryChip.Variant.MUTED));
        }
        if (snapshot.repoStatus() != null && snapshot.repoStatus().aheadCount() > 0) {
            chips.add(new SecondaryChip("Ahead " + snapshot.repoStatus().aheadCount(), SecondaryChip.Variant.ACCENT));
        }
        if (snapshot.repoStatus() != null && snapshot.repoStatus().behindCount() > 0) {
            chips.add(new SecondaryChip("Behind " + snapshot.repoStatus().behindCount(), SecondaryChip.Variant.WARNING));
        }
        if (!snapshot.authenticated()) {
            chips.add(new SecondaryChip("No token", SecondaryChip.Variant.MUTED));
        }
        return List.copyOf(chips);
    }

    private void recordRecent(CurrentRefState refState) {
        if (refState.remoteOnly() || refState.fullName().isBlank()) {
            recentRefs = recentRefStore.load(context.remoteUrl());
            rebuildPopupSections();
            return;
        }
        recentRefStore.record(context.remoteUrl(), new RecentRefStore.Entry(
            refState.fullName(),
            refState.displayName(),
            refState.kind()
        ));
        recentRefs = recentRefStore.load(context.remoteUrl());
        rebuildPopupSections();
    }

    private void setStatusMessage(StatusMessage message) {
        StatusMessage resolved = message == null ? StatusMessage.IDLE : message;
        switch (resolved.kind()) {
            case IDLE -> {
                statusText.set("");
                errorText.set("");
            }
            case BUSY, SUCCESS -> {
                statusText.set(resolved.text());
                errorText.set("");
            }
            case ERROR -> {
                statusText.set("");
                errorText.set(resolved.text());
            }
        }
        statusMessage.set(resolved);
    }

    private <T> void runCommand(
        String busyText,
        Callable<T> action,
        Consumer<T> onSuccess,
        Function<T, StatusMessage> successStatus
    ) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        setStatusMessage(new StatusMessage(StatusMessage.Kind.BUSY, busyText));

        commandRunner.run(action, value -> {
            busy.set(false);
            onSuccess.accept(value);
            setStatusMessage(successStatus.apply(value));
        }, throwable -> {
            busy.set(false);
            String message = mapError(throwable);
            setStatusMessage(new StatusMessage(StatusMessage.Kind.ERROR, message));
        });
    }

    private static String mapError(Throwable throwable) {
        if (throwable instanceof GitOperationException gitOperationException) {
            return gitOperationException.getMessage();
        }
        if (throwable instanceof GitHubApiException gitHubApiException) {
            return gitHubApiException.getMessage();
        }
        if (throwable.getCause() != null) {
            return mapError(throwable.getCause());
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? "Operation failed" : message;
    }

    private static int countDirtyEntries(RepoStatus repoStatus) {
        Set<String> dirtyEntries = new LinkedHashSet<>();
        dirtyEntries.addAll(repoStatus.added());
        dirtyEntries.addAll(repoStatus.changed());
        dirtyEntries.addAll(repoStatus.removed());
        dirtyEntries.addAll(repoStatus.missing());
        dirtyEntries.addAll(repoStatus.modified());
        dirtyEntries.addAll(repoStatus.untracked());
        return dirtyEntries.size();
    }

    private static List<RefPopupEntry> filterEntries(List<? extends RefPopupEntry> entries, String normalizedFilter) {
        if (normalizedFilter.isBlank()) {
            return List.copyOf(entries);
        }
        List<RefPopupEntry> filtered = new ArrayList<>();
        for (RefPopupEntry entry : entries) {
            if (matchesFilter(entry, normalizedFilter)) {
                filtered.add(entry);
            }
        }
        return List.copyOf(filtered);
    }

    private static boolean matchesFilter(RefPopupEntry entry, String normalizedFilter) {
        return normalizeFilter(entry.primaryText()).contains(normalizedFilter)
            || normalizeFilter(entry.secondaryText()).contains(normalizedFilter);
    }

    private static void addSectionIfNotEmpty(List<RefPopupSection> sections, String title, List<RefPopupEntry> entries) {
        if (!entries.isEmpty()) {
            sections.add(new RefPopupSection(title, entries));
        }
    }

    private static String normalizeFilter(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static String displayRefName(String refName, GitRefKind kind) {
        if (kind == GitRefKind.TAG || kind == GitRefKind.DETACHED_COMMIT) {
            return refName.startsWith("refs/") ? refName.substring(refName.lastIndexOf('/') + 1) : refName;
        }
        if (refName.startsWith("refs/heads/")) {
            return refName.substring("refs/heads/".length());
        }
        if (refName.startsWith("refs/remotes/")) {
            String value = refName.substring("refs/remotes/".length());
            int slashIndex = value.indexOf('/');
            return slashIndex >= 0 ? value.substring(slashIndex + 1) : value;
        }
        return refName;
    }

    private static List<RefPopupEntry.Action> remoteSubmenu(BranchRef branch) {
        return List.of(
            new RefPopupEntry.Action(
                "submenu-checkout-track-" + branch.fullName(),
                "Checkout and Track",
                branch.remoteName(),
                RefPopupEntry.Command.CHECKOUT_AND_TRACK,
                true,
                branch.fullName(),
                GitRefKind.REMOTE_BRANCH
            ),
            new RefPopupEntry.Action(
                "submenu-diff-" + branch.fullName(),
                "Show Diff with Working Tree",
                "",
                RefPopupEntry.Command.SHOW_DIFF_WITH_WORKING_TREE,
                false
            ),
            new RefPopupEntry.Action(
                "submenu-rename-" + branch.fullName(),
                "Rename...",
                "",
                RefPopupEntry.Command.RENAME,
                false
            ),
            new RefPopupEntry.Action(
                "submenu-delete-" + branch.fullName(),
                "Delete Local Branch...",
                "",
                RefPopupEntry.Command.DELETE_LOCAL_BRANCH,
                false
            )
        );
    }

    private static List<RefPopupEntry.Action> tagSubmenu(TagRef tag) {
        return List.of(
            new RefPopupEntry.Action(
                "submenu-tag-checkout-" + tag.fullName(),
                "Checkout Tag",
                "",
                RefPopupEntry.Command.CHECKOUT,
                true,
                tag.fullName(),
                GitRefKind.TAG
            ),
            new RefPopupEntry.Action(
                "submenu-tag-open-" + tag.fullName(),
                "Open Commit",
                "",
                RefPopupEntry.Command.OPEN_COMMIT,
                false
            ),
            new RefPopupEntry.Action(
                "submenu-tag-compare-" + tag.fullName(),
                "Compare with Working Tree",
                "",
                RefPopupEntry.Command.COMPARE_WITH_WORKING_TREE,
                false
            )
        );
    }

    private static List<RefPopupEntry.Action> detachedCommitSubmenu(CurrentRefState refState) {
        return List.of(
            new RefPopupEntry.Action(
                "submenu-commit-open-" + refState.fullName(),
                "Open Commit",
                "",
                RefPopupEntry.Command.OPEN_COMMIT,
                false
            ),
            new RefPopupEntry.Action(
                "submenu-commit-compare-" + refState.fullName(),
                "Compare with Working Tree",
                "",
                RefPopupEntry.Command.COMPARE_WITH_WORKING_TREE,
                false
            )
        );
    }

    private static Map<String, List<RefPopupEntry.Ref>> copyRemoteGroups(Map<String, List<RefPopupEntry.Ref>> remoteGroups) {
        Map<String, List<RefPopupEntry.Ref>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<RefPopupEntry.Ref>> entry : remoteGroups.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    public GitHubRepoContext context() {
        return context;
    }

    public ReadOnlyStringProperty currentBranchProperty() {
        return currentBranch;
    }

    public ReadOnlyStringProperty defaultBranchProperty() {
        return defaultBranch;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty;
    }

    public ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    public ReadOnlyStringProperty statusTextProperty() {
        return statusText;
    }

    public ReadOnlyStringProperty errorTextProperty() {
        return errorText;
    }

    public ReadOnlyBooleanProperty authenticatedProperty() {
        return authenticated;
    }

    public ReadOnlyBooleanProperty localAvailableProperty() {
        return localAvailable;
    }

    public ReadOnlyBooleanProperty remoteOnlyProperty() {
        return remoteOnly;
    }

    public ReadOnlyBooleanProperty detachedHeadProperty() {
        return detachedHead;
    }

    public ReadOnlyBooleanProperty defaultBranchActiveProperty() {
        return defaultBranchActive;
    }

    public ReadOnlyIntegerProperty aheadCountProperty() {
        return aheadCount;
    }

    public ReadOnlyIntegerProperty behindCountProperty() {
        return behindCount;
    }

    public ReadOnlyIntegerProperty dirtyCountProperty() {
        return dirtyCount;
    }

    public ReadOnlyObjectProperty<CommitInfo> headCommitProperty() {
        return headCommit;
    }

    public ReadOnlyListProperty<BranchRef> branchesProperty() {
        return branches;
    }

    public ReadOnlyObjectProperty<CurrentRefState> currentRefStateProperty() {
        return currentRefState;
    }

    public ReadOnlyListProperty<SecondaryChip> secondaryChipsProperty() {
        return secondaryChips;
    }

    public ReadOnlyObjectProperty<StatusMessage> statusMessageProperty() {
        return statusMessage;
    }

    public ReadOnlyListProperty<RefPopupSection> refPopupSectionsProperty() {
        return refPopupSections;
    }

    public StringProperty refPopupFilterProperty() {
        return refPopupFilter;
    }

    public BooleanBinding commitDisabledProperty() {
        return commitDisabled;
    }

    public BooleanBinding updateDisabledProperty() {
        return updateDisabled;
    }

    public BooleanBinding pushDisabledProperty() {
        return pushDisabled;
    }

    public BooleanBinding pullRequestDisabledProperty() {
        return pullRequestDisabled;
    }

    public boolean isHeadPushed() {
        if (!localAvailable.get()) {
            return false;
        }
        return gitRepository.isHeadPushed();
    }

    @Override
    public void close() {
        commandRunner.close();
        if (gitRepository != null) {
            gitRepository.close();
        }
    }

    private record ToolbarSnapshot(
        RepoStatus repoStatus,
        CurrentRefState currentRefState,
        List<BranchRef> branches,
        List<TagRef> tags,
        CommitInfo headCommit,
        String defaultBranch,
        boolean authenticated,
        boolean localAvailable
    ) {
    }

    private record CommitCommandResult(
        CommitInfo commit,
        ToolbarSnapshot snapshot
    ) {
    }

    private record RefCommandResult(
        ToolbarSnapshot snapshot,
        GitRefKind kind
    ) {
    }

    private record PopupEntries(
        List<RefPopupEntry.Ref> localEntries,
        Map<String, List<RefPopupEntry.Ref>> remoteEntries,
        List<RefPopupEntry.Ref> tagEntries,
        Map<String, RefPopupEntry.Ref> entriesByFullName
    ) {
    }
}
