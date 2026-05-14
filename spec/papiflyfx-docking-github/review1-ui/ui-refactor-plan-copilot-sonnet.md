# GitHub Toolbar UI Refactor — Implementation Plan

> Powered by Claude Sonnet 4.6  
> Source spec: `spec/papiflyfx-docking-github/review1-ui/ui-refactor.md`

---

## Problem Statement

The current `GitHubToolbar` is an `HBox` that renders four bordered `HBox` groups
(`repoGroup`, `branchGroup`, `changesGroup`, `remoteGroup`) of equal visual weight.
Repository identity is not the focal point: the repo link is a `Hyperlink` inside a
plain surface wrapper, branch selection is a `ComboBox<String>`, and state is spread
across eight always-rendered badge `Label`s that create visual noise.

The refactor replaces this with a Git-identity-first toolbar:

1. A clickable **repository pill** (`owner/repo`).
2. A clickable **ref pill** (status dot + icon + ref name + chevron) that opens a
   branch/tag popup.
3. **Contextual secondary chips** shown only when relevant.
4. A flat right-aligned **action area** (Update · Commit · Push · PR · `…`).
5. A transient **status slot** (spinner / success text / error pill) at the far right.

---

## Current State Summary

| Layer | Class | Current implementation |
|---|---|---|
| UI root | `GitHubToolbar extends HBox` | Four bordered `HBox` groups, eight badge labels, `ComboBox` for branch |
| View model | `GitHubToolbarViewModel` | Rich JavaFX property model; no ref-type or tag data |
| Model | `BranchRef` | `name`, `fullName`, `local`, `remote`, `current` — no ref type or tracking target |
| Model | `RepoStatus` | No ref type field |
| Git interface | `GitRepository` | No tag listing, no recent-ref history |
| Theme | `GitHubToolbarThemeMapper` | Complete; reusable as-is |
| CSS | `github-toolbar.css` | Badge, button, combo, group rules — needs pill / popup additions |

---

## Planned Changes

### Step 1 — Model: add ref type and tag support

**Files:** `BranchRef.java`, `RepoStatus.java`, new `RefType.java`, new `TagRef.java`,
`GitRepository.java`, `JGitRepository.java`

- Add `enum RefType { BRANCH, TAG, DETACHED_COMMIT }` to `org.metalib.papifly.fx.github.model`.
- Add `RefType refType()` to `RepoStatus` (default `BRANCH` if not detached, `DETACHED_COMMIT`
  when `detachedHead` is true).
- Add record `TagRef(String name, String fullName, String objectHash)` to
  `org.metalib.papifly.fx.github.model`.
- Add `String trackingTarget()` field to `BranchRef` (the remote-tracking ref name,
  e.g. `origin/main`; nullable).
- Extend `GitRepository` with:
  - `List<TagRef> listTags()`
  - `List<String> recentRefs(int max)`  — recent checked-out ref names from reflog
- Implement both methods in `JGitRepository`.

**Impact on view model:**
- Add `ListProperty<TagRef> tags` and `ListProperty<String> recentRefs` to
  `GitHubToolbarViewModel`.
- Populate them in `refresh()`.
- Add `ReadOnlyObjectProperty<RefType> refType()` derived from `RepoStatus`.

---

### Step 2 — New UI component: `RefPill`

**New file:** `org.metalib.papifly.fx.github.ui.RefPill`

A single `HBox` styled as a pill that replaces the `ComboBox` and the
`currentBranchBadge`/`detachedBadge` labels.

Layout (left to right):
```
[ ● ]  [ branch-icon ]  [ ref-name ]  [ ▾ ]
```

- The status dot is a small `Circle` (radius ≈ 4 px).
  - Green: `localAvailable && !dirty`
  - Red: `localAvailable && dirty`
  - Gray: `!localAvailable` (remote-only)
- The icon `Label` shows a branch glyph, tag glyph, or detached glyph depending on
  `RefType`.
- The ref-name `Label` is bound to `viewModel.currentBranchProperty()`.
- The trailing chevron is a small `Label("▾")` or `Text`.
- The entire pill is keyboard-focusable; `Enter`, `Space`, or `Alt+Down` opens the
  popup (same as mouse click).
- Hover gives the pill a raised look (use `pf-github-ref-pill:hover` CSS rule).

CSS class: `pf-github-ref-pill`

---

### Step 3 — New UI component: `RefPopup`

**New file:** `org.metalib.papifly.fx.github.ui.RefPopup`

A `PopupControl` (or `Popup` wrapping a styled `VBox`) anchored below the ref pill.

Dimensions: width 360–420 px, max height 520 px, scrollable.

Internal structure (top to bottom):

```
[ 🔍 Search branches, tags, and actions…  ]
────────────────────────────────────────────
  Update project…
  Commit…
  Push…
────────────────────────────────────────────
  New Branch…
  Checkout Tag or Revision…
────────────────────────────────────────────
Recent                              (section header)
  github                           origin/github
  main                               origin/main
────────────────────────────────────────────
Local                               (section header)
  github                           origin/github
  main                               origin/main
  feature/search                  origin/search ›
────────────────────────────────────────────
Remote                              (section header)
  ▸ origin
────────────────────────────────────────────
Tags                                (section header)
  v0.9.0                                        ›
  v0.8.0
```

Key behaviours:

- Search `TextField` at the top filters all sections simultaneously.
- Focus moves to the search field immediately on open.
- Clicking a **local branch** calls `viewModel.switchBranch(name, false)` directly.
- Items with a trailing `›` chevron open a side submenu `ContextMenu` with:
  - Branch: Checkout · Checkout and Track · Show Diff · Rename… · Delete Local Branch…
  - Tag: Checkout Tag · Open Commit · Compare with Working Tree
- Clicking an **action item** (Update, Commit…) fires the corresponding toolbar action.
- Keyboard navigation: `↑`/`↓` moves selection, `Enter` activates, `Escape` closes.

**Sub-components:**
- `RefPopupSection` — section header + item list (`VBox`)
- `RefPopupItem` — single row with name label, tracking label, optional chevron

CSS classes: `pf-github-ref-popup`, `pf-github-ref-popup-section-header`,
`pf-github-ref-popup-item`, `pf-github-ref-popup-item:hover`,
`pf-github-ref-popup-item:selected`

---

### Step 4 — Refactor `GitHubToolbar`

**File:** `GitHubToolbar.java`

Replace the current layout with the new single-row identity-first design.

**Remove:**
- `branchCombo` (`ComboBox<String>`)
- `checkoutButton` (checkout moves into the ref popup)
- `newBranchButton` (moves into the ref popup)
- `currentBranchBadge` (replaced by ref pill)
- `detachedBadge` (absorbed into ref pill icon)
- `modeBadge` ("Local clone" / "Remote only") — kept as a chip but only shown when
  relevant (remote-only mode)
- `branchGroup` and `changesGroup` as separate bordered groups
- `rollbackButton` — moved to the `…` overflow menu

**Add:**
- `RefPill refPill` — instantiated with `viewModel`
- `MoreActionsButton moreButton` (the `…` button) containing: Rollback, Token, and
  future repo actions
- `Button updateButton` — visible always; calls `viewModel.refresh()`

**New toolbar layout:**
```
[ owner/repo pill ]  [ ref pill ]  [ Ahead N ]  [ Behind N ]  spacer
                                    [Update] [Commit] [Push] [PR] [...]
```

Chip visibility rules (keep existing badge infrastructure, adjust `refreshBadges()`):

| Chip | Show when |
|---|---|
| `Ahead N` | `aheadCount > 0` |
| `Behind N` | `behindCount > 0` |
| `Default` | `defaultBranchActive` |
| `Detached` | `detachedHead` (as a chip, ref pill icon already signals it visually) |
| `Remote only` | `!localAvailable` |
| `N changed` | `dirty && dirtyCount > 0` (replaces the old `Dirty N` badge) |
| `No token` | `!authenticated` |

**Keep as-is:**
- `commitButton`, `pushButton`, `prButton` with their existing bindings
- `busyIndicator`, `statusLabel`, `errorLabel` in the status slot
- `applyTheme()`, `bindThemeProperty()`, `captureState()`, `fromState()` — no changes
- All dialog interactions (`onCommit()`, `onRollback()`, etc.)

**Action area CSS:**  
Remove the `pf-github-group` bordered box from action buttons. Style them flat with
`pf-github-action-button` (no border, just text + hover background).

---

### Step 5 — CSS updates (`github-toolbar.css`)

Add new rules:

```css
/* Ref pill */
.pf-github-ref-pill { ... }
.pf-github-ref-pill:hover { ... }
.pf-github-ref-pill:focused { ... }
.pf-github-ref-pill .pf-github-status-dot { ... }

/* Action buttons (flat, no border box) */
.pf-github-action-button { ... }
.pf-github-action-button:hover { ... }
.pf-github-action-button:disabled { ... }

/* Ref popup */
.pf-github-ref-popup { ... }
.pf-github-ref-popup-section-header { ... }
.pf-github-ref-popup-item { ... }
.pf-github-ref-popup-item:hover { ... }
.pf-github-ref-popup-item:selected { ... }
.pf-github-ref-popup-separator { ... }
```

The `pf-github-combo` rules remain for now but are no longer applied to the toolbar.

---

### Step 6 — Test updates

**Files to update:**
- `GitHubToolbarFxTest` — replace ComboBox assertions with RefPill lookup; add popup
  open/close tests; verify status dot color for clean/dirty/remote-only states.
- `GitHubToolbarViewModelTest` — add tests for `tags` and `recentRefs` properties.
- `GitHubToolbarThemeIntegrationTest` — verify ref pill receives theme colours.

**New test files:**
- `RefPillTest` — unit tests for status dot state, ref type icon selection, keyboard
  activation.
- `RefPopupTest` (FxTest) — open popup, filter by search text, select a branch,
  verify `switchBranch` is called.

---

## Scope Boundaries

**In scope:**
- `papiflyfx-docking-github` module only.
- All changes listed above.

**Out of scope (future work):**
- Remote branch checkout with tracking (side submenu skeleton is sufficient).
- Rename / delete branch dialogs — existing `NewBranchDialog` and dialogs stay intact.
- Diff viewer integration from popup items.
- Cloning a repo (remote-only → local transition).

---

## Dependency Flow

```
Step 1 (model)
  └─► Step 2 (RefPill — consumes refType from view model)
        └─► Step 3 (RefPopup — consumes tags, recentRefs, branches from view model)
              └─► Step 4 (GitHubToolbar — assembles RefPill + RefPopup)
                    └─► Step 5 (CSS — styles new components)
                          └─► Step 6 (tests — validate everything)
```

Steps 2 and 3 can be developed in parallel once Step 1 is complete.

---

## Risk Notes

- **JGit reflog API** — `recentRefs()` depends on JGit's `ReflogReader`; may need a
  fallback returning an empty list if the reflog is unavailable or pruned.
- **PopupControl focus** — JavaFX popup focus management can be tricky on macOS; test
  with `testfx.headless=true` and also interactively.
- **ComboBox removal** — `GitHubToolbarFxTest` currently queries `#github-branch-combo`;
  update test IDs before removing the node to avoid silent test gaps.
- **Theme variables** — new CSS rules must use existing `--pf-github-*` CSS variables
  from `GitHubThemeSupport.themeVariables()` so dark/light themes work automatically.
