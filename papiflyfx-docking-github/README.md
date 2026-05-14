# papiflyfx-docking-github

A GitHub workflow toolbar for PapiflyFX docking applications.

## Features

- Clickable repository link (`owner/repo`)
- Current branch and dirty-state indicator for local clones
- Checkout existing branch
- Create and checkout new branch
- Commit changes with default-branch protection
- Roll back last commit (revert-only when head is pushed)
- Push branch to remote
- Create pull request via GitHub REST API
- PAT authentication with pluggable token stores
- Toolbar mounting wrapper for top/bottom host placement
- Settings panel integration via `GitHubCategory`
- Theme support with light/dark mode mapping
- Shared pills, chips, popup surfaces, and status-slot primitives from `papiflyfx-docking-api`
- Ribbon integration via `RibbonProvider` with GitHub command groups (`Sync`, `Branches`, `Collaborate`, `State`)

## Maven dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-github</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick start

```java
GitHubRepoContext context = GitHubRepoContext.of(
    URI.create("https://github.com/org-metalib/papiflyfx-docking"),
    Path.of("/workspace/papiflyfx-docking")
);

GitHubToolbarContribution contribution = new GitHubToolbarContribution(
    context,
    GitHubToolbarContribution.Position.TOP
);

BorderPane root = new BorderPane();
contribution.mount(root);
```

## Remote-only mode

```java
GitHubRepoContext context = GitHubRepoContext.remoteOnly(
    URI.create("https://github.com/org-metalib/papiflyfx-docking")
);

GitHubToolbar toolbar = new GitHubToolbar(context);
```

In remote-only mode, local git actions are disabled and PR/auth actions remain available.

## Theme And UI Standards

The GitHub toolbar keeps its Git-specific behavior local while using the shared UI primitives introduced by the UI standardization rollout.

- `GitHubToolbar.bindThemeProperty(...)` keeps `Theme` as the only runtime theme input.
- Repository and ref triggers use the shared `UiPillButton` base.
- Brand, secondary, and error chips use shared chip variants from `UiChipLabel` and `UiChipVariant`.
- Inline status content uses `UiStatusSlot`.
- Ref popups and dialogs consume the shared popup-surface tokens and 4px spacing grid before layering GitHub-specific semantics on top.

## Module architecture

```
papiflyfx-docking-github/
  api/
    GitHubToolbar.java            — Main toolbar HBox, FACTORY_ID = "github-toolbar"
    GitHubToolbarFactory.java     — ContentFactory for docking integration
    GitHubToolbarStateAdapter.java— ContentStateAdapter for session persistence
    GitHubToolbarContribution.java— Mounting helper for top/bottom placement
    GitHubRibbonActions.java      — Ribbon action bridge implemented by `GitHubToolbar`
    GitHubRepoContext.java        — Repository context (remote URI + optional local path)
  ribbon/
    GitHubRibbonProvider.java     — ServiceLoader ribbon contribution
  auth/
    CredentialStore.java          — Pluggable credential SPI
    PatCredentialStore.java       — In-memory PAT store
    SecretStoreCredentialAdapter.java — Adapter bridging settings SecretStore
    KeychainTokenStore.java       — OS keychain-backed store
    PreferencesTokenStore.java    — java.util.prefs-backed store
  git/
    GitRepository.java            — Local repository abstraction
    JGitRepository.java           — JGit implementation (branch, commit, push, status)
    GitOperationException.java    — Git operation failures
  github/
    GitHubApiService.java         — GitHub REST API client (PR creation, auth)
    GitHubApiException.java       — API error wrapper
    RemoteUrlParser.java          — Extracts owner/repo from remote URLs
  model/
    BranchRef, TagRef, CommitInfo, CurrentRefState, RepoStatus,
    PullRequestDraft, PullRequestResult, RefPopupEntry, RefPopupSection,
    StatusMessage, SecondaryChip, RollbackMode, GitRefKind
  settings/
    GitHubCategory.java           — SettingsCategory for host, author, PAT configuration
  ui/
    GitHubToolbarViewModel.java   — MVVM view model binding toolbar state
    CommandRunner.java            — Async git command executor
    dialog/                       — Commit, NewBranch, PullRequest, Rollback, Token dialogs
    popup/                        — GitRefPopup for branch/tag selection
    state/                        — RecentRefStore, PreferencesRecentRefStore
    theme/                        — GitHubThemeSupport, GitHubToolbarTheme, ThemeMapper
    toolbar/                      — RefPill branch display widget
```

## Docking integration

```java
ContentStateRegistry.register(new GitHubToolbarStateAdapter());
dockManager.setContentFactory(new GitHubToolbarFactory(repoContext));
```

The module registers via ServiceLoader:
- `META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter`
- `META-INF/services/org.metalib.papifly.fx.settings.api.SettingsCategory`
- `META-INF/services/org.metalib.papifly.fx.api.ribbon.RibbonProvider`

## Settings integration

`GitHubCategory` provides settings for:
- `github.host` — GitHub API host (default: `api.github.com`)
- `github.author.name` — Commit author name
- `github.author.email` — Commit author email
- `github.pat` — Personal access token (stored via `SecretStore`)

## Dependencies

- `papiflyfx-docking-api` — ContentFactory, ContentStateAdapter, Theme
- `papiflyfx-docking-settings-api` — SettingsCategory, SecretStore, SecretKeyNames
- `org.eclipse.jgit` — Local git operations
- JavaFX (controls, base, graphics)
- `papiflyfx-docking-docks` (test scope only)

## Run tests

```bash
./mvnw -pl papiflyfx-docking-github -am -Dtestfx.headless=true test
```
