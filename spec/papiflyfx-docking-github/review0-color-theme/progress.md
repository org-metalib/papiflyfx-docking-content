# GitHub Toolbar Color Theme Harmonization Progress

Date: 2026-03-10
Scope: `spec/papiflyfx-docking-github/review0-color-theme/plan-codex.md`

## Overall status

- Phase A: `completed`
- Phase B: `completed`
- Phase C: `completed`
- Phase D: `completed`
- Phase E: `completed`
- Phase F: `completed`
- Phase G: `completed`

## Completed work

### Phase A and Phase B

- Added `GitHubToolbarTheme`, `GitHubToolbarThemeMapper`, and `GitHubThemeSupport`.
- Mapped docking `Theme` colors into a GitHub-specific palette with brightness-based dark and light detection.
- Moved toolbar and dialog theming to stylesheet-driven rendering with `github-toolbar.css` and `github-dialog.css`.
- Replaced opaque color conversion with alpha-safe `rgba(...)` output for theme variables.
- Added stable ids and style classes for toolbar groups, badges, buttons, status text, dialog pane, and dialog buttons.

### Phase C and Phase D

- Rebuilt `GitHubToolbar` around grouped repository, branch, changes, remote, and status surfaces.
- Removed literal separators and replaced them with contained surfaces and semantic chips.
- Added badges for current branch, dirty state, local-vs-remote mode, default branch, detached head, ahead, behind, and token state.
- Changed remote-only mode to collapse branch and change groups and hide push instead of rendering disabled local controls.
- Extended `GitHubToolbarViewModel` with richer read-only state:
  - `remoteOnlyProperty`
  - `detachedHeadProperty`
  - `defaultBranchActiveProperty`
  - `aheadCountProperty`
  - `behindCountProperty`
  - `dirtyCountProperty`
- Preserved command enablement behavior while computing compact dirty-count state for the new badges.

### Phase E

- Added shared dialog styling through `GitHubDialogStyler`.
- Updated `CommitDialog`, `NewBranchDialog`, `RollbackDialog`, `PullRequestDialog`, `TokenDialog`, and `DirtyCheckoutAlert` to receive the active theme.
- Styled primary, secondary, and destructive dialog actions after button nodes become available.
- Kept focus, hover, pressed, and disabled states aligned with the toolbar palette.

### Phase F

- Updated `GitHubToolbarFxTest` to assert grouped structure and semantic badge behavior.
- Added `GitHubToolbarThemeIntegrationTest` for theme binding, switching, unbinding, and dialog theme propagation.
- Added `GitHubToolbarThemeMapperTest`.
- Expanded `GitHubToolbarViewModelTest` to cover remote-only, ahead/behind, detached-head, default-branch, and dirty-count state.
- Added a gated review snapshot export path in `GitHubToolbarFxTest`.
- Removed JavaFX CSS size-cast warnings by moving fragile size styling away from looked-up CSS size variables.

### Phase G

- Updated `GitHubToolbarSample` to showcase both a local-clone workflow and a remote-only workflow.
- Compared the resulting toolbar treatment against:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/GoToLineController.java`
  - `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewToolbar.java`
- Captured review screenshots:
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-local-dark.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-local-light.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-remote-dark.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-remote-light.png`

## Validation

- Baseline before implementation:
  - `mvn -pl papiflyfx-docking-github -am test -Dtestfx.headless=true`
  - Result: success
- Post-implementation GitHub module suite:
  - `mvn -pl papiflyfx-docking-github -am test -Dtestfx.headless=true`
  - Result: success
  - Tests run: `32`, failures: `0`, errors: `0`
- Samples smoke validation:
  - `mvn -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: success
  - Tests run: `3`, failures: `0`, errors: `0`
- Review snapshot export:
  - `mvn -pl papiflyfx-docking-github -am -Dtest=GitHubToolbarFxTest#exportReviewSnapshotsWhenRequested -Dtestfx.headless=true -Dpapiflyfx.review.snapshots=true -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: success
  - Tests run: `1`, failures: `0`, errors: `0`
