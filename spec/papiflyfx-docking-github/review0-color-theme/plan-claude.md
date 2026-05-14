# GitHub Toolbar Color Theme Harmonization — Implementation Plan

## Executive Summary

The `GitHubToolbar` component does not harmonize visually with the rest of the docking framework.
Its `applyTheme(Theme)` method covers only four properties (container background, border,
`statusLabel` text fill, `dirtyDot` text fill), leaving buttons, the branch combo-box, the repo
hyperlink, the busy indicator, separator labels, and all dialog windows completely unstyled.
The result is a visible mismatch: JavaFX platform-default button chrome sits alongside the
carefully themed `DockTabGroup` and `SearchController` overlays.

This plan identifies every gap and prescribes changes that mirror the patterns already established
in `DockTabGroup` (JavaFX API styling) and `SearchController` (CSS-variable inline-style approach).

---

## 1. Current State Audit

### 1.1 `GitHubToolbar.applyTheme(Theme)` — what it covers today

| Element | Property applied | Source |
|---|---|---|
| `this` (HBox container) | `-fx-background-color`, `-fx-border-color`, `-fx-border-width` | `theme.headerBackground()`, `theme.borderColor()` |
| `statusLabel` | `textFill` | `theme.textColor()` |
| `dirtyDot` | `textFill` | `theme.accentColor()` |
| `errorLabel` | `textFill` | **hardcoded** `#d9534f` |

### 1.2 Elements with no theme applied

| Element | Missing |
|---|---|
| All `Button` controls (7 buttons) | Background, border, text fill, hover/pressed states, font |
| `branchCombo` (`ComboBox`) | Background, border, text fill, arrow button styling, font |
| `repoLink` (`Hyperlink`) | Text fill, visited color, hover color |
| `busyIndicator` (`ProgressIndicator`) | No accent color; remains platform default |
| Separator `Label` (pipe character) | Uses hardcoded `opacity(0.55)` instead of `theme.dividerColor()` |
| `statusLabel`, `errorLabel` | Font not applied |
| Dialogs (5) | Zero theme support |

### 1.3 `toHex` utility limitations

- Ignores alpha channel: `Color.rgb(0,122,204, 0.3)` becomes `#007ACC`, losing transparency.
- Returns a fallback gray (`#444444`) for any non-`Color` paint.
- The `SearchController` avoids this by outputting `rgba(r,g,b,a)` format — this toolbar should do the same.

### 1.4 Constructor always applies dark theme first

```java
applyTheme(Theme.dark());   // line 172 in GitHubToolbar
```

This is correct as a fallback, but the two convenience constructors
(`GitHubToolbar(GitHubRepoContext)` and `fromState(Map)`) do not expose a `Theme` parameter,
so external callers cannot seed the initial theme before `bindThemeProperty` is called.
Consequently, the toolbar briefly flashes dark on a light-themed application.

### 1.5 Visual language gaps vs. `DockTabGroup`

| Visual trait | DockTabGroup | GitHubToolbar |
|---|---|---|
| Button/tab background | JavaFX `Background` API | CSS inline string |
| Hover state | JavaFX mouse listeners, `buttonHoverBackground` | Not implemented |
| Pressed state | JavaFX mouse listeners, `buttonPressedBackground` | Not implemented |
| Font | `theme.headerFont()` / `theme.contentFont()` | Not applied |
| Corner radius | `theme.cornerRadius()` | Not applied to buttons |
| Divider color | `theme.dividerColor()` | Not applied |
| Accent for focus | `theme.accentColor()` | Only applied to dirty-dot |

---

## 2. Reference Patterns (Do Not Deviate)

### 2.1 DockTabGroup — JavaFX API styling for containers and tabs

```java
// Container background via JavaFX API
tabBar.setBackground(new Background(new BackgroundFill(
    theme.headerBackground(), CornerRadii.EMPTY, Insets.EMPTY)));

// Tab background with corner radius, active vs. inactive
tab.setBackground(new Background(new BackgroundFill(
    active ? theme.headerBackgroundActive() : theme.headerBackground(),
    new CornerRadii(theme.cornerRadius(), theme.cornerRadius(), 0, 0, false),
    Insets.EMPTY)));
label.setTextFill(active ? theme.textColorActive() : theme.textColor());
label.setFont(theme.headerFont());
```

### 2.2 SearchController — CSS custom-property approach for controls

```java
setStyle("""
    -pf-search-bg: %s;
    -pf-search-accent: %s;
    ...
    """.formatted(paintToCss(theme.searchOverlayBackground(), "#252526"), ...));
```

Uses `rgba(r,g,b,a)` output to preserve alpha transparency.

---

## 3. Implementation Plan

### Phase 1 — Fix `toCss` utility in `GitHubToolbar`

**File:** `GitHubToolbar.java`

Replace the `toHex` method with a `toCss` method that outputs `rgba(r,g,b,a)` (matching
`SearchController.paintToCss`) and supports alpha. Also add a `toHexOpaque` for
`-fx-background-color` values that need the `#RRGGBB` hex form.

```java
private static String toCss(Paint paint, String fallback) {
    if (paint instanceof Color c) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255),
            c.getOpacity());
    }
    return fallback;
}
```

**Impact:** Eliminates silent alpha-stripping for `dropHintColor`, `buttonHoverBackground`, etc.

---

### Phase 2 — Extend `applyTheme` to cover all toolbar elements

**File:** `GitHubToolbar.java`

#### 2a. Container styling (already partly done — fix border approach)

Switch from CSS inline string to match `DockTabGroup` style:

```java
// Use JavaFX API for container (HBox = GitHubToolbar itself)
setBackground(new Background(new BackgroundFill(
    resolved.headerBackground(), CornerRadii.EMPTY, Insets.EMPTY)));
setBorder(new Border(new BorderStroke(
    resolved.borderColor(), BorderStrokeStyle.SOLID,
    CornerRadii.EMPTY, new BorderWidths(0, 0, resolved.borderWidth(), 0))));
```

This removes the inline `-fx-border-width: 0 0 1 0` shorthand and uses the strongly-typed API.

#### 2b. Buttons — themed inline CSS

All seven buttons (`checkoutButton`, `newBranchButton`, `commitButton`, `rollbackButton`,
`pushButton`, `prButton`, `tokenButton`) need a shared helper:

```java
private void styleButton(Button button, Theme theme) {
    String bg     = toCss(theme.headerBackground(), "#2d2d2d");
    String hoverBg = toCss(theme.buttonHoverBackground(), "#464646");
    String pressedBg = toCss(theme.buttonPressedBackground(), "#5a5a5a");
    String text   = toCss(theme.textColor(), "#c8c8c8");
    String border = toCss(theme.borderColor(), "#3c3c3c");
    double r      = theme.cornerRadius();
    double bw     = theme.borderWidth();

    button.setStyle("""
        -fx-background-color: %s;
        -fx-text-fill: %s;
        -fx-border-color: %s;
        -fx-border-width: %.1f;
        -fx-border-radius: %.1f;
        -fx-background-radius: %.1f;
        -fx-cursor: hand;
        """.formatted(bg, text, border, bw, r, r));

    button.setOnMouseEntered(e ->
        button.setStyle(button.getStyle()
            .replace(bg, hoverBg)));
    button.setOnMouseExited(e ->
        button.setStyle(button.getStyle()
            .replace(hoverBg, bg)));
    button.setOnMousePressed(e ->
        button.setStyle(button.getStyle()
            .replace(hoverBg, pressedBg)));
    button.setOnMouseReleased(e ->
        button.setStyle(button.getStyle()
            .replace(pressedBg, bg)));

    button.setFont(theme.contentFont());
}
```

> **Note on hover implementation:** The string-replace approach above is fragile across
> repeated calls. A cleaner alternative — preferred for phase 2 — is to store the three
> CSS strings in the button's `UserData` as a record `ButtonStyles(normal, hover, pressed)`
> and set directly in the mouse listeners. The exact implementation choice is left to the
> engineer; both are functionally equivalent.

Apply this to every button in `applyTheme`:

```java
for (Button btn : List.of(checkoutButton, newBranchButton, commitButton,
                           rollbackButton, pushButton, prButton, tokenButton)) {
    styleButton(btn, resolved);
}
```

This requires promoting the seven buttons from local variables in the constructor to fields,
mirroring the existing fields for `branchCombo`, `dirtyDot`, `statusLabel`, and `errorLabel`.

#### 2c. Branch ComboBox

```java
branchCombo.setStyle("""
    -fx-background-color: %s;
    -fx-border-color: %s;
    -fx-border-width: %.1f;
    -fx-text-fill: %s;
    -fx-border-radius: %.1f;
    -fx-background-radius: %.1f;
    """.formatted(
        toCss(resolved.headerBackground(), "#2d2d2d"),
        toCss(resolved.borderColor(), "#3c3c3c"),
        resolved.borderWidth(),
        toCss(resolved.textColor(), "#c8c8c8"),
        resolved.cornerRadius(),
        resolved.cornerRadius()));
```

#### 2d. Repo Hyperlink

```java
repoLink.setStyle("""
    -fx-text-fill: %s;
    -fx-border-color: transparent;
    -fx-underline: false;
    """.formatted(toCss(resolved.accentColor(), "#007acc")));
```

Promote `repoLink` to a field for access in `applyTheme`.

#### 2e. Separator labels

Change `createSeparator()` to accept a `Theme` parameter — or keep it parameterless and
store the separator `Label` list as a field so `applyTheme` can restyle them:

```java
private List<Label> separators = new ArrayList<>();

private Node createSeparator() {
    Label separator = new Label("|");
    separators.add(separator);
    return separator;
}

// In applyTheme:
Paint divider = resolved.dividerColor();
for (Label sep : separators) {
    if (divider instanceof Color c) sep.setTextFill(c);
    sep.setOpacity(1.0);   // remove hardcoded opacity
}
```

#### 2f. Error label — extract semantic constant

Replace the hardcoded `#d9534f` with a package-private constant or derive it from `Theme`.
Since `Theme` has no explicit error color, use a private constant:

```java
private static final Color ERROR_COLOR = Color.web("#d9534f");
```

Keep this color fixed (it is semantic, not decorative) but at least make it visible in one
place and easy to change.

#### 2g. Font propagation

```java
statusLabel.setFont(resolved.contentFont());
errorLabel.setFont(resolved.contentFont());
```

#### 2h. ProgressIndicator accent

The `ProgressIndicator` cannot be trivially recolored without CSS. Apply a focused style string:

```java
busyIndicator.setStyle(
    "-fx-accent: " + toCss(resolved.accentColor(), "#007acc") + ";");
```

---

### Phase 3 — Expose initial Theme in convenience constructors

**File:** `GitHubToolbar.java`

Add an optional `Theme` parameter to the two-argument constructor and update factory methods:

```java
public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore) {
    this(context, credentialStore, null);
}

public GitHubToolbar(GitHubRepoContext context, CredentialStore credentialStore, Theme initialTheme) {
    this(buildViewModel(context, credentialStore), initialTheme);
}

public GitHubToolbar(GitHubToolbarViewModel viewModel) {
    this(viewModel, null);
}

public GitHubToolbar(GitHubToolbarViewModel viewModel, Theme initialTheme) {
    // existing construction ...
    applyTheme(initialTheme != null ? initialTheme : Theme.dark());
    viewModel.refresh();
}
```

Update `fromState` to accept optional theme, or leave it as-is (acceptable since
`bindThemeProperty` is called immediately after construction in practice).

---

### Phase 4 — Dialog Theme Support

**Files:** `CommitDialog.java`, `NewBranchDialog.java`, `TokenDialog.java`,
`RollbackDialog.java`, `PullRequestDialog.java`

All five dialogs are standard JavaFX `Dialog<T>` with no theme awareness. There are two
approaches; the simpler one is preferred:

#### Preferred approach: Dialog CSS inline style on `DialogPane`

Add a `Theme` parameter to each dialog constructor and apply a single inline style to the
`DialogPane` and its header/content:

```java
public CommitDialog(Theme theme) {
    // ... existing setup ...
    applyTheme(theme);
}

private void applyTheme(Theme theme) {
    Theme t = theme == null ? Theme.dark() : theme;
    String bg    = toCss(t.background(), "#1e1e1e");
    String hdrBg = toCss(t.headerBackground(), "#2d2d2d");
    String text  = toCss(t.textColor(), "#c8c8c8");
    String border = toCss(t.borderColor(), "#3c3c3c");

    getDialogPane().setStyle("""
        -fx-background-color: %s;
        -fx-border-color: %s;
        -fx-border-width: 1;
        """.formatted(bg, border));
    // Content label and input field colors can be set programmatically
    // since the content VBox/GridPane children are accessible at this point.
}
```

Because dialogs own their scene, inline styles on the `DialogPane` provide the background
but button row styling requires propagation to `.button-bar .button` via a lookup stylesheet
added to the dialog's scene — or by styling each button explicitly after
`getDialogPane().lookupButton(buttonType)`.

**Recommended minimal scope for dialogs (Phase 4):**
- `DialogPane` background and border: use `theme.background()` and `theme.borderColor()`
- Header background: `theme.headerBackground()`
- Label text fill: `theme.textColor()` (applied via `Label.setTextFill` on known labels)
- Input fields (`TextArea`, `TextField`, `PasswordField`): background, border, text-fill
- OK/Cancel buttons: same `styleButton` helper introduced in Phase 2 (after `lookupButton`)

**Caller update:**

`GitHubToolbar` currently instantiates dialogs without a `Theme` parameter.
After this phase, each dialog creation must pass `themeProperty != null ? themeProperty.get() : Theme.dark()`.

---

### Phase 5 — `GitHubToolbarTheme` mapper (optional, deferred)

If future requirements call for more sophisticated theming (e.g., distinct "toolbar accent" vs.
"editor accent"), introduce a `GitHubToolbarTheme` record following the pattern of
`CodeEditorTheme` / `CodeEditorThemeMapper`. This is **not required** for the current review
cycle and should only be done if simple field mapping from `Theme` proves insufficient.

---

## 4. Files to Change

| File | Change type |
|---|---|
| `GitHubToolbar.java` | Extend `applyTheme`, add `toCss`, promote 7 buttons + repoLink to fields, fix separators, font, busyIndicator |
| `CommitDialog.java` | Add `Theme` param, apply themed colors |
| `NewBranchDialog.java` | Add `Theme` param, apply themed colors |
| `TokenDialog.java` | Add `Theme` param, apply themed colors |
| `RollbackDialog.java` | Add `Theme` param, apply themed colors |
| `PullRequestDialog.java` | Add `Theme` param, apply themed colors |

No new files are required. No changes to `Theme.java`, `DockTabGroup.java`,
`CodeEditorTheme.java`, or any module POM are needed.

---

## 5. Visual Consistency Goals

After these changes, the GitHub toolbar should:

1. Match the `DockTabGroup` tab-bar background exactly (same `theme.headerBackground()` paint).
2. Use the same accent color (`theme.accentColor()`) for the repo hyperlink, dirty-dot,
   busy-indicator accent, and button focus ring.
3. Use the same divider color (`theme.dividerColor()`) as the `DockTabGroup` separator regions.
4. Use the same font (`theme.contentFont()`) for all toolbar text labels, matching dock leaf
   content areas.
5. Apply hover/pressed states using `theme.buttonHoverBackground()` and
   `theme.buttonPressedBackground()`, matching the minimized-bar button behavior.
6. Respond correctly to both `Theme.dark()` and `Theme.light()` without any hardcoded color
   leaking through.
7. Dialog windows should not appear as white-background popups on a dark-themed application.

---

## 6. Risks and Constraints

| Risk | Mitigation |
|---|---|
| JavaFX CSS specificity: inline `setStyle` may be overridden by user-agent stylesheet | Use `-fx-background-color` (which overrides Modena) and set `button.setBackground(null)` first if needed |
| `ProgressIndicator` accent via `-fx-accent` may not work for all Modena versions | Accept as best-effort; fallback to opacity adjustment |
| Dialog scene loads after `applyTheme` — `lookupButton` returns null in constructor | Call `getDialogPane().lookupButton(buttonType)` inside `Platform.runLater` or use `setOnShowing` hook |
| Hover listeners added in `applyTheme` accumulate on each theme change | Clear existing event handlers before registering new ones, or use a `UserData`-stored style record instead of nested setStyle calls |

---

## 7. Non-Goals

- No CSS stylesheets are to be created (project constraint: all programmatic, no CSS files).
- No changes to `Theme.java` record structure.
- No changes to `CodeEditorTheme` or `CodeEditorThemeMapper`.
- No changes to any Maven POM.
- No new module dependencies.