package org.metalib.papifly.fx.github.ui;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.github.api.GitHubRepoContext;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;
import org.metalib.papifly.fx.github.git.GitRepository;
import org.metalib.papifly.fx.github.github.GitHubApiException;
import org.metalib.papifly.fx.github.github.GitHubApiService;
import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.CommitInfo;
import org.metalib.papifly.fx.github.model.CurrentRefState;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.PullRequestDraft;
import org.metalib.papifly.fx.github.model.PullRequestResult;
import org.metalib.papifly.fx.github.model.RefPopupSection;
import org.metalib.papifly.fx.github.model.RepoStatus;
import org.metalib.papifly.fx.github.model.RollbackMode;
import org.metalib.papifly.fx.github.model.StatusMessage;
import org.metalib.papifly.fx.github.model.TagRef;
import org.metalib.papifly.fx.github.ui.state.RecentRefStore;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubToolbarViewModelTest {

    @Test
    void remoteOnlyModeDisablesLocalActionsAndUsesNeutralCurrentRef() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.remoteOnly(URI.create("https://github.com/org/repo"));

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            null,
            new FakeGitHubApiService("main", false),
            new CommandRunner(true),
            new InMemoryRecentRefStore()
        )) {
            viewModel.refresh();

            assertFalse(viewModel.localAvailableProperty().get());
            assertTrue(viewModel.remoteOnlyProperty().get());
            assertTrue(viewModel.commitDisabledProperty().get());
            assertTrue(viewModel.pushDisabledProperty().get());
            assertEquals(GitRefKind.REMOTE_BRANCH, viewModel.currentRefStateProperty().get().kind());
            assertEquals(CurrentRefState.StatusDotState.NEUTRAL, viewModel.currentRefStateProperty().get().statusDotState());
            assertTrue(viewModel.secondaryChipsProperty().stream().anyMatch(chip -> chip.text().equals("Remote only")));
        }
    }

    @Test
    void refreshPublishesRichRefStateAndSecondaryChips() {
        PatCredentialStore store = new PatCredentialStore();
        store.setToken("token");
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();
        repository.currentRefName = "v0.9.0";
        repository.currentRefKind = GitRefKind.TAG;
        repository.status = new RepoStatus(
            "v0.9.0",
            "main",
            true,
            3,
            1,
            Set.of("A.java"),
            Set.of("B.java"),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of()
        );

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService("main", false),
            new CommandRunner(true),
            new InMemoryRecentRefStore()
        )) {
            viewModel.refresh();

            assertTrue(viewModel.localAvailableProperty().get());
            assertFalse(viewModel.remoteOnlyProperty().get());
            assertTrue(viewModel.dirtyProperty().get());
            assertEquals(2, viewModel.dirtyCountProperty().get());
            assertEquals(3, viewModel.aheadCountProperty().get());
            assertEquals(1, viewModel.behindCountProperty().get());
            assertTrue(viewModel.detachedHeadProperty().get());
            assertEquals(GitRefKind.TAG, viewModel.currentRefStateProperty().get().kind());
            assertEquals(CurrentRefState.StatusDotState.DIRTY, viewModel.currentRefStateProperty().get().statusDotState());
            assertTrue(viewModel.secondaryChipsProperty().stream().anyMatch(chip -> chip.text().equals("Detached")));
            assertTrue(viewModel.secondaryChipsProperty().stream().anyMatch(chip -> chip.text().equals("Ahead 3")));
            assertTrue(viewModel.secondaryChipsProperty().stream().anyMatch(chip -> chip.text().equals("Behind 1")));
        }
    }

    @Test
    void backgroundApiFailureDoesNotOccupyErrorSlot() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService("main", true),
            new CommandRunner(true),
            new InMemoryRecentRefStore()
        )) {
            viewModel.refresh();

            assertEquals("", viewModel.errorTextProperty().get());
            assertEquals(StatusMessage.Kind.IDLE, viewModel.statusMessageProperty().get().kind());
        }
    }

    @Test
    void successfulCheckoutUpdatesRecentSectionsInNewestFirstOrder() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();
        InMemoryRecentRefStore recentRefStore = new InMemoryRecentRefStore();

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService("main", false),
            new CommandRunner(true),
            recentRefStore
        )) {
            viewModel.refresh();
            viewModel.switchRef("refs/heads/feature/x", GitRefKind.LOCAL_BRANCH, false);
            viewModel.switchRef("refs/tags/v0.9.0", GitRefKind.TAG, false);

            List<RefPopupSection> sections = List.copyOf(viewModel.refPopupSectionsProperty());
            RefPopupSection recentSection = sections.stream()
                .filter(section -> section.title().equals("Recent"))
                .findFirst()
                .orElseThrow();

            assertEquals(2, recentRefStore.entries.size());
            assertEquals("refs/tags/v0.9.0", recentRefStore.entries.getFirst().fullRefName());
            assertEquals("v0.9.0", recentSection.entries().getFirst().primaryText());
            assertEquals(StatusMessage.Kind.SUCCESS, viewModel.statusMessageProperty().get().kind());
            assertNotEquals("", viewModel.statusTextProperty().get());
        }
    }

    @Test
    void updateRepositoryCallsRepositoryAndPublishesSuccess() {
        PatCredentialStore store = new PatCredentialStore();
        GitHubRepoContext context = GitHubRepoContext.of(
            URI.create("https://github.com/org/repo"),
            Path.of(".")
        );

        FakeGitRepository repository = new FakeGitRepository();

        try (GitHubToolbarViewModel viewModel = new GitHubToolbarViewModel(
            context,
            store,
            repository,
            new FakeGitHubApiService("main", false),
            new CommandRunner(true),
            new InMemoryRecentRefStore()
        )) {
            viewModel.refresh();
            viewModel.updateRepository();

            assertTrue(repository.updateCalled);
            assertEquals(StatusMessage.Kind.SUCCESS, viewModel.statusMessageProperty().get().kind());
            assertEquals("Repository updated", viewModel.statusTextProperty().get());
        }
    }

    private static final class FakeGitHubApiService extends GitHubApiService {

        private final String defaultBranch;
        private final boolean failDefaultBranch;

        private FakeGitHubApiService(String defaultBranch, boolean failDefaultBranch) {
            this.defaultBranch = defaultBranch;
            this.failDefaultBranch = failDefaultBranch;
        }

        @Override
        public String fetchDefaultBranch(String owner, String repo) {
            if (failDefaultBranch) {
                throw new GitHubApiException(GitHubApiException.Category.NETWORK, "boom", -1);
            }
            return defaultBranch;
        }

        @Override
        public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft) {
            return new PullRequestResult(1, URI.create("https://github.com/org/repo/pull/1"));
        }
    }

    private static final class FakeGitRepository implements GitRepository {

        private RepoStatus status = new RepoStatus(
            "main",
            "main",
            false,
            0,
            0,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of()
        );

        private String currentRefName = "main";
        private GitRefKind currentRefKind = GitRefKind.LOCAL_BRANCH;
        private boolean updateCalled;

        @Override
        public RepoStatus loadStatus() {
            return status;
        }

        @Override
        public List<BranchRef> listBranches() {
            return List.of(
                new BranchRef("main", "refs/heads/main", true, false, currentRefKind == GitRefKind.LOCAL_BRANCH && "main".equals(currentRefName), "", "origin/main"),
                new BranchRef("feature/x", "refs/heads/feature/x", true, false, currentRefKind == GitRefKind.LOCAL_BRANCH && "feature/x".equals(currentRefName), "", "origin/feature/x"),
                new BranchRef("release", "refs/remotes/origin/release", false, true, false, "origin", "")
            );
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
                    status.dirty() ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
                    currentRefName.equals(status.defaultBranch()),
                    false,
                    false
                );
                case TAG -> new CurrentRefState(
                    currentRefName,
                    "refs/tags/" + currentRefName,
                    GitRefKind.TAG,
                    "",
                    status.dirty() ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
                    false,
                    true,
                    false
                );
                case DETACHED_COMMIT -> new CurrentRefState(
                    currentRefName,
                    "abcdef1234567890abcdef1234567890abcdef12",
                    GitRefKind.DETACHED_COMMIT,
                    "",
                    status.dirty() ? CurrentRefState.StatusDotState.DIRTY : CurrentRefState.StatusDotState.CLEAN,
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
                    false,
                    false,
                    false
                );
            };
        }

        @Override
        public void checkoutRef(String refName, GitRefKind kind, boolean force) {
            switch (kind) {
                case LOCAL_BRANCH -> {
                    currentRefKind = GitRefKind.LOCAL_BRANCH;
                    currentRefName = refName.substring(refName.lastIndexOf('/') + 1);
                }
                case REMOTE_BRANCH -> {
                    currentRefKind = GitRefKind.LOCAL_BRANCH;
                    currentRefName = refName.substring(refName.lastIndexOf('/') + 1);
                }
                case TAG -> {
                    currentRefKind = GitRefKind.TAG;
                    currentRefName = refName.substring(refName.lastIndexOf('/') + 1);
                }
                case DETACHED_COMMIT -> {
                    currentRefKind = GitRefKind.DETACHED_COMMIT;
                    currentRefName = refName.length() > 7 ? refName.substring(0, 7) : refName;
                }
            }
            status = new RepoStatus(
                currentRefName,
                status.defaultBranch(),
                kind == GitRefKind.TAG || kind == GitRefKind.DETACHED_COMMIT,
                status.aheadCount(),
                status.behindCount(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
            );
        }

        @Override
        public void checkout(String branchName, boolean force) {
            checkoutRef(branchName, GitRefKind.LOCAL_BRANCH, force);
        }

        @Override
        public void createAndCheckout(String branchName, String startPoint) {
            checkoutRef(branchName, GitRefKind.LOCAL_BRANCH, false);
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
            updateCalled = true;
        }

        @Override
        public boolean isHeadPushed() {
            return false;
        }

        @Override
        public String detectDefaultBranch() {
            return status.defaultBranch();
        }

        @Override
        public void close() {
        }
    }

    private static final class InMemoryRecentRefStore implements RecentRefStore {

        private final List<Entry> entries = new ArrayList<>();

        @Override
        public List<Entry> load(URI remoteUrl) {
            return List.copyOf(entries);
        }

        @Override
        public void record(URI remoteUrl, Entry entry) {
            entries.removeIf(existing -> existing.fullRefName().equals(entry.fullRefName()));
            entries.addFirst(entry);
            while (entries.size() > 5) {
                entries.removeLast();
            }
        }
    }
}
