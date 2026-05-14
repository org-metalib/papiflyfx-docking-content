# GitHub Toolbar UI Refactor

## Goal

Refactor the GitHub toolbar so the repository and current ref become the primary identity of the control.

Required interaction goals:

- Show the repository name in the toolbar.
- Show the current branch or tag in the toolbar.
- Show a small status dot:
  - green when the working tree is clean
  - red when there are local modifications
- Clicking the repository name opens the repository in the default browser.
- Clicking the branch or tag opens a popup switcher inspired by `idea-branch-popup.png`.

## Problems In The Current Toolbar

- The current layout gives equal visual weight to every button, so repository identity is not the focal point.
- Branch selection is a standard combo box. It feels like a form field, not like a Git ref switcher.
- State is spread across many badges, which adds noise before the user even reads the repository name.
- The clean/dirty state is not communicated in the fastest possible way.
- Advanced branch actions are outside the branch interaction itself, so the mental model is fragmented.

## Recommended Design

Use a compact single-row toolbar built around two primary pills on the left:

1. Repository pill
2. Ref pill

Everything else becomes supporting information or contextual actions.

### Toolbar Layout

```text
[ GitHub ] [ owner/repo ] [ ● github v ] [ Ahead 2 ] [ Behind 1 ]                                  [...]
```

Recommended visual hierarchy:

- `owner/repo` is the first readable element.
- The current ref pill is the second readable element.
- The status dot lives inside the ref pill so the user reads ref + status together.
- Ahead/behind counters stay compact and secondary.
- Repository and ref commands stay behind the lower-frequency actions trigger so the identity area stays dominant.

## Main Elements

### 1. Repository Pill

Content:

- GitHub mark or repo icon
- `owner/repo`

Behavior:

- Entire pill is clickable, not just the text.
- Hover state should make it feel link-like, but still styled as toolbar chrome.
- Click opens `context.remoteUrl()` in the default browser.

Why:

- The repository is the anchor of the whole toolbar.
- A larger click target is better than a thin hyperlink label.

### 2. Ref Pill

Content:

- leading status dot
- branch icon, tag icon, or detached icon
- current ref name
- trailing chevron

Examples:

- `● github v`
- `● main v`
- `● v0.9.0 v`
- `● 4fdbe7 v`

Behavior:

- Click opens the branch/tag popup anchored below the pill.
- Keyboard `Enter`, `Space`, or `Alt+Down` should open it too.
- The pill should look like a switcher, not a text badge.

Status dot rules:

- Green: local clone available and clean
- Red: local clone available and dirty
- Neutral gray: remote-only mode, where working tree status is unavailable

### 3. Secondary Chips

These are optional and should only appear when relevant:

- `Default`
- `Detached`
- `Remote only`
- `Ahead N`
- `Behind N`
- `No token`

Important rule:

- Do not render all possible badges all the time.
- Only show state that changes what the user should notice right now.

### 4. Action Area

Recommended visible action surface:

- `...`

Behavior notes:

- `...` is the only command surface on the toolbar.
- It contains `Update`, `Commit`, `Push`, `Pull Request`, `New Branch`, `Rollback`, `Token`, and future repository actions.
- `Commit` stays disabled on the default branch, as today.
- `Push` and `Pull Request` remain disabled until a token is configured.

Visual direction:

- Keep the lower-frequency actions trigger flatter and less dominant than the repo/ref pills.
- The current design's large segmented groups should be removed.

### 5. Status Slot

Use a compact transient status slot at the far right.

Content rules:

- Show a spinner while a Git command is running.
- Show short success text only after an action completes.
- Show errors as a red inline pill.
- Hide the slot when idle and there is nothing important to say.

Why:

- Status is useful, but it should not permanently compete with repository identity.

## Branch/Tag Popup

The popup should borrow the interaction model from `spec/papiflyfx-docking-github/review1-ui/idea-branch-popup.png`, 
but not copy it literally.

### Popup Structure

```text
+------------------------------------------------------+
| Search branches and tags                             |
|------------------------------------------------------|
| Recent                                               |
|   github                               origin/github |
|   main                                   origin/main |
|------------------------------------------------------|
| Local                                                |
|   github                               origin/github |
|   main                                   origin/main |
|   feature/search                       origin/search |
|------------------------------------------------------|
| Remote                                               |
|   origin                                             |
|     main                                             |
|     github                                           |
|------------------------------------------------------|
| Tags                                                 |
|   v0.9.0                                             |
|   v0.8.0                                             |
+------------------------------------------------------+
```

### Popup Behavior

Top search field:

- Filters branches, remotes, and tags in one place.
- Typing should immediately focus the search field when the popup opens.

Popup size:

- Target width: 360-420 px
- Max height: about 520 px before scrolling

Sections:

- `Recent`: last used refs, max 5
- `Local`: local branches
- `Remote`: grouped by remote name
- `Tags`: annotated and lightweight tags

Per-item behavior:

- Clicking a local branch checks it out.
- Clicking a remote branch either checks out a tracking branch or opens a submenu.
- Clicking a tag checks out detached HEAD at that tag.
- Items with more actions show a trailing chevron and open a side submenu.

Recommended side submenu actions:

- `Checkout`
- `Checkout and Track`
- `Show Diff with Working Tree`
- `Rename...`
- `Delete Local Branch...`

Tag submenu actions:

- `Checkout Tag`
- `Open Commit`
- `Compare with Working Tree`

## State Examples

### Clean local branch

```text
[ owner/repo ] [ green-dot github v ] [ Ahead 2 ] [ Behind 1 ]                                  [...]
```

### Dirty working tree

```text
[ owner/repo ] [ red-dot github v ] [ 3 changed ]                                               [...]
```

### Default branch with clean state

```text
[ owner/repo ] [ green-dot main v ] [ Default ]                                                 [...]
```

### Detached on tag

```text
[ owner/repo ] [ green-dot v0.9.0 v ] [ Detached ]                                              [...]
```

### Remote-only repository

```text
[ owner/repo ] [ gray-dot main v ] [ Remote only ]                                             [...]
```

In remote-only mode, the ref pill should show the remote default branch if available.

## Visual Language

Recommended styling direction:

- One continuous toolbar surface, not a row of boxed islands.
- Repo and ref pills have slightly stronger background than action buttons.
- Rounded corners should match the docking theme language already used by the code search overlays.
- Keep spacing tight and intentional; this should feel closer to IDE chrome than to a form.
- The green/red dot must be small and sharp, not a large badge.

Theme guidance:

- Reuse the GitHub toolbar theme mapper from review 0.
- Derive success, danger, hover, and focus colors from the active docking theme.
- Maintain the same layout in dark and light themes.

## Model Support Needed

The current implementation already supports:

- repository URL
- current branch
- clean/dirty state
- ahead/behind counts
- detached head
- default branch
- remote-only mode

To support the full design cleanly, add:

- current ref type: branch, tag, or detached commit
- tag listing
- recent ref history
- tracking target label for branch items
- popup item model for sections and submenus

## Implementation Notes

Recommended implementation direction:

- Replace the branch combo box with a dedicated clickable ref pill.
- Use `PopupControl` or an equivalent anchored popup instead of a combo box dropdown.
- Keep repo browser navigation on the repository pill.
- Keep the ref popup focused on switching refs and ref-specific submenus only.
- Route repository and branch-management commands through the lower-frequency actions menu.

## Summary

The new toolbar should feel like a Git identity bar first and a command menu second.

The key change is this:

- repository pill for navigation
- ref pill for status + branch/tag switching
- lower-frequency actions menu for all commands
- compact state chips only when relevant
- branch/tag popup as the main control surface for switching refs and related Git actions
