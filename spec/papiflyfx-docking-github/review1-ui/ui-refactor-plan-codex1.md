# GitHub Toolbar UI Refactor Plan (Codex)

## 1. Goal

Implement the toolbar redesign described in `ui-refactor.md` inside the existing `papiflyfx-docking-github` module without rewriting the whole feature from scratch.

Primary outcomes:

1. Repository and current ref become the primary identity area.
2. The branch combo box is replaced by a dedicated ref pill and anchored popup.
3. Permanent badge noise is reduced to contextual chips only.
4. The action strip is simplified to `Update`, `Commit`, `Push`, `PR`, and overflow actions.
5. Status becomes transient and no longer competes with repository identity.

## 2. Reviewed Artifacts

- `spec/papiflyfx-docking-github/review1-ui/ui-refactor.md`
- `spec/papiflyfx-docking-github/review1-ui/idea-branch-popup.png`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModel.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/git/GitRepository.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/git/JGitRepository.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/RepoStatus.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/BranchRef.java`
- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarFxTest.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModelTest.java`

## 3. Current Baseline

### 3.1 What is already in good shape

- `GitHubToolbarViewModel` already owns command execution and enable/disable rules.
- Review 0 already introduced a GitHub-specific theme mapper and stylesheet, so this refactor does not need a new theming architecture.
- The module already supports local-vs-remote-only mode, dirty state, ahead/behind counts, detached HEAD, token state, and dialogs for commit / rollback / PR / token / new branch.

### 3.2 What blocks the review 1 design

1. `GitHubToolbar` is still a monolithic control built around `Hyperlink`, `ComboBox<String>`, grouped buttons, and many always-on badges.
2. `BranchRef` only models local/remote branches. It does not model tags, ref kind, tracking labels, or detached-commit display.
3. `RepoStatus` does not expose enough information for the new ref pill by itself.
4. There is no popup model, no search/filter model, and no recent-ref history.
5. There is no `Update` command in the current Git layer.
6. `statusText` and `errorText` are string slots, not a structured transient status model.
7. Background API lookup failures can currently occupy the error slot even when the toolbar is otherwise usable.

## 4. Assumptions Used In This Plan

1. `Update` should be safe in v1.
   Recommended behavior: fetch from `origin`, refresh state, and do not auto-merge.
2. Existing dialogs remain in use in the first pass.
   `Commit`, `Rollback`, `New Branch`, `Token`, and `Pull Request` do not need new dialog UX for this review.
3. Recent refs should persist per repository, max 5 entries, using a lightweight preferences-backed store.
4. Recommended submenu actions from the review note such as `Rename`, `Delete Local Branch`, and `Show Diff with Working Tree` are follow-up work unless backend support is added explicitly.

## 5. Scope

### 5.1 In scope

1. Repository pill replacing the thin hyperlink surface.
2. Ref pill replacing the combo box and checkout button.
3. Anchored popup switcher with:
   - search field
   - action block
   - `Recent`, `Local`, `Remote`, and `Tags` sections
4. Contextual secondary chips:
   - `Default`
   - `Detached`
   - `Remote only`
   - `Ahead N`
   - `Behind N`
   - `No token`
5. Simplified action area:
   - `Update`
   - `Commit`
   - `Push`
   - `PR`
   - `...`
6. Far-right transient status slot with spinner, short success text, and inline error pill.
7. Model and repository changes needed to support ref type, tags, tracking labels, recent refs, and popup state.
8. Updated automated tests and visual snapshots.

### 5.2 Out of scope for this refactor

1. Full staging UI.
2. Branch rename/delete/diff flows if they require new Git commands beyond the popup shell.
3. Authentication UX redesign.
4. Multi-repo coordination.
5. Cloning a repository or handling remote-only to local transition.

## 6. Proposed Technical Approach

### 6.1 Keep public entry points stable

These APIs should stay stable unless implementation pressure proves otherwise:

- `GitHubToolbar`
- `GitHubToolbarContribution`
- `GitHubToolbarFactory`
- `GitHubRepoContext`
- toolbar state capture / restore

The refactor should be internal to the module, not a public API redesign.

### 6.2 Refactor `GitHubToolbar` into a root composer

`GitHubToolbar` should stop owning every visual detail directly.

Recommended internal composition:

```text
GitHubToolbar
  -> RepositoryPill
  -> RefPill
  -> SecondaryChipStrip
  -> ActionBar
  -> StatusSlot
  -> RefPopup (PopupControl anchored to RefPill)
```

Implementation note:

- These helper controls can be package-private classes under `ui/toolbar` and `ui/popup`.
- `GitHubToolbar` should remain responsible for host concerns such as opening the browser and showing dialogs.
- Popup items should delegate to view-model commands or toolbar callbacks, not call JGit directly.
- Existing persistence and theme hooks should remain intact:
  - `bindThemeProperty()`
  - `captureState()`
  - `fromState()`

### 6.3 Define the `RefPill` interaction contract explicitly

The ref pill needs to behave like a switcher, not like a badge.

Recommended structure:

```text
[ status dot ] [ ref-type icon ] [ ref name ] [ chevron ]
```

Recommended implementation details:

1. Use a small dedicated node for the status dot rather than text glyphs.
   A `Circle` is the simplest choice.
2. Ref-type icon should vary by current ref kind:
   - branch
   - tag
   - detached commit
3. The whole pill is one focusable target.
4. Hover and focus states should make it feel clickable but still like IDE chrome.

Keyboard contract:

1. `Enter` opens the popup.
2. `Space` opens the popup.
3. `Alt+Down` opens the popup.
4. `Esc` closes the popup when it is open.

### 6.4 Introduce richer UI state instead of adding more badge booleans

The current view model exposes many primitive properties that match the old badge-heavy layout.
The new toolbar should bind to richer state objects.

Recommended additions:

- `GitRefKind`
  - `LOCAL_BRANCH`
  - `REMOTE_BRANCH`
  - `TAG`
  - `DETACHED_COMMIT`
- `CurrentRefState`
  - display name
  - ref kind
  - optional tracking label
  - status-dot state
  - default-branch flag
  - detached flag
  - remote-only flag
- `SecondaryChip`
  - chip text
  - chip variant
- `StatusMessage`
  - kind: `IDLE`, `BUSY`, `SUCCESS`, `ERROR`
  - text
- `RefPopupSection`
  - title
  - entries
- `RefPopupEntry`
  - action item, ref item, or section separator metadata

Recommendation:

- Do not stretch `BranchRef` until it means everything.
- Keep `BranchRef` for existing branch-only dialogs if needed.
- Add a richer ref model specifically for the new toolbar and popup.

Practical compatibility note:

- If the implementation needs an intermediate step, adding `trackingTarget` to `BranchRef` and a lightweight `TagRef` record is an acceptable bridge before a fuller snapshot model lands.

### 6.5 Consolidate Git refresh data into a richer snapshot

The current view model refresh path calls `loadStatus()`, `listBranches()`, and `getHeadCommit()` separately.
The ref popup will need even more data.

Preferred direction:

- add a repository snapshot contract that returns, in one refresh operation:
  - current ref information
  - dirty / ahead / behind state
  - head commit
  - local branches
  - remote branches
  - tags
  - tracking labels where available

Minimum acceptable fallback if a snapshot feels too large right now:

- `listTags()`
- `loadCurrentRef()`
- `fetchOrigin()`
- `checkoutTag(...)` or `checkoutRef(...)`

Why a snapshot is better:

1. It avoids repeated JGit walks during one UI refresh.
2. It gives the popup and pills a coherent view of repository state.
3. It prevents the view model from stitching together too many partial DTOs.

### 6.6 Extend `JGitRepository` only where the new UI truly needs it

Needed backend additions for review 1:

1. Tag listing.
2. Current ref kind resolution:
   - local branch
   - remote-tracking branch when relevant
   - tag when HEAD matches a tag
   - detached commit short hash otherwise
3. Tracking label resolution for branch rows such as `origin/main`.
4. Safe `Update` command.
   Recommended v1 behavior: `fetch --prune origin` plus refresh.
5. Ref checkout that can handle branch and tag selection.

Recommended deferrals:

- branch rename
- branch delete
- diff actions
- raw revision browsing beyond a simple checkout dialog

### 6.7 Add a dedicated recent-ref store

Recent refs should not be inferred from the current branch list order.

Recommended structure:

- `RecentRefStore` interface
- `PreferencesRecentRefStore` implementation keyed by repository remote URL

Write rules:

1. Record only successful checkout targets.
2. De-duplicate by full ref name.
3. Keep newest first.
4. Keep max 5.

Alternative considered:

- Reading recent refs from reflog is possible, but it should be treated as an optional fallback because reflog availability and retention are not a stable UI contract.

### 6.8 Replace permanent text slots with structured transient status

The status slot should be quiet by default.

Behavior rules:

1. `BUSY` shows spinner plus short text.
2. `SUCCESS` shows short text temporarily, then clears.
3. `ERROR` shows inline error pill and stays until the next meaningful command or refresh.
4. `IDLE` hides the slot.

Important behavior correction:

- background default-branch lookup failures should not permanently render as a blocking error pill if the toolbar still has enough local state to work.

Migration note:

- The first implementation can reuse the existing busy indicator, status label, and error label nodes behind a new `StatusMessage` binding before tightening the visual structure further.

### 6.9 Keep the review 0 theme infrastructure and expand the stylesheet

The design direction in `ui-refactor.md` already fits the existing theme layer.

Use the current:

- `GitHubToolbarTheme`
- `GitHubToolbarThemeMapper`
- `GitHubThemeSupport`
- `github-toolbar.css`

Needed styling work:

1. Replace the boxed-islands layout with one continuous toolbar surface.
2. Give repo/ref pills stronger surfaces than the action buttons.
3. Add style classes for:
   - repo pill
   - ref pill
   - ref status dot
   - secondary chips
   - action bar
   - overflow button
   - status slot
   - popup
   - popup search field
   - popup section headers
   - popup row cells
   - popup submenu chevrons
4. Preserve the same geometry in dark and light themes.
5. Add a flatter action-button treatment so the main action area no longer reads like a bordered form group.

Recommended CSS direction:

- keep pill surfaces visually stronger
- keep action buttons flatter and quieter
- keep popup styling on the same theme variables already emitted by `GitHubThemeSupport`

### 6.10 Define popup row behavior and submenu boundaries

The popup should support three row categories:

1. Command rows such as `Update project...` and `Commit...`
2. Direct ref rows such as a local branch checkout target
3. Rows with trailing chevrons that open a side submenu

Recommended behavior:

1. Clicking a local branch performs checkout directly.
2. Clicking a remote branch may either:
   - perform the simplest supported checkout flow, or
   - open a submenu if tracking decisions are required
3. Clicking a tag checks out detached HEAD at that tag in the first implementation.
4. The top search field filters command rows and ref rows together.

Submenu boundary for this review:

- It is acceptable to implement only the submenu shell and supported actions first, while leaving rename/delete/diff actions disabled or deferred until backend support exists.

## 7. Phased Implementation Plan

### Phase 1. Data and command groundwork

1. Add the richer ref-state model and popup DTOs.
2. Add recent-ref persistence abstraction.
3. Extend `GitRepository` / `JGitRepository` with the data needed for tags, current ref type, tracking labels, and safe update.
4. Refactor `GitHubToolbarViewModel.refresh()` to build a coherent toolbar snapshot.
5. Add tests for:
   - tag listing
   - detached tag vs detached commit resolution
   - recent-ref ordering
   - update command behavior

Goal of this phase:

- the old toolbar can still exist temporarily, but the new data model is ready.

### Phase 2. Toolbar layout refactor

1. Replace repo hyperlink surface with a full clickable repository pill.
2. Replace combo box + checkout button with a ref pill stub.
3. Introduce a compact chip strip for optional state only.
4. Introduce new action bar layout and overflow button.
5. Introduce status slot component and structured status binding.

Goal of this phase:

- the toolbar reads like the new design even before the full popup is complete.

### Phase 3. Ref popup implementation

1. Add `PopupControl` anchored below the ref pill.
2. Add search field with immediate focus on popup open.
3. Add section rendering for `Recent`, `Local`, `Remote`, and `Tags`.
4. Add mouse and keyboard behavior:
   - click ref pill
   - `Enter`
   - `Space`
   - `Alt+Down`
   - `Esc` to close
5. Add row-key navigation inside the popup:
   - `Up`
   - `Down`
   - `Enter`
6. Add side-submenu support for rows that need more than one action.
7. Wire branch and tag selection through the existing dirty-checkout safeguards.
8. Record successful selections into `RecentRefStore`.

Goal of this phase:

- switching refs feels like the primary interaction surface, not a form field.

### Phase 4. Action migration and cleanup

1. Add visible `Update` action.
2. Keep `Commit`, `Push`, and `PR` in the main action bar.
3. Move lower-frequency actions such as `Rollback` and `Token` into `...`.
4. Move `New Branch...` into the popup top action block.
5. If time allows, add `Checkout Tag or Revision...` as a lightweight dialog launched from the popup.
6. Remove obsolete controls and logic:
   - branch combo box
   - checkout button
   - always-on auth badge
   - always-on mode badge

Goal of this phase:

- the old interaction model is fully removed.

### Phase 5. Testing, snapshots, and docs

1. Rewrite FX tests around the new contract instead of preserving assertions for the old grouped layout.
2. Add popup behavior tests:
   - open / close
   - keyboard open
   - arrow-key navigation
   - search filtering
   - remote-only rendering
   - default branch chip
   - detached tag rendering
   - submenu rows when supported
3. Keep view-model tests for command enablement and status transitions.
4. Add focused tests for the ref pill:
   - status-dot state
   - ref-type icon selection
   - keyboard activation
5. Keep theme-integration coverage for the new pill/popup classes.
6. Export new review snapshots to `spec/papiflyfx-docking-github/review1-ui/`.
7. Update module README only if the visible action set or behavior contract changes materially.

## 8. Recommended File Touch List

### Existing files likely to change

- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModel.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/git/GitRepository.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/git/JGitRepository.java`
- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarFxTest.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModelTest.java`

### Recommended new files

- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/GitRefKind.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/CurrentRefState.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/TagRef.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/SecondaryChip.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/StatusMessage.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/RefPopupSection.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/model/RefPopupEntry.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/toolbar/RefPill.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/popup/GitRefPopup.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/popup/GitRefPopupController.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/state/RecentRefStore.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/state/PreferencesRecentRefStore.java`

## 9. Validation Plan

### 9.1 Automated

Run:

```bash
mvn -pl papiflyfx-docking-github -am -Dtestfx.headless=true test
```

Add or update tests for these scenarios:

1. Clean local branch.
2. Dirty working tree.
3. Default branch with commit disabled.
4. Detached on tag.
5. Detached on commit hash.
6. Remote-only repository.
7. Token absent vs token present.
8. Popup search filtering.
9. Successful checkout adds to recent refs.
10. Background API failure does not permanently occupy the error slot.
11. Ref pill keyboard activation opens the popup.
12. Ref pill status-dot state matches clean / dirty / remote-only mode.

### 9.2 Visual review

Export snapshots for:

1. local clean dark
2. local clean light
3. dirty branch
4. default branch
5. detached tag
6. remote-only
7. popup open

Store them under:

- `spec/papiflyfx-docking-github/review1-ui/`

### 9.3 Manual interaction review

Verify:

1. Clicking the repo pill opens the browser.
2. Clicking the ref pill opens the popup at the correct anchor.
3. `Enter`, `Space`, and `Alt+Down` also open the popup.
4. `Esc` closes the popup.
5. Dirty branch checkout still prompts for confirmation.
6. Remote-only mode shows neutral status dot and hides local-only actions.

## 10. Risk Notes

1. JavaFX popup focus behavior can be fragile, especially for keyboard routing and initial search-field focus.
2. Recent-ref derivation from reflog should not become a hard dependency because reflog retention is environment-dependent.
3. Tests currently tied to `#github-branch-combo` and old grouped layout IDs must be rewritten before obsolete nodes are removed, or coverage will silently drop.
4. New popup and pill CSS should stay on top of the existing `GitHubThemeSupport.themeVariables()` contract so dark/light support does not regress.

## 11. Exit Criteria

This refactor is complete when all of the following are true:

1. No branch combo box remains in the toolbar.
2. Repo and ref pills are the first readable elements in the toolbar.
3. The popup is the main ref-switching surface and supports search.
4. Chips only appear when they communicate meaningful current state.
5. The status slot is hidden when idle.
6. Remote-only, default-branch, dirty, detached-tag, and detached-commit states all render distinctly.
7. The module test suite passes in headless mode.
