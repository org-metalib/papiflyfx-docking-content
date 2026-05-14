Below is a **research + implementation plan** for a new PapiflyFX docking component module named **`papiflyfx-docking-github`** that adds a **dockable GitHub/Git toolbar** (attachable to the top or bottom of the application window) with branch, commit, push, and PR workflows.

This plan assumes the parent project stays aligned with the current repo positioning (multi-module Maven build, Java 25 / JavaFX 23.0.1). ([GitHub][1])

---

## 1) Module definition

**GitHub module name**

* `papiflyfx-docking-github`

**High-level purpose**

* Provide a reusable **Docking “application toolbar” contribution** for repository-aware actions:

    * Repository link (clickable)
    * Current branch (when repo is available locally)
    * Switch branches
    * Checkout/create new branch
    * Commit (blocked on default branch)
    * Roll back last commit
    * Push
    * Create Pull Request

---

## 2) UX / interaction model

### 2.1 Toolbar placement (top/bottom)

* The component should be able to render as a **single JavaFX `Node`** intended to be mounted by the host app:

    * `TOP` area (typical IDE-style)
    * `BOTTOM` area (status-bar-like)

**Recommendation**

* Model this as a “toolbar contribution” that the docking host can mount in a standard container (e.g., `BorderPane.setTop(node)` or `setBottom(node)`), while still being distributed as a docking module.

### 2.2 Visual layout (suggested)

From left to right:

1. **GitHub icon + Repo link**

    * Displays `owner/repo` (or full URL)
    * Click opens system browser to the remote repo.

2. **Local repo status**

    * If no local clone configured / found: “Not cloned” + a “Locate…” button (optional)
    * If local clone exists:

        * **Branch selector** (combo box): shows current branch + local branches + remote tracking branches
        * Dirty indicator: e.g., `●` when there are uncommitted changes

3. Actions group (buttons / menu)

    * **Switch branch** (from combo) / **Checkout**
    * **New branch…**
    * **Commit…** (disabled if on default branch)
    * **Rollback last commit**
    * **Push**
    * **Create PR…**

### 2.3 Key behavior constraints

* **Commit is disabled** if current branch is `main/master/<default>`.

    * Default branch should be determined from remote metadata when possible; otherwise fall back to `{main, master}` heuristics.
* **Rollback last commit**

    * If last commit **not pushed**: allow “reset HEAD~1” equivalent (history rewrite, local only).
    * If last commit **already pushed**: prefer “revert last commit” behavior (non-rewriting), or show warning + choice.
    * (These semantics mirror common Git guidance: `revert` creates a new commit, while `reset` rewrites history.) ([Atlassian][2])

---

## 3) Architecture overview

Split into three layers:

### 3.1 UI layer (JavaFX)

* `GitHubDockingToolbar` (custom control)

    * Binds to a `GitHubToolbarViewModel`
    * Displays status, enables/disables actions, shows progress and error banners

### 3.2 Domain layer (Git operations)

Use **JGit** (recommended) rather than shelling out to `git`:

* Pros: no external dependency, easier cross-platform, structured exceptions
* Cons: you must implement authentication handling for push/fetch carefully

**Core service**

* `GitService`

    * `getCurrentBranch()`
    * `listBranches()`
    * `checkoutBranch(name)`
    * `createAndCheckoutBranch(name, startPoint)`
    * `getWorkingTreeStatus()` (dirty, staged, untracked)
    * `commit(message, author?)`
    * `rollbackLastCommit(mode)` where `mode ∈ {RESET_SOFT, RESET_HARD, REVERT}`
    * `push()`

### 3.3 Integration layer (GitHub PR creation)

For **Create Pull Request**, use **GitHub REST API**:

* Endpoint is under “Pull requests” REST endpoints; PR creation is done by POSTing to `/repos/{owner}/{repo}/pulls`. ([GitHub Docs][3])

**Core service**

* `GitHubApiService`

    * `getRepoDefaultBranch(owner, repo)`
    * `createPullRequest(owner, repo, title, head, base, body?)`

---

## 4) Repository discovery & configuration

The toolbar needs to know:

* Remote repo URL (for clickable link and API calls)
* Local clone path (optional but required for branch/commit/push)

**Config options**

1. **Explicit configuration**

    * App passes a `GitHubRepoContext`:

        * `remoteUrl` (required)
        * `localPath` (optional)
2. **Auto-discovery (optional)**

    * If localPath is set, parse `.git/config` to determine `origin` URL
    * If remoteUrl is set, but localPath not set, allow “Locate local clone…” file chooser

---

## 5) Branch operations (switch + checkout new branch)

### 5.1 Switching branches

* Provide a branch combo box + “Checkout” action
* Must enforce:

    * No checkout if working tree has uncommitted changes unless user confirms (stash/commit/discard choice)

### 5.2 Checkout new branch

* UI: “New branch…” dialog:

    * branch name
    * start point (current HEAD or select another branch/tag)

This maps to typical Git semantics (create branch + checkout). ([Git SCM][4])

---

## 6) Commit rules (block on default branch)

### 6.1 Determine default branch

Prefer:

1. Query GitHub to get default branch (requires auth if repo private; may be public without auth depending on rate limits)
2. If GitHub not reachable, fallback:

    * If branch named `main` exists: default = `main`
    * else if `master` exists: default = `master`
    * else treat current as default only if it matches remote HEAD

### 6.2 Commit flow

* Dialog:

    * Commit message (required)
    * “Stage all” toggle (or include a file-level staging UI later)
* Disable commit button if:

    * on default branch
    * nothing to commit
* After commit:

    * refresh status, enable “Rollback last commit”

---

## 7) Rollback last commit (safety-first behavior)

### 7.1 Modes

* **Safe default**: “Revert last commit” (non-destructive; creates a new commit) ([gitkraken.com][5])
* **Power-user option**: “Reset HEAD~1” (destructive; only if not pushed, or if user explicitly confirms)

### 7.2 UI suggestion

“Rollback last commit…” dialog:

* If HEAD is pushed (best-effort detection):

    * Recommend **Revert**
* Else:

    * Offer Reset Soft / Reset Hard / Revert
* Always show:

    * commit hash + message being rolled back

---

## 8) Push changes (remote auth considerations)

Push requires credentials. Options:

### Option A (best UX for desktop): GitHub OAuth device flow

* Implement OAuth device flow for a desktop application:

    * GitHub supports device flow and requires enabling it for the OAuth/GitHub App. ([GitHub Docs][6])
* Store access token securely (OS keychain if possible; otherwise encrypted local storage)

### Option B (simpler implementation): Personal Access Token (PAT)

* User pastes a PAT once; stored securely
* Use PAT for:

    * GitHub REST API calls (PR creation)
    * Git push authentication (via HTTPS remote)

**Recommendation**

* Implement **PAT-first** (fast path), then add **device flow** as an enhancement.

---

## 9) Create Pull Request flow (GitHub REST API)

### 9.1 Preconditions

* Must have remote repo owner/name
* Must have a branch pushed to remote (head branch exists remotely)
* Must know base branch (default branch)

### 9.2 UI

“Create Pull Request…” dialog:

* Title (required)
* Body (optional)
* Base branch (default selected)
* Head branch (current selected)
* Checkbox: “Open PR in browser after creation”

### 9.3 API call

* Use GitHub REST “Create a pull request” endpoint under pull request endpoints. ([GitHub Docs][3])

---

## 10) Concurrency, responsiveness, and failure handling

### 10.1 Never block the FX thread

* All git + network calls run in background tasks
* UI binds to:

    * `busy` flag
    * `progress` text (“Fetching branches…”, “Pushing…”, etc.)
    * `lastError` with “Copy details” button

### 10.2 Error categories

* “Local repo not found”
* “Detached HEAD”
* “Uncommitted changes block checkout”
* Auth failures (401/403)
* Rate limiting (GitHub API)
* Push rejected (non-fast-forward, protected branch)

---

## 11) Proposed public API (module-facing)

Minimal, host-friendly integration:

```java
public record GitHubRepoContext(
    URI remoteRepoUrl,
    Path localClonePath // nullable
) {}

public interface DockingToolbarContribution {
    Node toolbarNode();               // the UI
    DockPosition preferredPosition();  // TOP or BOTTOM
    void setContext(GitHubRepoContext ctx);
}
```

Provide:

* `GitHubToolbarContribution implements DockingToolbarContribution`

---

## 12) Maven/module setup (suggested)

* Add module folder:

    * `papiflyfx-docking-github/`
* Add to root `pom.xml` aggregator modules list
* Dependencies (recommended minimal):

    * JGit (Git operations)
    * Java HTTP client (built-in) for GitHub REST calls (no extra dependency)
    * (Optional later) a small JSON library if you don’t already have one in the ecosystem

---

## 13) Testing strategy

### 13.1 Unit tests

* `GitService` with temporary local repositories (init, commits, branches)
* Rollback behaviors (reset/revert scenarios)

### 13.2 Integration tests

* Optional “manual test mode”:

    * run against a real repo + token provided via env var

### 13.3 UI tests

* Basic TestFX:

    * verifies enable/disable logic on buttons based on branch and dirty state

---

## 14) Implementation phases (incremental delivery)

**Phase 1 — Read-only UI**

* Toolbar renders
* Repo link opens browser
* Detect local repo, show current branch, list branches

**Phase 2 — Branch switching**

* Checkout existing branch
* Create + checkout new branch

**Phase 3 — Commit + rollback**

* Commit dialog
* Enforce “no commit on default branch”
* Rollback dialog with revert/reset logic

**Phase 4 — Push**

* Auth (PAT)
* Push with progress + errors

**Phase 5 — PR creation**

* GitHub REST integration + PR dialog + open-in-browser

**Phase 6 — Polish**

* Better default-branch detection (remote metadata)
* Protected branch detection and clearer messaging
* Optional device-flow auth

---

[1]: https://github.com/org-metalib/papiflyfx-docking "GitHub - org-metalib/papiflyfx-docking: PapiflyFX Docking Framework · GitHub"
[2]: https://www.atlassian.com/git/tutorials/resetting-checking-out-and-reverting?utm_source=chatgpt.com "Resetting, Checking Out & Reverting | Atlassian Git Tutorial"
[3]: https://docs.github.com/en/rest/pulls?utm_source=chatgpt.com "REST API endpoints for pull requests"
[4]: https://git-scm.com/docs/git-checkout?utm_source=chatgpt.com "Git - git-checkout Documentation"
[5]: https://www.gitkraken.com/learn/git/problems/revert-git-commit?utm_source=chatgpt.com "Git Revert Commit | Solutions to Git Problems"
[6]: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps?utm_source=chatgpt.com "Authorizing OAuth apps"
