# papiflyfx-docking-github — Unified Implementation Plan

This plan merges:
- `spec/papiflyfx-docking-github/plan-codex.md`
- `spec/papiflyfx-docking-github/plan-copilot-opus.md`
- `spec/papiflyfx-docking-github/plan-copilot-sonnet.md`

## 1. Objective

Implement a new module `papiflyfx-docking-github` that provides a toolbar component for GitHub workflows:

1. Show clickable repository link.
2. Show current branch + dirty status for local clones.
3. Checkout branch / create and checkout new branch.
4. Commit changes, but block commit on default branch.
5. Roll back last commit with safe behavior.
6. Push changes.
7. Create pull requests.
8. Mount toolbar at top or bottom of host window.

## 2. Conflict Resolution Matrix

| Topic | Conflict in source plans | Unified decision |
|---|---|---|
| GitHub client | Codex used `org.kohsuke:github-api`; Opus/Sonnet used `java.net.http` | Use `java.net.http.HttpClient` + minimal JSON extraction (no new runtime dependency). |
| Local git abstraction | `GitService` (Codex/Opus) vs `GitRepository` (Sonnet) | Use `GitRepository` interface + `JGitRepository` impl for clean test seams. |
| Public UI type | `GitHubToolbarPane` (Codex) vs `GitHubToolbar` (Opus/Sonnet) | Use `GitHubToolbar extends HBox`; keep host-facing contribution wrapper. |
| Credential storage | In-memory only (Opus) vs preferences persistence (Codex/Sonnet) | Support both: `PatCredentialStore` (default) + `PreferencesTokenStore` (opt-in). |
| Remote-only mode | Explicit in Opus/Sonnet; not primary in Codex | Support remote-only mode (`localClonePath == null`); disable local git actions automatically. |
| Rollback policy | Codex recommended revert for pushed commits; Opus/Sonnet disabled reset for pushed commits | Enforce strict rule: if pushed, only `REVERT`; otherwise allow `RESET_SOFT`/`RESET_HARD`. |
| Docking persistence | Codex included `ContentStateAdapter`; Opus/Sonnet did not | Defer adapter to Phase 6 (optional hardening item), keep v1 focused on toolbar behavior. |
| Module coupling to docks | Codex showed `docks` compile dependency; Opus/Sonnet used test scope | Keep `papiflyfx-docking-docks` in `test` scope only (matches existing module pattern). |

## 3. Scope

### 3.1 In scope (v1)

1. New Maven module `papiflyfx-docking-github`.
2. Toolbar UI + ViewModel + dialogs.
3. JGit-backed local operations (status, branch, checkout, create branch, commit, rollback, push).
4. GitHub REST operations (default branch, PR creation).
5. PAT management with pluggable credential store.
6. Unit tests + headless TestFX coverage.

### 3.2 Out of scope (v1)

1. Full staging/index manipulation UI.
2. Rebase/cherry-pick/merge UI.
3. OAuth device-flow wizard.
4. Stash workflow automation.
5. Mandatory docking content-state persistence.

## 4. Module & Maven Plan

### 4.1 Root `pom.xml` updates

```xml
<modules>
    ...
    <module>papiflyfx-docking-github</module>
</modules>

<properties>
    ...
    <jgit.version>7.2.0.202503040940-r</jgit.version>
</properties>

<dependencyManagement>
    <dependencies>
        ...
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>${jgit.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 4.2 `papiflyfx-docking-github/pom.xml`

Follow same pattern as `papiflyfx-docking-tree` and `papiflyfx-docking-code`:

1. Compile dependencies: `papiflyfx-docking-api`, `javafx-base`, `javafx-graphics`, `javafx-controls`, `org.eclipse.jgit`.
2. Test dependencies: `papiflyfx-docking-docks`, JUnit 5, TestFX, Monocle.
3. Surefire config + JavaFX exports/opens + `testfx.headless` profile.

## 5. Package Layout

```text
papiflyfx-docking-github/
  src/main/java/org/metalib/papifly/fx/github/
    api/
      GitHubRepoContext.java
      GitHubToolbar.java
      GitHubToolbarContribution.java
    model/
      BranchRef.java
      RepoStatus.java
      CommitInfo.java
      RollbackMode.java
      PullRequestDraft.java
      PullRequestResult.java
    auth/
      CredentialStore.java
      PatCredentialStore.java
      PreferencesTokenStore.java
    git/
      GitRepository.java
      JGitRepository.java
      GitOperationException.java
    github/
      RemoteUrlParser.java
      GitHubApiService.java
      GitHubApiException.java
    ui/
      CommandRunner.java
      GitHubToolbarViewModel.java
      dialog/
        CommitDialog.java
        NewBranchDialog.java
        RollbackDialog.java
        PullRequestDialog.java
        TokenDialog.java
        DirtyCheckoutAlert.java
  src/test/java/org/metalib/papifly/fx/github/...
```

## 6. Core Contracts

### 6.1 Context

```java
public record GitHubRepoContext(
        URI remoteUrl,
        Path localClonePath,
        String owner,
        String repo
) {
    public static GitHubRepoContext of(URI remoteUrl, Path localClonePath) {
        var c = RemoteUrlParser.parse(remoteUrl.toString());
        return new GitHubRepoContext(remoteUrl, localClonePath, c.owner(), c.repo());
    }

    public static GitHubRepoContext remoteOnly(URI remoteUrl) {
        return of(remoteUrl, null);
    }

    public boolean hasLocalClone() {
        return localClonePath != null;
    }
}
```

### 6.2 Git repository contract

```java
public interface GitRepository extends AutoCloseable {
    RepoStatus loadStatus();
    List<BranchRef> listBranches();
    void checkout(String branchName, boolean force);
    void createAndCheckout(String branchName, String startPoint);
    CommitInfo commitAll(String message);
    CommitInfo getHeadCommit();
    void rollback(RollbackMode mode);
    void push(String remoteName);
    boolean isHeadPushed();
    String detectDefaultBranch();
    @Override void close();
}
```

### 6.3 GitHub API contract

```java
public final class GitHubApiService {
    public String fetchDefaultBranch(String owner, String repo);
    public PullRequestResult createPullRequest(String owner, String repo, PullRequestDraft draft);
}
```

## 7. Git Layer (JGit) Behavior

`JGitRepository` responsibilities:

1. Open and validate repository from `localClonePath`.
2. Load `RepoStatus` (current/default branch, detached, ahead/behind, dirty sets).
3. Branch listing: local + remote refs, filter remote `HEAD`, include current marker.
4. Checkout: normal by default; forced only after explicit dirty-tree confirmation.
5. Create and checkout branch from user-provided start point.
6. Commit: stage tracked + untracked changes, message validation.
7. Rollback:
   - pushed commit -> only `REVERT` is permitted by UI.
   - local-only commit -> `RESET_SOFT`, `RESET_HARD`, `REVERT`.
8. Push to `origin` with PAT-derived JGit credentials; map push rejections to user-friendly errors.

Default branch detection priority:

1. GitHub REST (`/repos/{owner}/{repo}` `default_branch`).
2. JGit remote `HEAD` via `ls-remote`.
3. Local `main`, then local `master`.
4. Current branch fallback.

## 8. GitHub REST Layer

Use `java.net.http.HttpClient` with:

1. `Accept: application/vnd.github+json`
2. `X-GitHub-Api-Version: 2022-11-28`
3. Optional `Authorization: Bearer <token>`

Supported endpoints:

1. `GET /repos/{owner}/{repo}` -> `default_branch`.
2. `POST /repos/{owner}/{repo}/pulls` -> PR number + URL.

Map HTTP failures to typed `GitHubApiException`:

1. 401/403: authentication error.
2. 422: validation error (head/base/title issues).
3. 429/5xx: transient/limit errors.

## 9. Authentication Plan

### 9.1 Credential abstraction

```java
public interface CredentialStore {
    Optional<String> getToken();
    void setToken(String token);
    void clearToken();

    default boolean isAuthenticated() { return getToken().isPresent(); }
    default CredentialsProvider toJGitCredentials() { ... }
}
```

### 9.2 Implementations

1. `PatCredentialStore`: thread-safe in-memory.
2. `PreferencesTokenStore`: optional persistence using `java.util.prefs`.

### 9.3 UX flow

1. Toolbar includes token button (opens `TokenDialog`).
2. On save, ViewModel updates auth state and rebinds JGit credentials.
3. On 401/403, show error and keep token entry flow immediately available.

## 10. ViewModel & Async Execution

`GitHubToolbarViewModel` is the single source of truth:

1. Observable state: `currentBranch`, `defaultBranch`, `dirty`, `busy`, `statusText`, `errorText`, `authenticated`, `localAvailable`, `headCommit`, `branches`.
2. Derived bindings:
   - commit disabled on default branch / clean tree / busy.
   - push disabled when unauthenticated / no local clone / busy.
   - PR disabled when unauthenticated / busy / current == default.
3. Commands: `refresh`, `switchBranch`, `createBranch`, `commit`, `rollback`, `push`, `createPullRequest`, `saveToken`.
4. Execute blocking calls via `CommandRunner` (single-thread executor + FX-thread callbacks).

## 11. UI & Dialog Design

### 11.1 Toolbar layout

```text
[owner/repo link] | [branch + dirty dot + combo + checkout + new branch]
| [commit + rollback + push] | [create PR + token] .... [spinner + status + error]
```

### 11.2 Mounting

Expose `GitHubToolbarContribution` with `Position { TOP, BOTTOM }` and `toolbarNode()` for host `BorderPane.setTop/setBottom`.

### 11.3 Dialogs

1. `NewBranchDialog`: name + start point.
2. `CommitDialog`: message required.
3. `RollbackDialog`: mode selection with pushed-commit safety.
4. `PullRequestDialog`: title/body/head/base + open-in-browser option.
5. `TokenDialog`: PAT entry.
6. `DirtyCheckoutAlert`: confirm/cancel forced checkout on dirty tree.

## 12. UX Guardrails

1. Commit on default branch is always blocked.
2. Rollback on pushed commit only allows `REVERT`.
3. Dirty checkout requires explicit user confirmation for force.
4. Push and PR require authentication.
5. All git/network operations are non-blocking for FX thread.

## 13. Error Handling Model

Primary categories:

1. Repository not found / invalid local path.
2. Detached HEAD.
3. Dirty checkout blocked.
4. Commit blocked by policy.
5. Push rejected (non-fast-forward / remote changed / protected branch).
6. GitHub API auth / validation / rate-limit / network failures.

Presentation model:

1. One-line status + one-line error in toolbar.
2. Dialog warnings for destructive actions.
3. No token values in logs or user-visible error text.

## 14. Testing Strategy

### 14.1 Unit tests

1. `RemoteUrlParserTest`: HTTPS/SCP/SSH parsing.
2. `JGitRepositoryTest`: branch ops, commit, rollback modes, push rejection mapping.
3. `GitHubApiServiceTest`: JSON extraction + HTTP status mapping.
4. `GitHubToolbarViewModelTest`: state transitions, enable/disable rules, auth updates.

### 14.2 UI tests (TestFX, headless)

1. Toolbar renders expected controls.
2. Remote-only mode disables local actions.
3. Commit button disabled on default branch.
4. Push/PR buttons disabled until token provided.

### 14.3 Optional integration tests

Use env vars (`GITHUB_TEST_TOKEN`, `GITHUB_TEST_OWNER`, `GITHUB_TEST_REPO`) for live default-branch and PR tests in disposable repos.

## 15. Incremental Delivery Phases

### Phase 1: Scaffold + read-only status

1. Module + pom wiring.
2. Context/model/parser.
3. `JGitRepository` read-only status/branches/head.
4. Toolbar shell + theme binding + base tests.

### Phase 2: Branch operations

1. Checkout/create branch.
2. New-branch + dirty-checkout dialogs.
3. ViewModel wiring + branch tests.

### Phase 3: Commit + rollback

1. Commit flow + default-branch guard.
2. Rollback dialog + safety policy.
3. Unit tests for all rollback modes.

### Phase 4: Push + auth

1. Credential store implementations.
2. Token dialog + push flow.
3. Push rejection handling + tests.

### Phase 5: Pull requests

1. GitHub API service.
2. PR dialog + open-in-browser option.
3. API and ViewModel tests.

### Phase 6: Hardening + optional persistence

1. Retry/backoff for transient API errors.
2. Optional `ContentStateAdapter` for toolbar context restore.
3. Optional keychain-backed credentials and/or OAuth device flow.

## 16. Acceptance Criteria

1. Toolbar mounts at top or bottom via host wrapper.
2. Repo link opens browser.
3. Branch + dirty status updates correctly.
4. Branch checkout and new branch creation work for local clone.
5. Commit is blocked on default branch.
6. Rollback enforces safe behavior for pushed commits.
7. Push works with PAT and reports common rejection reasons.
8. PR creation succeeds and returns/open URL.
9. No long-running operation blocks FX thread.
10. Unit + TestFX suites pass in headless mode.

## 17. Clarifications Requested

The merged plan can proceed as written, but these two decisions should be confirmed before implementation starts:

1. Confirm `java.net.http` as the canonical GitHub integration (instead of `org.kohsuke:github-api`).
2. Confirm whether toolbar state persistence via `ContentStateAdapter` is required in v1 or should remain Phase 6 optional.

## 18. Implementation Status

### Clarifications

- [x] `java.net.http` kept as canonical GitHub integration.
- [x] `ContentStateAdapter` support implemented in the module.

### Phase 1: Scaffold + read-only status

- [x] Module + pom wiring.
- [x] Context/model/parser.
- [x] `JGitRepository` read-only status/branches/head.
- [x] Toolbar shell + theme binding + base tests.

### Phase 2: Branch operations

- [x] Checkout/create branch.
- [x] New-branch + dirty-checkout dialogs.
- [x] ViewModel wiring + branch tests.

### Phase 3: Commit + rollback

- [x] Commit flow + default-branch guard.
- [x] Rollback dialog + safety policy.
- [x] Unit tests for rollback modes.

### Phase 4: Push + auth

- [x] Credential store implementations.
- [x] Token dialog + push flow.
- [x] Push rejection handling + tests.

### Phase 5: Pull requests

- [x] GitHub API service.
- [x] PR dialog + open-in-browser option.
- [x] API and ViewModel tests.

### Phase 6: Hardening + optional persistence

- [x] Retry/backoff for transient API errors.
- [x] Optional `ContentStateAdapter` for toolbar context restore.
- [x] Optional keychain-backed credentials (`KeychainTokenStore`).

### Acceptance Criteria

- [x] Toolbar mounts at top or bottom via host wrapper.
- [x] Repo link opens browser.
- [x] Branch + dirty status updates correctly.
- [x] Branch checkout and new branch creation work for local clone.
- [x] Commit is blocked on default branch.
- [x] Rollback enforces safe behavior for pushed commits.
- [x] Push works with PAT and reports common rejection reasons.
- [x] PR creation succeeds and returns/open URL.
- [x] No long-running operation blocks FX thread.
- [x] Unit + TestFX suites pass in headless mode.
