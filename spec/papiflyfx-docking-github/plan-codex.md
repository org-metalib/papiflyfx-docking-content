# papiflyfx-docking-github Implementation Plan (Codex)

## 1. Objective

Implement a new module `papiflyfx-docking-github` that provides a docking-aware GitHub toolbar component for repository workflows:

1. Show clickable repository link.
2. Show current branch and dirty status for a local clone.
3. Switch branches.
4. Create and checkout new branch.
5. Commit changes, but block commits on default branch.
6. Roll back the last commit with safe behavior.
7. Push changes.
8. Create pull requests.
9. Mount toolbar at top or bottom of host window.

This plan is implementation-focused and intentionally excludes compatibility requirement sections.

## 2. Source Synthesis

This plan merges ideas from:

1. `spec/papiflyfx-docking-github/github-chatgpt.md`
2. `spec/papiflyfx-docking-github/github-grok.md`
3. `spec/papiflyfx-docking-github/github-gemini.md`

Resulting decisions:

1. Keep a clear 3-layer design: UI, Git domain, GitHub integration.
2. Use JGit for local Git operations.
3. Use explicit safe rollback semantics (`revert` default, reset only with confirmation).
4. Detect default branch from remote metadata first, with `main/master` fallback.
5. Start with PAT authentication path for push and PR creation.
6. Run all Git and network calls off the JavaFX thread.
7. Mount as a toolbar `Node` that host can attach to top or bottom.

Note on Gemini source: `github-gemini.md` currently contains only a heading and no technical detail, so actionable content is derived from ChatGPT + Grok docs and repository conventions.

## 3. Scope

### 3.1 In Scope (v1)

1. New Maven module `papiflyfx-docking-github`.
2. Toolbar UI and view-model.
3. Local Git actions:
   - branch list
   - checkout existing branch
   - create + checkout new branch
   - commit (default branch blocked)
   - rollback last commit
   - push
4. GitHub actions:
   - resolve default branch
   - create PR
5. PAT credential entry + persistence abstraction.
6. Deterministic unit tests and basic TestFX coverage.

### 3.2 Out of Scope (v1)

1. Full staging UI (file-by-file index manipulation).
2. Rebase/cherry-pick/merge UI.
3. Advanced auth UX (device flow wizard).
4. Multi-repo workspace orchestration in one toolbar instance.

## 4. Architecture

## 4.1 Layers

```text
GitHubToolbarPane (JavaFX UI)
  -> GitHubToolbarViewModel (state + commands)
      -> GitService (local repo operations via JGit)
      -> GitHubApiService (PR/default branch operations)
      -> CredentialsService (PAT load/save/provide)
```

## 4.2 Core Design Principles

1. UI contains no direct JGit or GitHub API calls.
2. Domain services return immutable DTOs or throw typed exceptions.
3. ViewModel owns command enable/disable logic.
4. All slow operations execute on background executor.
5. Toolbar can be hosted either as app chrome (`setTop`/`setBottom`) or inside a dock leaf.

## 4.3 Runtime State Model

```text
UNINITIALIZED
  -> READY (repo loaded, metadata loaded)
  -> AUTH_REQUIRED (token needed for push/PR)
  -> ERROR (recoverable)
READY
  -> BUSY (command running)
BUSY
  -> READY | AUTH_REQUIRED | ERROR
```

## 5. Module and Package Layout

```text
papiflyfx-docking-github/
  pom.xml
  src/main/java/org/metalib/papifly/fx/github/api/
    GitHubToolbarPane.java
    GitHubToolbarFactory.java
    GitHubToolbarStateAdapter.java
    GitHubRepoContext.java
    GitHubToolbarState.java
  src/main/java/org/metalib/papifly/fx/github/viewmodel/
    GitHubToolbarViewModel.java
    CommandRunner.java
  src/main/java/org/metalib/papifly/fx/github/git/
    GitService.java
    JGitService.java
    GitOperationException.java
    RollbackMode.java
  src/main/java/org/metalib/papifly/fx/github/github/
    GitHubApiService.java
    GitHubApiClientService.java
    GitHubRemoteParser.java
  src/main/java/org/metalib/papifly/fx/github/auth/
    CredentialsService.java
    TokenStore.java
    PreferencesTokenStore.java
  src/main/java/org/metalib/papifly/fx/github/model/
    RepoStatus.java
    BranchRef.java
    PullRequestRequest.java
    PullRequestResult.java
  src/main/resources/META-INF/services/
    org.metalib.papifly.fx.docking.api.ContentStateAdapter
  src/test/java/org/metalib/papifly/fx/github/
    JGitServiceTest.java
    GitHubRemoteParserTest.java
    GitHubToolbarViewModelTest.java
    GitHubToolbarPaneFxTest.java
    GitHubToolbarStateAdapterTest.java
```

## 6. Maven Integration Plan

## 6.1 Parent Aggregator Update

Add module in root `pom.xml`:

```xml
<modules>
    ...
    <module>papiflyfx-docking-github</module>
</modules>
```

## 6.2 Parent Dependency and Plugin Management

Follow repository rule: all dependency versions managed in parent.

```xml
<properties>
    ...
    <jgit.version>...</jgit.version>
    <github.api.version>...</github.api.version>
</properties>

<dependencyManagement>
    <dependencies>
        ...
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>${jgit.version}</version>
        </dependency>
        <dependency>
            <groupId>org.kohsuke</groupId>
            <artifactId>github-api</artifactId>
            <version>${github.api.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 6.3 Module `pom.xml` Skeleton

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.14-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-github</artifactId>
    <name>papiflyfx-docking-github</name>
    <description>GitHub toolbar component for PapiflyFX docking.</description>

    <dependencies>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>

        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
        </dependency>
        <dependency>
            <groupId>org.kohsuke</groupId>
            <artifactId>github-api</artifactId>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

## 7. Core Domain Contracts

## 7.1 Context and State

```java
package org.metalib.papifly.fx.github.api;

import java.net.URI;
import java.nio.file.Path;

public record GitHubRepoContext(
    URI remoteRepoUrl,
    Path localClonePath,
    DockToolbarPosition preferredPosition,
    String contentId
) {
    public enum DockToolbarPosition {
        TOP, BOTTOM
    }
}
```

```java
package org.metalib.papifly.fx.github.model;

public record RepoStatus(
    String currentBranch,
    String defaultBranch,
    boolean dirty,
    boolean detachedHead,
    boolean aheadOfRemote,
    boolean behindRemote
) {}
```

## 7.2 Git Service Contract

```java
package org.metalib.papifly.fx.github.git;

import org.metalib.papifly.fx.github.model.BranchRef;
import org.metalib.papifly.fx.github.model.RepoStatus;

import java.nio.file.Path;
import java.util.List;

public interface GitService {
    void open(Path repoPath);
    RepoStatus loadStatus();
    List<BranchRef> listBranches();
    void checkout(String branchName, boolean force);
    void createAndCheckout(String branchName, String startPoint);
    void commitAll(String message);
    void rollbackLastCommit(RollbackMode mode);
    void push(String remoteName, String branchName, char[] token);
    boolean isLastCommitPushed();
    void close();
}
```

```java
package org.metalib.papifly.fx.github.git;

public enum RollbackMode {
    REVERT,
    RESET_SOFT,
    RESET_HARD
}
```

## 7.3 GitHub API Contract

```java
package org.metalib.papifly.fx.github.github;

import org.metalib.papifly.fx.github.model.PullRequestRequest;
import org.metalib.papifly.fx.github.model.PullRequestResult;

public interface GitHubApiService {
    String fetchDefaultBranch(String owner, String repo, char[] token);
    PullRequestResult createPullRequest(String owner, String repo, PullRequestRequest request, char[] token);
}
```

## 8. Git Domain Implementation Details (JGit)

## 8.1 Repository Open and Validation

1. Validate provided path exists.
2. Resolve repository root with `RepositoryBuilder.findGitDir(...)`.
3. Keep one opened `Git` instance per toolbar instance.
4. Fail fast with typed `GitOperationException` for user-friendly messaging.

```java
public void open(Path repoPath) {
    try {
        Repository repository = new FileRepositoryBuilder()
            .findGitDir(repoPath.toFile())
            .build();
        this.git = new Git(repository);
    } catch (IOException ex) {
        throw new GitOperationException("Local repository not found: " + repoPath, ex);
    }
}
```

## 8.2 Branch Listing and Current Branch

```java
public List<BranchRef> listBranches() {
    try {
        List<Ref> local = git.branchList().call();
        List<Ref> remote = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call();

        Stream<BranchRef> localRefs = local.stream()
            .map(ref -> new BranchRef(shortName(ref.getName()), false));
        Stream<BranchRef> remoteRefs = remote.stream()
            .filter(ref -> !ref.getName().endsWith("/HEAD"))
            .map(ref -> new BranchRef(shortName(ref.getName()), true));

        return Stream.concat(localRefs, remoteRefs)
            .distinct()
            .sorted(Comparator.comparing(BranchRef::name))
            .toList();
    } catch (GitAPIException ex) {
        throw new GitOperationException("Failed to load branches", ex);
    }
}
```

## 8.3 Default Branch Resolution

Resolution order:

1. GitHub metadata (`repository.getDefaultBranch()`).
2. Local branch `main` if present.
3. Local branch `master` if present.
4. Upstream HEAD symbolic reference (if resolvable).
5. Fallback to current branch.

This combines source guidance from ChatGPT and Grok while staying deterministic when offline.

## 8.4 Checkout Existing Branch

Rule:

1. If working tree dirty, ask user for explicit confirmation before force checkout.
2. Default action is non-force checkout.

```java
public void checkout(String branchName, boolean force) {
    try {
        CheckoutCommand cmd = git.checkout().setName(branchName);
        if (force) {
            cmd.setForced(true);
        }
        cmd.call();
    } catch (GitAPIException ex) {
        throw new GitOperationException("Failed to checkout branch: " + branchName, ex);
    }
}
```

## 8.5 Create and Checkout New Branch

```java
public void createAndCheckout(String branchName, String startPoint) {
    try {
        git.checkout()
            .setCreateBranch(true)
            .setName(branchName)
            .setStartPoint(startPoint)
            .call();
    } catch (GitAPIException ex) {
        throw new GitOperationException("Failed to create branch: " + branchName, ex);
    }
}
```

## 8.6 Commit with Default Branch Guard

Guard conditions:

1. Current branch is default branch -> block commit.
2. No changes staged or unstaged -> block commit.
3. Empty message -> block commit.

```java
public void commitAll(String message) {
    if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("Commit message is required");
    }
    try {
        git.add().addFilepattern(".").call();
        git.commit().setMessage(message.trim()).call();
    } catch (GitAPIException ex) {
        throw new GitOperationException("Commit failed", ex);
    }
}
```

## 8.7 Rollback Last Commit (Safety-First)

Behavior matrix:

1. Last commit pushed -> default to `REVERT`.
2. Last commit not pushed -> allow `RESET_SOFT` or `RESET_HARD`.
3. `RESET_HARD` always shows destructive warning.

```java
public void rollbackLastCommit(RollbackMode mode) {
    try {
        ObjectId head = git.getRepository().resolve("HEAD");
        if (head == null) {
            throw new GitOperationException("No commit to rollback");
        }
        switch (mode) {
            case REVERT -> git.revert().include(head).call();
            case RESET_SOFT -> git.reset().setMode(ResetCommand.ResetType.SOFT).setRef("HEAD~1").call();
            case RESET_HARD -> git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD~1").call();
        }
    } catch (Exception ex) {
        throw new GitOperationException("Rollback failed", ex);
    }
}
```

## 8.8 Push

Use HTTPS remote and PAT:

```java
public void push(String remoteName, String branchName, char[] token) {
    CredentialsProvider cp = new UsernamePasswordCredentialsProvider("x-access-token", new String(token));
    try {
        Iterable<PushResult> results = git.push()
            .setRemote(remoteName)
            .setCredentialsProvider(cp)
            .add("refs/heads/" + branchName)
            .call();
        validatePushResults(results);
    } catch (GitAPIException ex) {
        throw new GitOperationException("Push failed", ex);
    }
}
```

## 9. GitHub API Integration Details

## 9.1 Remote URL Parsing

Support URL forms:

1. `https://github.com/owner/repo.git`
2. `git@github.com:owner/repo.git`
3. `ssh://git@github.com/owner/repo.git`

```java
public record RemoteCoordinates(String owner, String repo) {}

public RemoteCoordinates parse(String remote) {
    String normalized = remote.trim().replace(".git", "");
    if (normalized.startsWith("git@github.com:")) {
        String pair = normalized.substring("git@github.com:".length());
        String[] parts = pair.split("/");
        return new RemoteCoordinates(parts[0], parts[1]);
    }
    URI uri = URI.create(normalized.replace("ssh://git@github.com/", "https://github.com/"));
    String[] parts = uri.getPath().replaceFirst("^/", "").split("/");
    return new RemoteCoordinates(parts[0], parts[1]);
}
```

## 9.2 API Service with `org.kohsuke.github`

`org.kohsuke.github` avoids custom JSON mapping and keeps API code compact.

```java
public final class GitHubApiClientService implements GitHubApiService {

    @Override
    public String fetchDefaultBranch(String owner, String repo, char[] token) {
        try {
            GitHub gh = new GitHubBuilder()
                .withOAuthToken(new String(token))
                .build();
            GHRepository repository = gh.getRepository(owner + "/" + repo);
            return repository.getDefaultBranch();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load default branch from GitHub", ex);
        }
    }

    @Override
    public PullRequestResult createPullRequest(
        String owner,
        String repo,
        PullRequestRequest request,
        char[] token
    ) {
        try {
            GitHub gh = new GitHubBuilder()
                .withOAuthToken(new String(token))
                .build();
            GHRepository repository = gh.getRepository(owner + "/" + repo);
            GHPullRequest pr = repository.createPullRequest(
                request.title(),
                request.headBranch(),
                request.baseBranch(),
                request.body()
            );
            return new PullRequestResult(pr.getNumber(), pr.getHtmlUrl().toString());
        } catch (IOException ex) {
            throw new RuntimeException("Create PR failed", ex);
        }
    }
}
```

## 9.3 PR Preconditions

Before create PR:

1. Current branch must not equal base/default branch.
2. Branch should be pushed to remote (or prompt to push first).
3. Token must be available.

## 10. Authentication Plan

## 10.1 PAT-First Flow

1. User opens token dialog once.
2. Token is saved through `TokenStore`.
3. Push and PR operations request token from `CredentialsService`.
4. On 401/403, clear cache and re-prompt.

## 10.2 Credential Contracts

```java
public interface TokenStore {
    Optional<char[]> load(String key);
    void save(String key, char[] token);
    void clear(String key);
}
```

```java
public final class PreferencesTokenStore implements TokenStore {
    private final Preferences prefs = Preferences.userRoot().node("org.metalib.papifly.github");

    @Override
    public Optional<char[]> load(String key) {
        String encoded = prefs.get(key, null);
        return encoded == null ? Optional.empty() : Optional.of(encoded.toCharArray());
    }

    @Override
    public void save(String key, char[] token) {
        prefs.put(key, new String(token));
    }

    @Override
    public void clear(String key) {
        prefs.remove(key);
    }
}
```

Later enhancement path:

1. Replace token store implementation with OS keychain backed storage.
2. Add optional device flow path while reusing same `CredentialsService` contract.

## 11. UI and Interaction Design

## 11.1 Toolbar Layout

Left to right:

1. GitHub icon + repo hyperlink (`owner/repo`).
2. Branch combo + dirty indicator.
3. Action buttons:
   - `Checkout`
   - `New Branch`
   - `Commit`
   - `Rollback`
   - `Push`
   - `Create PR`
4. Status text / spinner.

## 11.2 Pane Skeleton

```java
public final class GitHubToolbarPane extends BorderPane implements DisposableContent {

    private final GitHubToolbarViewModel vm;
    private final Hyperlink repoLink = new Hyperlink();
    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final Label dirtyDot = new Label("●");
    private final Button checkoutBtn = new Button("Checkout");
    private final Button newBranchBtn = new Button("New Branch");
    private final Button commitBtn = new Button("Commit");
    private final Button rollbackBtn = new Button("Rollback");
    private final Button pushBtn = new Button("Push");
    private final Button prBtn = new Button("Create PR");
    private final Label status = new Label();
    private final ProgressIndicator busy = new ProgressIndicator();

    public GitHubToolbarPane(GitHubToolbarViewModel vm) {
        this.vm = vm;
        HBox actions = new HBox(8, checkoutBtn, newBranchBtn, commitBtn, rollbackBtn, pushBtn, prBtn);
        HBox left = new HBox(8, repoLink, branchCombo, dirtyDot);
        BorderPane row = new BorderPane();
        row.setLeft(left);
        row.setCenter(actions);
        row.setRight(new HBox(6, busy, status));
        setCenter(row);
        bind();
    }

    @Override
    public void dispose() {
        vm.dispose();
    }
}
```

## 11.3 Command Enable/Disable Rules

1. `Commit` disabled if on default branch OR no local changes OR busy.
2. `Push` disabled if not ahead OR busy.
3. `Create PR` disabled if current branch equals default branch OR busy.
4. `Rollback` disabled if no commits OR busy.

```java
commitBtn.disableProperty().bind(
    vm.busyProperty()
        .or(vm.onDefaultBranchProperty())
        .or(vm.hasNoChangesProperty())
);
```

## 11.4 Top/Bottom Mounting

Use existing host pattern around `DockManager.getRootPane()`:

```java
DockManager dm = new DockManager();
GitHubToolbarPane toolbar = factory.createToolbar(context);

BorderPane wrapper = new BorderPane();
wrapper.setCenter(dm.getRootPane());

if (context.preferredPosition() == GitHubRepoContext.DockToolbarPosition.TOP) {
    wrapper.setTop(toolbar);
} else {
    wrapper.setBottom(toolbar);
}

return wrapper;
```

This mirrors existing sample usage in the repository and aligns with ChatGPT recommendation.

## 12. ViewModel and Async Execution

## 12.1 Command Runner

```java
public final class CommandRunner implements AutoCloseable {

    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "github-toolbar-io");
        t.setDaemon(true);
        return t;
    });

    public <T> void run(
        Supplier<T> supplier,
        Consumer<T> onSuccess,
        Consumer<Throwable> onError
    ) {
        CompletableFuture.supplyAsync(supplier, io)
            .thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
            .exceptionally(ex -> {
                Platform.runLater(() -> onError.accept(unwrap(ex)));
                return null;
            });
    }

    @Override
    public void close() {
        io.shutdownNow();
    }
}
```

## 12.2 ViewModel Shape

```java
public final class GitHubToolbarViewModel implements AutoCloseable {

    private final CommandRunner runner;
    private final GitService gitService;
    private final GitHubApiService apiService;
    private final CredentialsService credentials;

    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty currentBranch = new SimpleStringProperty("");
    private final StringProperty defaultBranch = new SimpleStringProperty("");
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final StringProperty statusText = new SimpleStringProperty("");

    public void refresh() {
        busy.set(true);
        runner.run(
            gitService::loadStatus,
            status -> {
                currentBranch.set(status.currentBranch());
                defaultBranch.set(status.defaultBranch());
                dirty.set(status.dirty());
                busy.set(false);
            },
            error -> {
                statusText.set(error.getMessage());
                busy.set(false);
            }
        );
    }
}
```

## 13. Dialog Flows

## 13.1 New Branch Dialog

Fields:

1. Branch name (required).
2. Start point (default current branch).

Validation:

1. Reject blank name.
2. Reject invalid refs.
3. Reject existing branch unless explicit overwrite option is supported (not in v1).

## 13.2 Commit Dialog

Fields:

1. Commit message (required).
2. Optional checkbox `Stage all`.

Guard:

1. On default branch -> show reason and keep disabled.

## 13.3 Rollback Dialog

Fields:

1. Last commit hash + subject (read-only).
2. Mode options:
   - `Revert (safe)`
   - `Reset soft`
   - `Reset hard`

Rule:

1. If last commit appears pushed, preselect and recommend `Revert`.

## 13.4 Create PR Dialog

Fields:

1. Title (required).
2. Body (optional).
3. Base branch (default = resolved default branch).
4. Head branch (default = current branch).
5. Checkbox `Open PR in browser`.

## 14. Docking Persistence Integration

Even if toolbar is frequently app chrome, support persistence so host can restore context.

## 14.1 Factory + State Adapter

```java
public final class GitHubToolbarFactory implements ContentFactory {
    public static final String FACTORY_ID = "github-toolbar";

    private final Supplier<GitHubToolbarPane> supplier;

    public GitHubToolbarFactory(Supplier<GitHubToolbarPane> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Node create(String factoryId) {
        return FACTORY_ID.equals(factoryId) ? supplier.get() : null;
    }
}
```

```java
public final class GitHubToolbarStateAdapter implements ContentStateAdapter {
    @Override
    public String getTypeKey() { return GitHubToolbarFactory.FACTORY_ID; }

    @Override
    public int getVersion() { return 1; }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof GitHubToolbarPane pane)) return Map.of();
        GitHubToolbarState state = pane.captureState();
        return Map.of(
            "remoteRepoUrl", state.remoteRepoUrl().toString(),
            "localClonePath", state.localClonePath().toString(),
            "preferredPosition", state.preferredPosition().name()
        );
    }

    @Override
    public Node restore(LeafContentData content) {
        // Rebuild through injected factory/provider in production wiring.
        return null;
    }
}
```

Service registration file:

```text
org.metalib.papifly.fx.github.api.GitHubToolbarStateAdapter
```

## 15. Error Handling Model

## 15.1 Error Categories

1. Repository not found.
2. Detached HEAD.
3. Dirty worktree blocks checkout.
4. Commit blocked on default branch.
5. Authentication failed (401/403).
6. Push rejected (non-fast-forward or branch protection).
7. PR creation rejected (validation).
8. Rate-limited GitHub API.

## 15.2 User-Facing Messaging

1. Keep one-line summary in status bar.
2. Provide expandable technical details in dialog.
3. Keep raw exception logging for troubleshooting.

Example:

```java
statusText.set("Push rejected: remote contains newer commits");
errorDetails.set("""
Command: push origin feature/toolbar
Reason: non-fast-forward
Suggestion: pull/rebase or create a new branch
""");
```

## 16. Testing Plan

## 16.1 Unit Tests (No UI)

`JGitServiceTest`:

1. Open repo and load status.
2. List branches.
3. Checkout existing branch.
4. Create + checkout branch.
5. Commit blocked on default branch (guard in ViewModel).
6. Revert and reset rollback paths.
7. Push error handling with mocked remote rejection.

Use temporary repositories:

```java
@Test
void createAndCheckoutBranch() throws Exception {
    Path repoDir = Files.createTempDirectory("git-toolbar-test");
    Git git = Git.init().setDirectory(repoDir.toFile()).call();
    Files.writeString(repoDir.resolve("README.md"), "x");
    git.add().addFilepattern(".").call();
    git.commit().setMessage("init").call();

    JGitService service = new JGitService();
    service.open(repoDir);
    service.createAndCheckout("feature/test", "HEAD");

    assertEquals("feature/test", service.loadStatus().currentBranch());
}
```

`GitHubRemoteParserTest`:

1. Parse HTTPS URL.
2. Parse SCP SSH URL.
3. Parse SSH URI.
4. Reject malformed remote.

`GitHubToolbarViewModelTest`:

1. Button enable/disable logic.
2. Busy flag transitions.
3. Error propagation.

## 16.2 Integration Tests

Optional profile with env token and disposable test repo:

1. `GITHUB_TEST_TOKEN`
2. `GITHUB_TEST_OWNER`
3. `GITHUB_TEST_REPO`

Scenarios:

1. Fetch default branch from live GitHub.
2. Create PR against sandbox repo branch.

## 16.3 TestFX UI Tests

`GitHubToolbarPaneFxTest`:

1. Toolbar controls render.
2. Commit button disabled on default branch.
3. Status text updates after simulated command.
4. Branch combo updates on refresh.

## 16.4 Persistence Tests

`GitHubToolbarStateAdapterTest`:

1. `saveState` map keys and values.
2. Version marker retained.
3. Restore path delegates correctly.

## 17. Incremental Delivery Plan

## Phase 1: Module Bootstrap

1. Add module folder + POM.
2. Add parent module entry and managed dependencies.
3. Add empty packages and baseline tests.

## Phase 2: Local Git Read-Only Status

1. Implement repo open + status.
2. Implement branch listing.
3. Build toolbar shell with repo link + branch + dirty indicator.

## Phase 3: Branch and Commit Workflows

1. Checkout existing branch.
2. Create and checkout new branch.
3. Commit dialog and default-branch guard.

## Phase 4: Rollback and Push

1. Add rollback dialog and modes.
2. Add pushed-state heuristic.
3. Add push flow with token prompt and failure mapping.

## Phase 5: PR Creation

1. Parse owner/repo from remote.
2. Add default branch fetch.
3. Add PR dialog and API call.
4. Optional open-in-browser after success.

## Phase 6: Persistence, Samples, and Polish

1. Add state adapter and service file.
2. Add sample wiring in `papiflyfx-docking-samples`.
3. Add end-to-end UI tests.
4. Polish messages, progress text, and guardrails.

## 18. Acceptance Criteria

1. Toolbar mounts at top or bottom based on context.
2. Repository link opens system browser.
3. Branch data and dirty indicator update correctly.
4. Checkout and new branch creation work for local clone.
5. Commit action is blocked on default branch.
6. Rollback defaults to safe behavior when last commit is pushed.
7. Push works with PAT and reports failures clearly.
8. PR creation works with selected base/head and returns URL.
9. All long-running actions are non-blocking for JavaFX thread.
10. Unit tests and TestFX tests cover key state transitions.

## 19. Risks and Mitigations

1. Risk: ambiguous default branch while offline.
   Mitigation: deterministic local fallback order with visible status note.
2. Risk: reset modes can destroy local work.
   Mitigation: explicit warning + safe default `Revert`.
3. Risk: token leakage.
   Mitigation: isolate token store API, clear memory buffers where practical, avoid logging tokens.
4. Risk: remote parsing edge cases.
   Mitigation: parser unit tests for all URL forms and invalid input.
5. Risk: UI freezes from long Git operations.
   Mitigation: strict command runner and busy-state gating.

## 20. Source Traceability

## 20.1 Adopted from ChatGPT source

1. Top/bottom toolbar mounting model.
2. JGit service abstraction and operation set.
3. Default branch detection strategy with fallback.
4. Safe rollback decision model.
5. PAT-first authentication strategy.
6. Async background operation requirement.

## 20.2 Adopted from Grok source

1. Practical toolbar action set and dialog workflow.
2. DockLeaf/app-toolbar host integration idea.
3. GitHub API client option for PR creation.
4. Explicit error categories and guardrails.

## 20.3 Adopted from Gemini source

1. No direct technical additions (file contains heading only).

## 21. Implementation Notes for First PR

First PR should only include:

1. Module scaffold + parent POM updates.
2. `GitService` + parser + model DTOs.
3. Read-only toolbar (`repo link`, `branch`, `dirty indicator`).
4. Unit tests for parser and status loading.

Second PR should add write operations (`checkout`, `new branch`, `commit`) and associated UI dialogs. This keeps review size controlled and lowers rollback risk.
