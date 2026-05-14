# GitHub Toolbar Color Theme Harmonization Plan

## 1. Goal

Bring `papiflyfx-docking-github` onto the same visual level as the stronger in-repo components, especially:

- `papiflyfx-docking-code` search / go-to overlays
- `papiflyfx-docking-hugo` toolbar grouping and action hierarchy
- the shared docking `Theme`

This plan focuses on visual design, theming architecture, and UX polish for the GitHub toolbar first, then the dialogs it opens so the experience stays coherent end-to-end.

Assumption: the review note's `huge` reference means `HugoPreviewToolbar`.

## 2. Reviewed Artifacts

- `spec/papiflyfx-docking-github/review0-color-theme/README.md`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/api/GitHubToolbar.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/GitHubToolbarViewModel.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/dialog/CommitDialog.java`
- `papiflyfx-docking-github/src/main/java/org/metalib/papifly/fx/github/ui/dialog/TokenDialog.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/api/go-to-line-overlay.css`
- `papiflyfx-docking-hugo/src/main/java/org/metalib/papifly/fx/hugo/api/HugoPreviewToolbar.java`
- `papiflyfx-docking-github/src/test/java/org/metalib/papifly/fx/github/ui/GitHubToolbarFxTest.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/github/GitHubToolbarSample.java`

## 3. Current Baseline

### 3.1 What the current toolbar does well

- The toolbar already binds to a `Theme` property.
- The data and command model is cleanly separated in `GitHubToolbarViewModel`.
- IDs already exist for most controls, so targeted FX tests and CSS hooks are practical.

### 3.2 Current visual problems

1. `GitHubToolbar` is built as one long row of stock JavaFX controls and literal `"|"` separators.
   - See `GitHubToolbar.java:72-173` and `GitHubToolbar.java:303-307`.
2. `applyTheme(...)` only styles the container background, border, status text, dirty dot, and error text.
   - See `GitHubToolbar.java:309-321`.
3. Buttons, combo box, hyperlink, and progress indicator are still mostly Modena defaults, so they do not read as part of the docking chrome.
4. Hard-coded values dominate spacing, padding, widths, and colors.
   - Examples: `setSpacing(8)`, `setPadding(new Insets(6, 10, 6, 10))`, fixed combo width `180`, hard-coded error color `#d9534f`.
5. Shared `Theme` tokens such as `headerHeight`, `cornerRadius`, `buttonSpacing`, `headerFont`, `contentFont`, `buttonHoverBackground`, and `buttonPressedBackground` are not used.
   - See `Theme.java:35-118`.
6. The toolbar exposes very little of the repository state visually.
   - `RepoStatus` already has `aheadCount`, `behindCount`, and per-file dirty buckets, but the view model reduces this mostly to `dirty`, branch, and plain text status.
   - See `GitHubToolbarViewModel.java:43-56` and `GitHubToolbarViewModel.java:94-147`.
7. Remote-only mode disables several local actions but keeps the whole dead control set visible.
   - This is functionally correct but visually noisy.
8. The dialogs opened from the toolbar are also stock JavaFX dialog panes, so even a redesigned toolbar would currently drop the user into an inconsistent visual language.
   - See `CommitDialog.java:13-37` and `TokenDialog.java:13-36`.
9. Existing tests verify behavior and enable/disable rules, but they do not protect the visual contract.
   - See `GitHubToolbarFxTest.java:45-95`.

### 3.3 Stronger reference patterns already present in the repo

1. The code search overlay uses a good pattern:
   - static CSS classes in a stylesheet
   - runtime CSS variables set from theme data
   - programmatic color application only where JavaFX CSS needs help
   - See `SearchController.java:552-603` and `search-overlay.css`.
2. The Hugo toolbar uses better composition:
   - grouped surfaces instead of pipe separators
   - clear action hierarchy
   - shaped controls with intent
   - See `HugoPreviewToolbar.java:20-330`.

### 3.4 Detailed theme coverage audit

Current `GitHubToolbar.applyTheme(...)` coverage is narrower than it first appears.

| Element | Current theme coverage | Gap |
|---|---|---|
| Toolbar root container | background, border | no group-level or control-level styling |
| `statusLabel` | text fill | no font, no semantic status treatment |
| `dirtyDot` | accent text fill | relies on color only |
| `errorLabel` | hardcoded error red | not theme-derived, no font |
| Buttons | none | background, text, hover, pressed, focus, disabled |
| `branchCombo` | none | field surface, arrow button, text, disabled, focus |
| `repoLink` | none | link color, hover, visited, contained surface |
| `busyIndicator` | none | accent / track styling remains platform default |
| Separator labels | opacity only | should use semantic divider treatment |
| Dialogs | none | background, border, inputs, buttons, warning text |

Additional explicit gap list from the current implementation:

- There are seven stock `Button` controls with no toolbar-specific theme treatment.
- The separator labels use a fixed opacity instead of `Theme.dividerColor()`.
- Fonts from `Theme.headerFont()` and `Theme.contentFont()` are not propagated into toolbar controls.

### 3.5 Utility and constructor gaps

Two low-level issues from the current code deserve explicit tracking because they are easy to miss during a visual pass.

1. `toHex(Paint)` strips alpha.
   - Any translucent paint becomes opaque.
   - This is already solved better in `SearchController.paintToCss(...)`, which emits `rgba(r,g,b,a)`.
2. `GitHubToolbar` applies `Theme.dark()` in the constructor before external theme binding.
   - In a light-themed host, that can cause an initial dark flash before `bindThemeProperty(...)` runs.
   - This matters most for convenience constructors and `fromState(...)`, where the caller cannot currently seed an initial theme.

### 3.6 Additional reference pattern: `DockTabGroup`

The toolbar should also align with the more programmatic JavaFX styling approach already used in docking chrome.

| Visual trait | `DockTabGroup` pattern | Current `GitHubToolbar` state |
|---|---|---|
| Container background | strongly-typed JavaFX `Background` API | root inline style string |
| Hover / pressed states | explicit themed behavior | not implemented |
| Font usage | `theme.headerFont()` / `theme.contentFont()` | not applied |
| Corner radius | derived from `theme.cornerRadius()` | not applied consistently |
| Divider color | semantic divider paint | literal `"|"` label with opacity |
| Accent / focus treatment | theme accent used structurally | accent only reaches dirty dot |

## 4. Design Direction

The GitHub toolbar should feel like application chrome, not a default form row.

Recommended target characteristics:

- Theme-derived in both dark and light modes
- Compact grouped surfaces similar to a professional IDE toolbar
- Rounded controls and badges that visually relate to the code search overlays
- Clear action weighting instead of every button looking equivalent
- Repository state shown as small chips / badges rather than plain text only
- No literal text separators
- No hard-coded dark palette copied from Hugo

Important design rule:

- Borrow Hugo's grouping and action hierarchy, not its exact colors.
- Borrow the code search overlay's theme-variable architecture, not its exact layout.

## 5. Proposed Technical Approach

### 5.1 Add a GitHub-specific derived theme layer

Create a local theme model for the GitHub UI instead of expanding `Theme` immediately.

Recommended new types:

- `org.metalib.papifly.fx.github.ui.theme.GitHubToolbarTheme`
- `org.metalib.papifly.fx.github.ui.theme.GitHubToolbarThemeMapper`

Why this is the right first step:

- `Theme` is intentionally small and shared across modules.
- The GitHub toolbar needs more semantic tokens than `Theme` currently exposes.
- `papiflyfx-docking-code` already proves the mapper pattern works well via `CodeEditorThemeMapper`.

Recommended derived tokens:

- `toolbarBackground`
- `toolbarBorder`
- `groupBackground`
- `groupBorder`
- `controlBackground`
- `controlBackgroundHover`
- `controlBackgroundPressed`
- `controlBorder`
- `focusBorder`
- `textPrimary`
- `textMuted`
- `textDisabled`
- `linkText`
- `accent`
- `success`
- `warning`
- `danger`
- `badgeBackground`
- `badgeBorder`
- `statusBackground`
- `errorBackground`
- `busyTrack`
- `busyIndicator`
- `shadow`
- `cornerRadius`
- `compactRadius`
- `toolbarHeight`
- `buttonHeight`
- `contentPadding`
- `groupGap`

Mapping rules:

- Base surface should come from `Theme.headerBackground()`.
- Accent should flow from `Theme.accentColor()`.
- Hover and pressed surfaces should derive from `Theme.buttonHoverBackground()` and `Theme.buttonPressedBackground()`.
- Text and borders should derive from `Theme.textColor()`, `Theme.textColorActive()`, and `Theme.borderColor()`.
- Success / warning / danger should be derived locally from light-or-dark mode, not added to `Theme` yet.

Recommendation:

- Keep `Theme` unchanged in this review.
- Revisit shared semantic status colors only if another module needs the same tokens after this work.

### 5.2 Move the toolbar to stylesheet-driven rendering

Add:

- `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-toolbar.css`
- optionally `papiflyfx-docking-github/src/main/resources/org/metalib/papifly/fx/github/ui/github-dialog.css`

Implementation pattern should match the code search overlay:

1. Load a stylesheet once.
2. Apply stable style classes to nodes.
3. Set CSS custom properties on the toolbar root from the mapped theme.
4. Use programmatic color assignment only for cases JavaFX CSS does not handle well, such as SVG icon fills or progress graphics.

This should replace the current narrow `applyTheme(...)` implementation.

Recommended root style variables:

- `-pf-github-toolbar-bg`
- `-pf-github-toolbar-border`
- `-pf-github-group-bg`
- `-pf-github-group-border`
- `-pf-github-control-bg`
- `-pf-github-control-hover-bg`
- `-pf-github-control-pressed-bg`
- `-pf-github-control-border`
- `-pf-github-focus-border`
- `-pf-github-text`
- `-pf-github-muted-text`
- `-pf-github-disabled-text`
- `-pf-github-link`
- `-pf-github-accent`
- `-pf-github-success`
- `-pf-github-warning`
- `-pf-github-danger`
- `-pf-github-badge-bg`
- `-pf-github-badge-border`
- `-pf-github-error-bg`
- `-pf-github-shadow`

### 5.3 Supplemental engineering notes from the current code audit

These are low-level implementation details worth keeping even if the final design stays CSS-first.

1. Replace `toHex(Paint)` with an alpha-safe helper similar to `SearchController.paintToCss(...)`.
   - Keep an opaque-hex helper only if a specific JavaFX API call actually requires it.
2. If any controls remain programmatically themed, promote constructor-local nodes to fields first.
   - At minimum: all action buttons, `repoLink`, and any separator nodes that still exist.
3. If any grouped surfaces are implemented programmatically, prefer JavaFX `Background` / `Border` APIs for containers over long style strings.
4. If any button hover / pressed behavior is implemented programmatically, avoid repeated string replacement and listener accumulation across theme updates.
   - Preferred fallback pattern: keep normal / hover / pressed styles in a small record stored on the control.
5. `ProgressIndicator` accent styling is likely a best-effort path even in a CSS-first design.
   - If JavaFX skin limitations prevent full color control, accept a partial result rather than over-engineering it.

### 5.4 Initial theme seeding and no-flash behavior

The final implementation should explicitly address first-render theme correctness.

Recommended options:

1. Allow constructors, factories, and `fromState(...)` paths to accept an initial theme.
2. Or ensure the toolbar is not rendered before theme binding in sample / host integration code.

Minimum requirement:

- The toolbar should not briefly render in dark mode inside a light-themed application.

## 6. Toolbar UX Redesign

### 6.1 Replace the flat row with grouped surfaces

Recommended visual grouping:

1. Repository group
   - repo icon / link
   - current branch badge
   - dirty badge
   - mode badge: `Local` or `Remote only`
2. Branch group
   - branch combo
   - `Checkout`
   - `New Branch`
3. Change group
   - `Commit`
   - `Rollback`
4. Remote group
   - `Push`
   - `Create PR`
   - `Token`
5. Status group
   - busy spinner
   - ahead / behind chips
   - status text
   - error badge when needed

This directly removes the current dependence on `|` separators and gives the toolbar the same intentional structure that the Hugo toolbar already has.

### 6.2 Action hierarchy

Do not render every action with the same visual weight.

Recommended weights:

- Neutral buttons: `Checkout`, `New Branch`, `Token`
- Primary-accent button: `Push`
- Accent-outline button: `Create PR`
- Standard commit action: `Commit`
- Danger-outline action: `Rollback`

If desired later, `Commit` can become the primary-accent action only when the repo is dirty, but that should be treated as an optional refinement, not part of the first implementation pass.

### 6.3 Replace the dirty dot with richer chips

Current state:

- dirty state is only a single colored dot

Recommended replacement:

- `Dirty` badge when there are local changes
- optional compact file-count badge if useful
- `Ahead N` and `Behind N` badges from `RepoStatus`
- `Default branch` badge when on default branch
- `Detached HEAD` badge when applicable

This uses data the module already has instead of relying on color alone.

### 6.4 Remote-only mode should be visually deliberate

Recommended behavior:

- Hide local-only action groups when there is no local clone
- Show a compact `Remote only` badge in the repository group
- Keep `Create PR` and `Token` available

Reason:

- disabled dead controls make the toolbar look broken
- a reduced layout will read as intentional and cleaner

If product requirements insist on preserving discoverability, keep disabled controls only in a fallback variant, but the primary recommendation is to collapse them.

### 6.5 Improve branch and repo presentation

Recommended UI details:

- Give the repo link a contained surface instead of bare hyperlink styling
- Add a simple SVG branch icon next to the combo box or branch badge
- Use `OverrunStyle.LEADING_ELLIPSIS` or constrained width rules on repo text where needed
- Prefer branch chip + combo surface treatment over a plain default combo box sitting directly on the toolbar background

## 7. View Model Changes Needed For The Visual Design

The redesign needs slightly richer state from `GitHubToolbarViewModel`.

Recommended additions:

- `aheadCountProperty`
- `behindCountProperty`
- `detachedHeadProperty`
- `defaultBranchActiveProperty`
- `dirtyCountProperty`
- `authenticatedProperty` already exists and should be surfaced visually
- optional `remoteOnlyProperty`

Notes:

- Do not move presentation logic into `GitRepository`.
- Keep the view model as the place where raw repo state becomes UI state.
- Continue keeping slow work off the FX thread.

Suggested behavior changes tied to the design:

- differentiate warning text from error text
- expose a concise status summary string for the trailing status region
- prefer semantic chips over a long trailing sentence when state is available structurally

## 8. Dialog Harmonization

The toolbar redesign will feel incomplete if the dialogs stay pure Modena.

Recommended scope for the same review:

- `CommitDialog`
- `NewBranchDialog`
- `RollbackDialog`
- `PullRequestDialog`
- `TokenDialog`
- `DirtyCheckoutAlert`

Recommended approach:

- Reuse the same `GitHubToolbarThemeMapper`
- Add a shared GitHub dialog stylesheet
- Style `DialogPane`, buttons, labels, text fields, combo boxes, and warning text
- Keep dialog layout simple, but use the same surface, border, and focus treatment as the toolbar

Minimum dialog theming scope from the audit:

- `DialogPane` background and border
- header / title area background
- body labels and helper text
- `TextArea`, `TextField`, `PasswordField`, and combo boxes
- action buttons, including destructive emphasis where applicable

Implementation timing note:

- If dialog button styling depends on `lookupButton(...)`, apply it in `setOnShowing(...)` or another post-scene hook.
- The constructor alone may be too early for reliable button node access.

Recommended priority:

- Phase 1 must include toolbar restyling
- Phase 2 should include dialog alignment

## 9. Concrete Implementation Phases

## Phase A - Theme foundation

- [x] Add `GitHubToolbarTheme` record.
- [x] Add `GitHubToolbarThemeMapper`.
- [x] Add light/dark detection logic based on background brightness.
- [x] Add mapper unit tests similar to `CodeEditorThemeMapperTest`.
- [x] Keep `Theme` unchanged for this pass.

## Phase B - CSS infrastructure

- [x] Add `github-toolbar.css`.
- [x] Load the stylesheet from `GitHubToolbar`.
- [x] Replace the existing `applyTheme(...)` node-by-node styling with CSS variable assignment on the toolbar root.
- [x] Add stable style classes and ids for groups, badges, action types, and status area.
- [x] Add simple SVG icon support if needed, without new runtime dependencies.
- [x] Use alpha-safe `rgba(...)` variable output for theme values instead of opaque hex-only conversion.

## Phase C - Toolbar layout refactor

- [x] Replace literal separators with grouped containers.
- [x] Split the toolbar into repository, branch, changes, remote, and status groups.
- [x] Convert the repo link into a contained surface.
- [x] Introduce badges / chips for dirty, remote-only, default-branch, ahead, and behind states.
- [x] Refine button hierarchy and spacing using theme-derived radii and heights.
- [x] Make remote-only mode collapse local groups instead of just disabling them.

## Phase D - View model enrichment

- [x] Preserve current behavior but expose richer read-only properties for UI state.
- [x] Surface ahead / behind / detached / default-branch state.
- [x] Compute a compact dirty count.
- [x] Keep command enablement logic unchanged unless required by the visual redesign.

## Phase E - Dialog alignment

- [x] Add `github-dialog.css` or a shared GitHub UI stylesheet.
- [x] Theme the GitHub dialogs with the same derived palette.
- [x] Style destructive and primary dialog actions consistently with the toolbar.
- [x] Ensure focus rings and disabled states still read clearly in both dark and light themes.
- [x] Pass the active theme from `GitHubToolbar` into dialog construction or apply it at dialog-show time.
- [x] Style dialog buttons only after button nodes are available.

## Phase F - Tests and validation

- [x] Add `GitHubToolbarThemeMapperTest`.
- [x] Add `GitHubToolbarThemeIntegrationTest` modeled after `CodeEditorThemeIntegrationTest`.
- [x] Verify dark theme variables are applied after binding.
- [x] Verify switching to `Theme.light()` changes the toolbar root style string.
- [x] Verify remote-only mode collapses the local groups if that recommendation is accepted.
- [x] Verify badges appear for dirty / ahead / behind / error states.
- [x] Verify dialogs receive the same theme treatment.
- [x] Update `GitHubToolbarFxTest` to assert style classes or high-level structure, not exact pixel colors.

## Phase G - Sample and manual review

- [x] Update `GitHubToolbarSample` to showcase both remote-only and local-clone states if practical.
- [x] Manually compare the toolbar against:
  - code search overlay in dark and light themes
  - go-to-line overlay in dark and light themes
  - Hugo toolbar grouping and action weight
- [x] Capture screenshots for the review folder if the team wants a visual record.

Completed review notes:

- Manual comparison was checked against `SearchController`, `GoToLineController`, and `HugoPreviewToolbar`; the GitHub toolbar now matches those references on grouped surfaces, action hierarchy, and theme-driven hover/focus treatment.
- Review screenshots were exported to:
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-local-dark.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-local-light.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-remote-dark.png`
  - `spec/papiflyfx-docking-github/review0-color-theme/github-toolbar-remote-light.png`

### 9.1 Supplemental file impact checklist

Likely files touched by the preferred plan:

| File | Expected change |
|---|---|
| `GitHubToolbar.java` | layout regrouping, stylesheet loading, theme-variable application, initial-theme handling |
| `GitHubToolbarViewModel.java` | richer read-only state for badges and status |
| `CommitDialog.java` | themed dialog treatment |
| `NewBranchDialog.java` | themed dialog treatment |
| `TokenDialog.java` | themed dialog treatment |
| `RollbackDialog.java` | themed dialog treatment |
| `PullRequestDialog.java` | themed dialog treatment |
| `GitHubToolbarFxTest.java` | structural and theme-aware assertions |
| `GitHubToolbarSample.java` | improved themed sample coverage |
| `github-toolbar.css` | toolbar classes and CSS variables |
| `github-dialog.css` | dialog classes and CSS variables |
| `GitHubToolbarTheme.java` | GitHub-specific derived theme contract |
| `GitHubToolbarThemeMapper.java` | mapping from docking `Theme` to GitHub UI palette |

Files that should stay unchanged unless later work proves otherwise:

- `Theme.java`
- Maven POMs
- external dependencies
- `CodeEditorTheme` and `CodeEditorThemeMapper`

## 10. Acceptance Criteria

The review should be considered complete only when all of the following are true:

1. The toolbar no longer depends on stock Modena visuals for its primary controls.
2. The toolbar updates correctly for both `Theme.dark()` and `Theme.light()`.
3. The root, groups, buttons, combo box, hyperlink surface, badges, and status area all derive from the same mapped theme.
4. The toolbar uses grouped surfaces instead of literal pipe separators.
5. Dirty, remote-only, ahead, behind, error, and default-branch states are visible without relying on color alone.
6. Remote-only mode reads as intentional rather than disabled / broken.
7. GitHub dialogs no longer feel visually disconnected from the toolbar.
8. Tests protect theme mapping and theme switching behavior.

### 10.1 Detailed visual consistency targets

In addition to the acceptance list above, the Claude review surfaced a few more concrete visual checks worth preserving:

1. The toolbar background should read as the same family as docking chrome, not as an unrelated form surface.
2. The accent color should be used consistently across repo link treatment, focus states, badges, and busy affordances.
3. Divider treatment should be semantic and theme-driven, not opacity-based decoration.
4. Toolbar typography should match docking content and chrome typography intentionally.
5. Hover and pressed states should clearly derive from `Theme.buttonHoverBackground()` and `Theme.buttonPressedBackground()`.
6. Light theme should not leak dark-only assumptions, and dark theme should not leak white dialog panes.
7. Remote-only mode should look purpose-built, not merely disabled.

## 11. Risks And How To Contain Them

1. JavaFX `ComboBox` skin styling can be brittle across platforms.
   - Prefer class-based styling of the root, display area, and arrow button.
   - Test structure and variable application instead of exact skin internals.
2. Expanding shared `Theme` too early could create unnecessary API churn.
   - Keep GitHub-specific semantic colors local for now.
3. Hugo uses a hard-coded palette that is visually strong but not theme-derived.
   - Reuse layout ideas, not literal Hugo colors.
4. It is easy to over-design a toolbar and reduce clarity.
   - Keep labels explicit.
   - Use icons only as support, not as the sole meaning carrier.
5. JavaFX user-agent styling can still interfere with some programmatic or inline overrides.
   - Prefer CSS variables + explicit style classes for controls.
   - Use strongly-typed `Background` / `Border` APIs for container surfaces where that is more reliable.
6. Dialog button nodes may not exist at constructor time.
   - Apply any `lookupButton(...)` styling in `setOnShowing(...)`, `onShown`, or equivalent late hook.
7. Reapplying theme logic can accidentally accumulate hover / pressed listeners in any programmatic fallback path.
   - Centralize control styling and avoid re-registering listeners on every theme change.

## 12. Recommended Implementation Order

1. Theme mapper and CSS variables
2. Toolbar structure and grouped surfaces
3. Semantic badges and remote-only layout cleanup
4. Dialog theming
5. Theme integration tests
6. Sample update and manual visual comparison

## 13. Recommendation Summary

The right fix is not "pick better colors" inside `GitHubToolbar.applyTheme(...)`.

The right fix is:

- create a GitHub-specific theme mapper
- move the toolbar to stylesheet-driven theming
- redesign the toolbar into grouped surfaces with semantic chips
- expose richer repo state visually
- align the dialogs with the same palette
- add theme-focused tests so the polish does not regress

## 14. Additional Notes From Claude Review

The Claude review also described a lower-change fallback path that keeps styling mostly programmatic and avoids new CSS files.

That is not the preferred direction in this document, but the alternative is worth recording:

- extend the current `applyTheme(...)` directly to all controls
- theme dialogs by styling `DialogPane` and looked-up buttons programmatically
- keep `Theme.java` unchanged
- require no new files in that fallback path
- avoid Maven or dependency changes

Decision:

- Keep the Codex plan's CSS-variable + local theme-mapper direction as the primary implementation path.
- Retain the Claude review's low-level audit notes as engineering guidance and fallback options if CSS adoption hits a JavaFX limitation.
