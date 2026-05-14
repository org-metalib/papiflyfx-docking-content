# GitHub Toolbar UI Refactor — Implementation Plan

> Powered by Claude Opus 4.6
> Source spec: `spec/papiflyfx-docking-github/review1-ui/ui-refactor.md`
> Reference screenshot: `spec/papiflyfx-docking-github/review1-ui/idea-branch-popup.png`

---

## 1. Current State Assessment

### 1.1 What already works well

`GitHubToolbarViewModel` (454 lines) is a clean separation of concerns: it owns all
JavaFX properties, command execution via `CommandRunner`, and enable/disable bindings.
The review 0 theme pipeline (`GitHubToolbarThemeMapper` → `GitHubToolbarTheme` record
→ `GitHubThemeSupport.themeVariables()` → CSS custom properties) is mature and
reusable. Six dialogs already handle commit, rollback, new branch, PR, token, and
dirty-checkout confirmation. State persistence via `captureState()` / `fromState()`
is operational.

### 1.2 What must change

| Current artifact | Problem for the new design |
|---|---|
| `GitHubToolbar.java` (559 lines) | Monolithic HBox with five bordered groups, eight always-rendered badge labels, a `ComboBox<String>` for branch switching, and scattered action buttons that all compete for attention. |
| `BranchRef` record (31 lines) | Models `name`, `fullName`, `local`, `remote`, `current` only. No tracking-target label (e.g. `origin/main`), no ref-type distinction. |
| `RepoStatus` record (40 lines) | Reports `detachedHead` as a boolean but provides no information about whether the detached state is on a tag or a bare commit. No tag data at all. |
| `GitRepository` interface (35 lines) | Has `listBranches()` and `loadStatus()` but no `listTags()`, no `fetchOrigin()`, no ref-type resolution, and no reflog/recent-ref access. |
| `GitHubToolbarTheme` record (39 lines) | Has `success`, `warning`, `danger` but no dedicated dot, pill, or chip color tokens. |
| `refreshBadges()` (lines 373–414 in `GitHubToolbar`) | Renders all eight badges unconditionally, toggling visibility but keeping them in the scene graph. The mode badge ("Local clone" / "Remote only") and auth badge ("Token" / "No token") are always visible. |

### 1.3 Public API surface that must remain stable

- `GitHubToolbar` class name, `FACTORY_ID`, constructors accepting `GitHubRepoContext`, `CredentialStore`, `Theme`
- `GitHubToolbar.bindThemeProperty(ObjectProperty<Theme>)`, `captureState()`, `fromState(Map)`
- `GitHubToolbarContribution`, `GitHubToolbarFactory`, `GitHubToolbarStateAdapter`
- `GitHubRepoContext` record
- `GitHubToolbarViewModel` constructor and all existing `ReadOnly*Property` accessors

Internal composition changes freely. No downstream consumer should break.

---

## 2. Scope

### In scope

1. Repository pill replacing the `Hyperlink` + `repoLinkSurface` wrapper.
2. Ref pill replacing `branchCombo`, `checkoutButton`, `currentBranchBadge`, `dirtyBadge`, and `detachedBadge`.
3. PopupControl-based branch/tag switcher with search, action block, and four collapsible sections (Recent, Local, Remote, Tags).
4. Contextual chip strip (chips appear only when they communicate actionable state).
5. Simplified action area: `Update`, `Commit`, `Push`, `PR`, `…` (overflow).
6. Far-right transient status slot.
7. Model extensions: ref type, tag listing, tracking targets, recent-ref history.
8. Git layer extensions: `listTags()`, `fetchOrigin()`, `checkoutRef()`.
9. Theme token additions for dot, pill, and chip styling.
10. Test rewrites and new visual snapshots.

### Out of scope

- Branch rename / delete / diff commands (popup shows the menu items as disabled stubs).
- Full staging UI or file-level diff viewer.
- Authentication UX redesign.
- Multi-repo coordination.
- Cloning (remote-only to local transition).

---

## 3. Design Decisions

### 3.1 No new ViewModel — extend the existing one

The Codex plan suggests introducing `CurrentRefState`, `SecondaryChip`, `StatusMessage`,
and other intermediate state objects. This plan takes a lighter approach: the existing
`GitHubToolbarViewModel` already exposes the right granularity of properties. The new
pills and chips can bind directly to the existing `dirtyProperty()`, `detachedHeadProperty()`,
`aheadCountProperty()`, etc. Only three genuinely new properties are required:
`refTypeProperty`, `tagsProperty`, and `recentRefsProperty`.

Rationale: introducing aggregate state records adds an indirection layer that makes the
binding story more complex without reducing the amount of code. The existing flat
properties are well-named and independently observable, which is exactly what pills and
chips need.

### 3.2 Toolbar components are package-private inner helpers, not separate public classes

The IntelliJ reference popup is a single cohesive UI. Splitting it across many small public
classes increases the API surface and makes it harder to refactor later. This plan uses:

- `RepositoryPill` — package-private class in `ui.toolbar`
- `RefPill` — package-private class in `ui.toolbar`
- `StatusDot` — package-private class in `ui.toolbar`
- `ChipStrip` — package-private class in `ui.toolbar`
- `ActionStrip` — package-private class in `ui.toolbar`
- `StatusSlot` — package-private class in `ui.toolbar`
- `RefPopup` — package-private class in `ui.popup`
- `RefPopupContent` — package-private class in `ui.popup`

`GitHubToolbar` remains the single public entry point and composes these internally.

### 3.3 Recent-ref history lives in the ViewModel, not a separate persistence layer

The max-5 recent-ref list is small enough to serialize inside the existing
`captureState()` / `fromState()` Map. A dedicated `PreferencesRecentRefStore` is
unnecessary complexity for five strings. If the persistence requirement grows later,
it can be extracted.

### 3.4 `Update` command = `fetch --prune origin` + `refresh()`

This is the safest v1 behavior. No auto-merge, no auto-rebase. The user sees updated
remote tracking branches and ahead/behind counts after the fetch.

### 3.5 Popup uses a custom `Popup` wrapping a styled `VBox`, not `PopupControl`

`PopupControl` requires implementing a `Skin`, which is heavyweight for a simple anchored
panel. A plain `Popup` with a styled `VBox` content root is simpler, provides the same
anchoring via `show(Node, double, double)`, and avoids the Skin boilerplate. Focus
management uses `popup.setAutoFix(true)` and `popup.setAutoHide(true)`, with
`Platform.runLater(() -> searchField.requestFocus())` on show.

---

## 4. Phased Implementation

### Phase 1 — Model and Git layer extensions

**Goal:** provide all data the new UI needs without changing any existing UI code.

#### 1a. New file: `model/RefType.java`

```java
package org.metalib.papifly.fx.github.model;

public enum RefType {
    BRANCH,
    TAG,
    DETACHED_COMMIT
}
```

#### 1b. New file: `model/TagRef.java`

```java
package org.metalib.papifly.fx.github.model;

public record TagRef(
    String name,
    String fullName,
    String objectHash
) implements Comparable<TagRef> {
    @Override
    public int compareTo(TagRef other) {
        return name.compareToIgnoreCase(other.name);
    }
}
```

#### 1c. Extend `BranchRef` record

Add an optional tracking-target field. To preserve binary compatibility with existing
callers that construct `BranchRef` with five arguments, add a compact constructor:

```java
// New field
public record BranchRef(
    String name,
    String fullName,
    boolean local,
    boolean remote,
    boolean current,
    String trackingTarget   // nullable, e.g. "origin/main"
) implements Comparable<BranchRef> {
    // Backwards-compatible constructor for existing callers
    public BranchRef(String name, String fullName, boolean local, boolean remote, boolean current) {
        this(name, fullName, local, remote, current, null);
    }
    // existing compareTo unchanged
}
```

**Impact:** `FakeGitRepository` in both test files uses the five-arg constructor — unchanged.
`JGitRepository.listBranches()` updated to resolve tracking config and pass the sixth argument.

#### 1d. Extend `RepoStatus` record

Add `RefType refType` as a new component. Update the canonical constructor to derive it
from `detachedHead` (the caller in `JGitRepository` can compute it more precisely):

```java
public record RepoStatus(
    String currentBranch,
    String defaultBranch,
    boolean detachedHead,
    RefType refType,          // NEW
    int aheadCount,
    int behindCount,
    Set<String> added,
    Set<String> changed,
    Set<String> removed,
    Set<String> missing,
    Set<String> modified,
    Set<String> untracked
) { ... }
```

**Backwards-compatibility:** no external callers use a positional constructor for
`RepoStatus` outside tests and `JGitRepository`. All sites are inside the module and
can be updated in one pass.

#### 1e. Extend `GitRepository` interface

```java
List<TagRef> listTags();

void fetchOrigin();

void checkoutRef(String refName, boolean force);
```

- `listTags()` returns annotated and lightweight tags sorted by name.
- `fetchOrigin()` runs `fetch --prune origin`.
- `checkoutRef()` handles both branch names and tag names (detaching HEAD for tags).

Implement all three in `JGitRepository`.

#### 1f. Extend `GitHubToolbarViewModel`

New properties:

```java
private final ObjectProperty<RefType> refType = new SimpleObjectProperty<>(RefType.BRANCH);
private final ListProperty<TagRef> tags = new SimpleListProperty<>(FXCollections.observableArrayList());
private final ListProperty<String> recentRefs = new SimpleListProperty<>(FXCollections.observableArrayList());
```

New property accessors:

```java
public ReadOnlyObjectProperty<RefType> refTypeProperty()
public ReadOnlyListProperty<TagRef> tagsProperty()
public ReadOnlyListProperty<String> recentRefsProperty()
```

New command:

```java
public void update() {
    runCommand("Fetching from origin...", () -> {
        gitRepository.fetchOrigin();
        return true;
    }, value -> {
        statusText.set("Updated");
        refresh();
    });
}
```

Changes to `refresh()`:

- After loading `RepoStatus`, set `refType` from `repoStatus.refType()`.
- Call `gitRepository.listTags()` and update `tags`.
- The existing `switchBranch()` success handler pushes the branch name onto `recentRefs`
  (maintained as a `Deque<String>` capped at 5, newest first, deduplicated).

Changes to `captureState()` / `fromState()` in `GitHubToolbar`:

- Serialize `recentRefs` as a comma-separated string under key `"recentRefs"`.
- Deserialize on restore.

**Deliverable:** all existing tests pass (after updating `RepoStatus` constructor sites).
New unit tests for `listTags()`, `fetchOrigin()`, `RefType` derivation, and recent-ref
ring-buffer behavior.

---

### Phase 2 — Toolbar sub-components

**Goal:** build and unit-test each sub-component independently before wiring them into
the toolbar.

#### 2a. `StatusDot` — small colored circle

```java
// ui/toolbar/StatusDot.java
class StatusDot extends Region {
    enum State { CLEAN, DIRTY, UNAVAILABLE }
    // 8x8 px circle rendered via -fx-background-radius and -fx-background-color
    // bound to an ObjectProperty<State>
    // Colors derived from GitHubToolbarTheme: success → CLEAN, danger → DIRTY, textMuted → UNAVAILABLE
}
```

No Canvas needed — a `Region` with fixed size and a round background is simpler and
integrates naturally with CSS theming.

#### 2b. `RepositoryPill` — clickable owner/repo

```java
// ui/toolbar/RepositoryPill.java
class RepositoryPill extends HBox {
    // Internal: icon label + name label
    // Constructor: RepositoryPill(String ownerRepo, URI remoteUrl, GitHubToolbarTheme theme)
    // Click and Enter/Space → Desktop.browse(remoteUrl)
    // Hover → slight background tint from theme
    // Styled with rounded corners matching theme.compactRadius()
    // ID: "github-repo-pill"
}
```

Replaces: `repoLink` (Hyperlink), `repoLinkSurface` (HBox wrapper) from current
`GitHubToolbar` lines 119–129.

#### 2c. `RefPill` — clickable ref switcher

```java
// ui/toolbar/RefPill.java
class RefPill extends HBox {
    // Internal: StatusDot + icon label + ref name label + chevron label
    // Constructor: RefPill(GitHubToolbarTheme theme)
    // Bindings accepted:
    //   - ObjectProperty<StatusDot.State> dotState
    //   - ObjectProperty<RefType> refType (drives icon: branch/tag/detached glyph)
    //   - StringProperty refName
    // Click, Enter, Space, Alt+Down → fires onAction callback (opens popup)
    // Focusable: true
    // ID: "github-ref-pill"
}
```

Replaces: `branchCombo`, `checkoutButton`, `currentBranchBadge`, `dirtyBadge`,
`detachedBadge` from current toolbar.

StatusDot.State derivation:

```java
if (!localAvailable) → UNAVAILABLE
else if (dirty) → DIRTY
else → CLEAN
```

This logic lives in `GitHubToolbar` and is pushed into the `RefPill` via property binding.

#### 2d. `ChipStrip` — sparse contextual chips

```java
// ui/toolbar/ChipStrip.java
class ChipStrip extends HBox {
    // Constructor: ChipStrip(GitHubToolbarTheme theme)
    // Method: rebuild(ChipStrip.Context ctx)
    //   clears children, then conditionally adds Label chips:
    //   - "Default" when ctx.defaultBranchActive
    //   - "Detached" when ctx.detachedHead
    //   - "Remote only" when !ctx.localAvailable
    //   - "Ahead N" when ctx.aheadCount > 0
    //   - "Behind N" when ctx.behindCount > 0
    //   - "N changed" when ctx.dirty && ctx.dirtyCount > 0
    //   - "No token" when !ctx.authenticated
    // Each chip is a Label with style class "pf-github-chip" + variant class
    // ID: "github-chip-strip"
}
```

Replaces: `refreshBadges()` (lines 373–414) and the eight always-present badge labels.

Critical behavioral change from current code: the mode badge ("Local clone" / "Remote only")
and auth badge ("Token" / "No token") are currently always visible (lines 391–393,
411–413). In the new design, "Remote only" only appears when `!localAvailable`, and
"No token" only appears when `!authenticated`. The "Local clone" and "Token" states are
the happy path and show nothing.

#### 2e. `ActionStrip` — flat right-aligned buttons

```java
// ui/toolbar/ActionStrip.java
class ActionStrip extends HBox {
    // Buttons: updateButton, commitButton, pushButton, prButton, moreButton
    // moreButton opens a ContextMenu with: Rollback, Token
    // All buttons styled flat (class "pf-github-action-button") — no group borders
    // Button disable bindings wired by GitHubToolbar after construction
    // ID: "github-action-strip"
}
```

Replaces: `branchGroup`, `changesGroup`, `remoteGroup` as separate bordered HBox groups.

The `newBranchButton` moves into the popup action block. The `rollbackButton` and
`tokenButton` move into the `…` overflow `ContextMenu`.

#### 2f. `StatusSlot` — transient far-right status

```java
// ui/toolbar/StatusSlot.java
class StatusSlot extends HBox {
    // Internal: busyIndicator (ProgressIndicator 16x16) + statusLabel + errorLabel
    // busyIndicator visible when busy
    // statusLabel bound to statusText; hidden when text is empty or "Idle" or "Ready"
    // errorLabel styled as red pill; visible when errorText is non-empty
    // When idle (not busy, no error, no recent status), the entire slot is hidden
    // ID: "github-status-slot"
}
```

Replaces: `statusGroup` and the `aheadBadge`/`behindBadge`/`authBadge` that currently
live inside it. Ahead/behind move to `ChipStrip`. Auth badge is removed (replaced by
"No token" chip).

**Deliverable:** each sub-component can be instantiated in a TestFX `@Start` stage and
verified for correct node structure, visibility, and basic interaction.

---

### Phase 3 — Theme token additions

**Goal:** provide color and size tokens for the new components without breaking existing
theme mapping.

#### 3a. Extend `GitHubToolbarTheme` record

Add after `shadow` (line 30):

```java
// Status dot
Paint dotClean,
Paint dotDirty,
Paint dotUnavailable,

// Pill surfaces
Paint pillBackground,
Paint pillHoverBackground,
Paint pillBorder,

// Chip surface
Paint chipBackground,
Paint chipText,

// Popup
Paint popupBackground,
Paint popupBorder,
Paint popupItemHover,
Paint popupSectionText
```

#### 3b. Update `GitHubToolbarThemeMapper.map()`

Derive new tokens from existing base theme colors:

```java
Color dotClean = success;                            // reuse existing success
Color dotDirty = danger;                             // reuse existing danger
Color dotUnavailable = textMuted;                    // gray dot for remote-only

Color pillBackground = blend(toolbarBackground, accent, dark ? 0.08 : 0.04);
Color pillHoverBackground = blend(pillBackground, accent, dark ? 0.12 : 0.08);
Color pillBorder = blend(border, accent, dark ? 0.15 : 0.10);

Color chipBackground = blend(groupBackground, background, dark ? 0.14 : 0.06);
Color chipText = textMuted;

Color popupBackground = dark ? blend(toolbarBackground, Color.BLACK, 0.12)
                             : blend(toolbarBackground, Color.WHITE, 0.06);
Color popupBorder = border;
Color popupItemHover = hover;
Color popupSectionText = textMuted;
```

#### 3c. Update `GitHubThemeSupport.themeVariables()`

Add CSS custom properties:

```
--pf-github-dot-clean, --pf-github-dot-dirty, --pf-github-dot-unavailable
--pf-github-pill-bg, --pf-github-pill-hover-bg, --pf-github-pill-border
--pf-github-chip-bg, --pf-github-chip-text
--pf-github-popup-bg, --pf-github-popup-border, --pf-github-popup-item-hover, --pf-github-popup-section-text
```

#### 3d. Add CSS rules to `github-toolbar.css`

```css
.pf-github-repo-pill {
    -fx-background-color: -pf-github-pill-bg;
    -fx-background-radius: ...;
    -fx-padding: 2 10 2 8;
    -fx-cursor: hand;
}
.pf-github-repo-pill:hover {
    -fx-background-color: -pf-github-pill-hover-bg;
}

.pf-github-ref-pill { /* similar to repo pill but with focus ring support */ }
.pf-github-ref-pill:hover { ... }
.pf-github-ref-pill:focused { -fx-border-color: -pf-github-focus-border; }

.pf-github-status-dot { /* 8x8, round background */ }

.pf-github-chip {
    -fx-background-color: -pf-github-chip-bg;
    -fx-text-fill: -pf-github-chip-text;
    -fx-background-radius: ...;
    -fx-padding: 1 6;
    -fx-font-size: 0.85em;
}

.pf-github-action-button { /* flat, no border, minimal padding */ }

.pf-github-popup-root { /* 360-420 width, max 520 height, shadow, border */ }
.pf-github-popup-section-header { /* bold, muted text */ }
.pf-github-popup-item { /* full-width row with padding */ }
.pf-github-popup-item:hover { -fx-background-color: -pf-github-popup-item-hover; }
```

**Deliverable:** `GitHubToolbarThemeMapperTest` updated with assertions for new tokens.
`GitHubToolbarThemeIntegrationTest` verifies new CSS variables appear in the style string.

---

### Phase 4 — Ref popup

**Goal:** replace the `ComboBox` dropdown with a rich, searchable, sectioned popup.

#### 4a. New file: `ui/popup/RefPopup.java`

Extends `Popup` (not `PopupControl` — avoids Skin boilerplate).

```java
class RefPopup extends Popup {
    private final RefPopupContent content;

    RefPopup(GitHubToolbarTheme theme) {
        setAutoHide(true);
        setAutoFix(true);
        content = new RefPopupContent(theme);
        getContent().add(content);
    }

    void showBelow(Node anchor) {
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
        Platform.runLater(() -> content.focusSearch());
    }
}
```

#### 4b. New file: `ui/popup/RefPopupContent.java`

A `VBox` containing:

1. **Search field** (`TextField`) — filters all sections. Typing is immediate. Escape
   in the search field closes the popup.

2. **Action block** — five clickable rows:
   - `Update project…` → calls `viewModel.update()`
   - `Commit…` → calls `toolbar.onCommit()`
   - `Push…` → calls `viewModel.push()`
   - `New Branch…` → calls `toolbar.onCreateBranch()`
   - `Checkout Tag or Revision…` → placeholder, disabled in v1

3. **Section: Recent** — up to 5 entries from `viewModel.recentRefsProperty()`.
   Each row shows: icon + ref name + tracking label (right-aligned, muted).

4. **Section: Local** — from `viewModel.branchesProperty()` filtered to `local == true`.
   Each row: branch icon + name + `trackingTarget` (right-aligned). Current branch is
   visually marked (bold or accent color). Clicking checks out the branch.

5. **Section: Remote** — from `viewModel.branchesProperty()` filtered to `remote == true`,
   grouped by remote name (extracted from `fullName`, e.g. `refs/remotes/origin/main`
   → group `origin`, display `main`). Remote names are collapsible tree items.

6. **Section: Tags** — from `viewModel.tagsProperty()`.
   Clicking a tag calls `viewModel.switchBranch(tagName, false)` which will be extended
   to handle tag checkout via `checkoutRef()`.

Each section is a `VBox` with a header `Label` and item rows. The section collapses to
zero height when all its items are filtered out by search.

**Item rows** are `HBox` nodes with:
- Type icon (branch / tag / remote / detached glyph as text)
- Name label (left-growing)
- Tracking label (right-aligned, muted color)
- Optional trailing chevron `›` for items with sub-actions

**Side submenu** — a `ContextMenu` shown on chevron click or hover. Branch submenu items:
- `Checkout` — calls `switchBranch()`
- `Checkout and Track` — for remote branches (calls `createAndCheckout()`)
- `Show Diff with Working Tree` — disabled stub
- `Rename…` — disabled stub
- `Delete Local Branch…` — disabled stub

Tag submenu items:
- `Checkout Tag` — calls `checkoutRef()`
- `Open Commit` — disabled stub
- `Compare with Working Tree` — disabled stub

Disabled stubs show the menu item but grayed out, indicating future availability.

**Keyboard navigation:**
- `↑` / `↓` moves visual selection across all items (skips section headers).
- `Enter` activates the selected item.
- `→` opens submenu on the selected item if it has one.
- `Escape` closes the popup.
- Any printable key input moves focus to the search field.

**Deliverable:** popup opens, search filters items across all sections, keyboard
navigation works, branch/tag checkout triggers the correct ViewModel command.

---

### Phase 5 — Toolbar layout rewrite

**Goal:** replace the five-group HBox layout in `GitHubToolbar` with the identity-first
design.

#### 5a. New layout

```
GitHubToolbar (HBox, id="github-toolbar")
├── repositoryPill (RepositoryPill, id="github-repo-pill")
├── refPill (RefPill, id="github-ref-pill")
├── chipStrip (ChipStrip, id="github-chip-strip")
├── spacer (Region, HGrow.ALWAYS)
├── actionStrip (ActionStrip, id="github-action-strip")
│   ├── updateButton (id="github-update-button")
│   ├── commitButton (id="github-commit-button")
│   ├── pushButton (id="github-push-button")
│   ├── prButton (id="github-pr-button")
│   └── moreButton (id="github-more-button")
└── statusSlot (StatusSlot, id="github-status-slot")
    ├── busyIndicator (id="github-busy-indicator")
    ├── statusLabel (id="github-status-text")
    └── errorLabel (id="github-error-text")
```

#### 5b. Removed fields

These fields from the current `GitHubToolbar` are deleted:

| Field | Replacement |
|---|---|
| `repoLink` (Hyperlink) | `RepositoryPill` handles browser navigation |
| `repoLinkSurface` (HBox) | absorbed into `RepositoryPill` |
| `branchCombo` (ComboBox) | `RefPill` + `RefPopup` |
| `checkoutButton` | popup item |
| `newBranchButton` | popup action block |
| `rollbackButton` | `…` overflow ContextMenu |
| `tokenButton` | `…` overflow ContextMenu |
| `currentBranchBadge` | `RefPill` ref name label |
| `dirtyBadge` | `StatusDot` color inside `RefPill` |
| `modeBadge` | "Remote only" chip in `ChipStrip` (only when remote-only) |
| `defaultBranchBadge` | "Default" chip in `ChipStrip` |
| `detachedBadge` | "Detached" chip in `ChipStrip` + ref type icon in `RefPill` |
| `aheadBadge` | "Ahead N" chip in `ChipStrip` |
| `behindBadge` | "Behind N" chip in `ChipStrip` |
| `authBadge` | "No token" chip in `ChipStrip` (only when unauthenticated) |
| `repoGroup` (HBox) | replaced by flat layout |
| `branchGroup` (HBox) | removed |
| `changesGroup` (HBox) | removed |
| `remoteGroup` (HBox) | replaced by `actionStrip` |
| `statusGroup` (HBox) | replaced by `statusSlot` |

#### 5c. Wiring

```java
// In constructor, after creating sub-components:

// Repository pill
repositoryPill.setOwnerRepo(viewModel.context().owner() + "/" + viewModel.context().repo());
repositoryPill.setRemoteUrl(viewModel.context().remoteUrl());

// Ref pill status dot
refPill.dotStateProperty().bind(Bindings.createObjectBinding(() -> {
    if (!viewModel.localAvailableProperty().get()) return StatusDot.State.UNAVAILABLE;
    if (viewModel.dirtyProperty().get()) return StatusDot.State.DIRTY;
    return StatusDot.State.CLEAN;
}, viewModel.localAvailableProperty(), viewModel.dirtyProperty()));

refPill.refTypeProperty().bind(viewModel.refTypeProperty());
refPill.refNameProperty().bind(viewModel.currentBranchProperty());
refPill.setOnAction(() -> refPopup.showBelow(refPill));

// Chip strip — rebuilt on any state change
InvalidationListener chipListener = obs -> chipStrip.rebuild(new ChipStrip.Context(
    viewModel.defaultBranchActiveProperty().get(),
    viewModel.detachedHeadProperty().get(),
    viewModel.localAvailableProperty().get(),
    viewModel.aheadCountProperty().get(),
    viewModel.behindCountProperty().get(),
    viewModel.dirtyProperty().get(),
    viewModel.dirtyCountProperty().get(),
    viewModel.authenticatedProperty().get()
));
// attach chipListener to all relevant properties

// Action strip disable bindings
actionStrip.updateButton().disableProperty().bind(viewModel.busyProperty());
actionStrip.commitButton().disableProperty().bind(viewModel.commitDisabledProperty());
actionStrip.pushButton().disableProperty().bind(viewModel.pushDisabledProperty());
actionStrip.prButton().disableProperty().bind(viewModel.pullRequestDisabledProperty());

// Remote-only: hide update and commit
actionStrip.updateButton().visibleProperty().bind(viewModel.localAvailableProperty());
actionStrip.commitButton().visibleProperty().bind(viewModel.localAvailableProperty());
// push stays visible (disabled when !authenticated)
```

#### 5d. Dialog handling

All dialog-opening methods (`onCommit()`, `onRollback()`, `onCreateBranch()`,
`onCreatePullRequest()`, `onToken()`) move unchanged from the current `GitHubToolbar`
into the new one. They are called by `ActionStrip` buttons, `RefPopup` action items,
and the overflow `ContextMenu`.

#### 5e. `applyTheme()` update

The theme application loop (current lines 428–475) is simplified:

- `repositoryPill.applyTheme(theme)` — sets pill background/hover colors
- `refPill.applyTheme(theme)` — sets dot colors, pill background, icon/text colors
- `chipStrip.applyTheme(theme)` — sets chip background/text colors
- `actionStrip.applyTheme(theme)` — sets button height, font, flat styling
- `statusSlot.applyTheme(theme)` — sets indicator/label styling
- `refPopup.applyTheme(theme)` — sets popup background, border, item hover

The `buildRootStyle()` method still emits CSS custom properties via
`GitHubThemeSupport.themeVariables()` for the stylesheet rules.

#### 5f. State persistence

`captureState()` extended:

```java
public Map<String, Object> captureState() {
    String localPath = context().localClonePath() == null ? "" : context().localClonePath().toString();
    String recentCsv = String.join(",", viewModel.recentRefsProperty());
    return Map.of(
        "remoteUrl", context().remoteUrl().toString(),
        "localClonePath", localPath,
        "owner", context().owner(),
        "repo", context().repo(),
        "recentRefs", recentCsv
    );
}
```

`fromState()` extended to parse and pass recent refs to the ViewModel.

**Deliverable:** the new toolbar renders correctly. `GitHubToolbarSample` works
interactively.

---

### Phase 6 — Tests

#### 6a. Test IDs that will break and their replacements

| Old ID | Old assertion | New ID | New assertion |
|---|---|---|---|
| `#github-repo-group` | assertNotNull | `#github-repo-pill` | assertNotNull |
| `#github-branch-group` | assertNotNull, isVisible | removed | — |
| `#github-changes-group` | assertNotNull, isVisible | removed | — |
| `#github-remote-group` | assertNotNull | `#github-action-strip` | assertNotNull |
| `#github-status-group` | assertNotNull | `#github-status-slot` | assertNotNull |
| `#github-mode-badge` | getText "Local clone"/"Remote only" | `#github-chip-strip` | contains/does-not-contain "Remote only" chip |
| `#github-dirty-badge` | getText "Dirty 1" | `#github-ref-pill` | StatusDot color is red |
| `#github-ahead-badge` | getText "Ahead 2" | `#github-chip-strip` | contains "Ahead 2" chip |
| `#github-behind-badge` | getText "Behind 1" | `#github-chip-strip` | contains "Behind 1" chip |
| `#github-auth-badge` | getText "Token"/"No token" | `#github-chip-strip` | contains/does-not-contain "No token" chip |
| `#github-default-branch-badge` | isVisible | `#github-chip-strip` | contains "Default" chip |
| `#github-branch-combo` | — | removed | — |
| `#github-checkout-button` | — | removed (popup) | — |
| `#github-push-button` | isVisible, isDisable | `#github-push-button` | same assertions |
| `#github-pr-button` | isDisable | `#github-pr-button` | same assertions |
| `#github-commit-button` | isDisable | `#github-commit-button` | same assertions |

#### 6b. Updated `GitHubToolbarFxTest` scenarios

1. **`rendersIdentityFirstToolbar()`** — verify `#github-repo-pill` and `#github-ref-pill`
   are the first two children. Verify chip strip contains "Ahead 2", "Behind 1" chips.
   Verify status dot color is red when dirty.

2. **`remoteOnlyHidesLocalActions()`** — verify `#github-update-button` and
   `#github-commit-button` are not visible. Verify chip strip contains "Remote only".
   Verify status dot color is gray.

3. **`commitDisabledOnDefaultBranch()`** — verify `#github-commit-button` is disabled.
   Verify chip strip contains "Default".

4. **`pushAndPrDisabledUntilToken()`** — verify `#github-push-button` and `#github-pr-button`
   disabled. Verify chip strip contains "No token". After saving token, verify chip
   strip does NOT contain "No token".

5. **`exportReviewSnapshotsWhenRequested()`** — updated to export snapshots for the new
   design, including a popup-open snapshot.

#### 6c. New test: `RefPillFxTest`

- Verify correct StatusDot color for CLEAN (green), DIRTY (red), UNAVAILABLE (gray).
- Verify ref name label text binding.
- Verify ref type icon changes (branch glyph vs tag glyph vs detached glyph).
- Verify click fires onAction callback.
- Verify `Enter`, `Space`, `Alt+Down` keyboard activation.

#### 6d. New test: `RefPopupFxTest`

- Verify popup opens anchored below a node.
- Verify search field has focus on open.
- Verify search text filters Local section (type "feat" → only "feature/x" visible).
- Verify clicking a local branch row calls `switchBranch()`.
- Verify `Escape` closes the popup.
- Verify action block items delegate to the correct ViewModel methods.
- Verify tags section renders `TagRef` entries.
- Verify sections collapse when all items are filtered out.

#### 6e. New test: `RecentRefHistoryTest` (plain JUnit, no FX)

- Verify max-5 capacity.
- Verify newest-first ordering.
- Verify deduplication (pushing an existing entry moves it to the front).
- Verify serialization round-trip (comma-separated string).

#### 6f. Updated `GitHubToolbarViewModelTest`

- Add test for `refTypeProperty()` — BRANCH when normal, DETACHED_COMMIT when detached.
- Add test for `tagsProperty()` — populated after refresh.
- Add test for `update()` — calls `fetchOrigin()`, then triggers refresh.
- Add test for recent-ref tracking — `switchBranch()` success pushes to recentRefs.

#### 6g. Updated `GitHubToolbarThemeMapperTest`

- Add assertions for `dotClean`, `dotDirty`, `dotUnavailable` tokens.
- Add assertions for `pillBackground`, `chipBackground`, `popupBackground` tokens.

**Deliverable:** full test suite passes headless.

```bash
./mvnw -pl papiflyfx-docking-github -am -Dtestfx.headless=true test
```

---

### Phase 7 — Sample and snapshots

#### 7a. Update `GitHubToolbarSample`

- Local-clone scenario: green dot, "Ahead 2" chip, full action strip.
- Remote-only scenario: gray dot, "Remote only" chip, reduced actions.
- Add an interactive button to simulate dirty state (red dot, "3 changed" chip).
- Add an interactive button to open the ref popup manually.

#### 7b. Export review snapshots

Store under `spec/papiflyfx-docking-github/review1-ui/`:

1. `github-toolbar-local-clean-dark.png`
2. `github-toolbar-local-clean-light.png`
3. `github-toolbar-local-dirty-dark.png`
4. `github-toolbar-default-branch-dark.png`
5. `github-toolbar-detached-tag-dark.png`
6. `github-toolbar-remote-only-dark.png`
7. `github-toolbar-popup-open-dark.png`

---

## 5. Execution Order and Dependencies

```
Phase 1 (model + git layer)
  │
  ├──► Phase 2 (toolbar sub-components) ──┐
  │                                        │
  └──► Phase 3 (theme tokens) ────────────┤
                                           │
                                           ▼
                                     Phase 4 (ref popup)
                                           │
                                           ▼
                                     Phase 5 (toolbar rewrite)
                                           │
                                           ▼
                                     Phase 6 (tests)
                                           │
                                           ▼
                                     Phase 7 (sample + snapshots)
```

Phases 2 and 3 can proceed in parallel once Phase 1 is complete.
Phase 4 depends on Phase 1 (needs tags, ref type) and Phase 2 (needs RefPill as anchor).
Phase 5 depends on all of 2, 3, and 4.

---

## 6. What Is NOT Changed

- `GitHubToolbarViewModel` — only additive extensions; all 17 existing `ReadOnly*Property`
  accessors preserved; all six command methods (`refresh`, `switchBranch`, `createBranch`,
  `commit`, `rollback`, `push`, `createPullRequest`, `saveToken`) preserved.
- `CommandRunner` — unchanged.
- All six dialog classes (`CommitDialog`, `DirtyCheckoutAlert`, `NewBranchDialog`,
  `PullRequestDialog`, `RollbackDialog`, `TokenDialog`) — unchanged.
- `GitHubRepoContext` record — unchanged.
- `CommitInfo`, `PullRequestDraft`, `PullRequestResult`, `RollbackMode` — unchanged.
- Auth layer (`CredentialStore`, `PatCredentialStore`, `KeychainTokenStore`,
  `PreferencesTokenStore`) — unchanged.
- `GitHubApiService` — unchanged.
- `RemoteUrlParser` — unchanged.
- `GitHubToolbarContribution`, `GitHubToolbarFactory`, `GitHubToolbarStateAdapter` —
  unchanged.

---

## 7. Risk Assessment

| Risk | Mitigation |
|---|---|
| `Desktop.browse()` may fail in headless test on Java 25 | Mock the browser call behind a `BrowseAction` functional interface; inject a no-op in tests. The current code already guards with `Desktop.isDesktopSupported()` (line 417). |
| JavaFX `Popup` focus on macOS — search field may not receive focus immediately | Use `Platform.runLater(() -> searchField.requestFocus())` after `popup.show()`. Test both headless and interactively. |
| JGit `listTags()` may be slow on repos with many tags | Load tags lazily only when the popup opens, not during `refresh()`. Cache the tag list and invalidate on `update()`. |
| JGit reflog for recent refs may be unavailable or pruned | `recentRefs` falls back to empty list. The Recent section simply hides. |
| `BranchRef` six-arg constructor may break if downstream code exists outside the module | The record is in the `model` package which is module-internal. All five-arg call sites are in tests and `JGitRepository` — all within the module. |
| Nested popup focus (side submenu ContextMenu + main Popup) | Use `ContextMenu` for submenus — it has its own focus management independent of the parent Popup. Test that closing the submenu returns focus to the main popup rather than closing both. |
| `RepoStatus` record component reorder breaks serialization | `RepoStatus` is never serialized externally — it is created fresh on each `loadStatus()` call. Only constructor call sites need updating. |

---

## 8. Exit Criteria

This refactor is complete when:

1. No `ComboBox` or bordered group HBox remains in the toolbar.
2. Repository pill and ref pill are the first two readable elements.
3. The popup is the primary ref-switching surface with working search.
4. Chips only appear when they represent non-default state.
5. The status slot is hidden when idle.
6. All five state examples from the spec render correctly:
   - Clean local branch with ahead/behind chips
   - Dirty working tree with red dot and "N changed" chip
   - Default branch with "Default" chip and disabled Commit
   - Detached on tag with "Detached" chip
   - Remote-only with gray dot and "Remote only" chip
7. The module test suite passes headless: `./mvnw -pl papiflyfx-docking-github -am -Dtestfx.headless=true test`
8. Review snapshots exported under `spec/papiflyfx-docking-github/review1-ui/`.