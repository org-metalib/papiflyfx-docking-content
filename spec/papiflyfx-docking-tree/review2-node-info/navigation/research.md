# Research: Feature-Flagged Node-Info Focus Toggle (Keyboard and Mouse)

## Scope

Goal: make node-info toggling in `papiflyfx-docking-tree` controllable by a feature flag so it can be enabled via:

- keyboard shortcut only,
- mouse click only,
- both,
- or disabled.

Additionally, clarify focus behavior when toggle happens.

## Current behavior in code

1. Keyboard toggle already exists.
   - `TreeInputController.handleKeyPressed(...)` checks `isToggleFocusedInfoShortcut(...)` and calls `toggleFocusedInfo()`.
   - Shortcuts are platform-specific:
     - macOS: `Meta+I`
     - Windows/Linux: `Alt+Enter`
   - Code: `TreeInputController`.

2. Mouse toggle already exists.
   - `TreePointerController.handleMousePressed(...)` checks `hitInfo.infoToggleHit()` and calls `nodeInfoModel.toggle(item)`.
   - Info-toggle hit zone is computed in `TreeViewport.hitTest(...)`.
   - Toggle icon is rendered by `TreeContentPass.drawInfoToggle(...)`.

3. Focus behavior is asymmetric today.
   - Keyboard path toggles focused item by design.
   - Mouse toggle path toggles the clicked item but does not set selection/focus on that item.

4. There is no feature-flag abstraction yet.
   - No reusable feature-flag framework in tree module.
   - Toggle behavior is currently always active when `nodeInfoProvider != null`.

## What is required to implement feature-flagged behavior

### 1) Add explicit navigation/toggle flag model

Recommended API shape:

```java
public enum TreeNodeInfoToggleMode {
    DISABLED,
    KEYBOARD_ONLY,
    MOUSE_ONLY,
    KEYBOARD_AND_MOUSE
}
```

Optional focus policy (recommended):

```java
public enum TreeNodeInfoFocusPolicy {
    KEEP_CURRENT_FOCUS,
    FOCUS_TOGGLED_ITEM
}
```

Why: an enum mode is safer than multiple booleans and gives a clear test matrix.

### 2) Expose it on `TreeView`

Add `ObjectProperty<TreeNodeInfoToggleMode>` (and optional focus policy property) to `TreeView` and wire it into controllers.

Suggested defaults:

- `TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE`
- `TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM` (recommended for consistent UX), or keep old behavior if regression-free rollout is preferred.

### 3) Gate keyboard path in `TreeInputController`

Current path:

- `isToggleFocusedInfoShortcut(event)` -> `toggleFocusedInfo()`.

Required change:

- only allow this branch when mode includes keyboard.

No other key navigation behavior needs change.

### 4) Gate mouse path in `TreePointerController`

Current path:

- `hitInfo.infoToggleHit()` -> `nodeInfoModel.toggle(item)`.

Required change:

- only execute this branch when mode includes mouse.
- apply focus policy here:
  - if `FOCUS_TOGGLED_ITEM`, set selection/focus/anchor to the clicked item before or after toggle.
  - call `viewport.ensureItemVisible(item)` and `markDirty()` as done today.

### 5) Keep render/hit-test consistent with the flag

If mouse toggling is disabled, two options exist:

1. Keep icon visible but inactive.
2. Hide icon and make hit-test return `infoToggleHit=false`.

Recommended: hide when mouse is disabled to avoid dead UI affordances.

This touches:

- `TreeRenderRow` (`infoAvailable` should account for mode),
- `TreeContentPass.drawInfoToggle(...)`,
- `TreeViewport.hitTest(...)` info-toggle detection.

### 6) Add/adjust tests

Existing coverage already validates keyboard and mouse toggles in `TreeViewFxTest`. Add feature-flag matrix tests:

- keyboard-only: keyboard toggles, mouse does not.
- mouse-only: mouse toggles, keyboard does not.
- disabled: neither toggles.
- both: both toggles.
- focus policy:
  - mouse toggle updates focused item when enabled,
  - mouse toggle keeps prior focus when configured.

Also keep regression checks for:

- search overlay shortcut handling order (`TreeView.installHandlers`),
- state save/restore of expanded info rows.

### 7) Persistence decision

`TreeViewStateData` currently stores tree state (expanded paths, selected/focused paths, scrolls), not behavior configuration.

Recommendation:

- do not persist feature flags in `TreeViewStateData`;
- treat flags as runtime component configuration.

Persist only if product requirements explicitly need per-session behavior recall.

## Implementation impact summary

Main files impacted:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `.../controller/TreeInputController.java`
- `.../controller/TreePointerController.java`
- `.../render/TreeViewport.java`
- `.../render/TreeRenderRow.java`
- `.../render/TreeContentPass.java`
- `papiflyfx-docking-tree/src/test/java/org/metalib/papifly/fx/tree/api/TreeViewFxTest.java`

Complexity: low-to-medium.

- Logic changes are localized.
- Highest risk is UX inconsistency if rendering/hit-test/controller gating are not updated together.

## Bottom line

The tree already supports node-info toggle by keyboard and mouse. Implementing a feature-flagged version is primarily a control-plane change: add an explicit toggle mode + focus policy, gate both input paths, and keep icon/hit-test behavior aligned with the active mode.

## Post-Implementation Addendum (2026-03-02)

1. Samples integration proved useful for discoverability.
   - `TreeViewNodeInfoSample` now exposes runtime controls for:
     - `TreeNodeInfoMode`
     - `TreeNodeInfoToggleMode`
     - `TreeNodeInfoFocusPolicy`
   - This makes keyboard/mouse gating and focus policy behavior directly verifiable by users.

2. Visual behavior gap discovered after rollout.
   - Inline info rows were not highlighted even when the owning item was selected.
   - Root cause: `TreeBackgroundPass` applied selected/hover paint only for `ITEM` rows.
   - Initial resolution (superseded): apply selected/hover paint by row owner item across both `ITEM` and `INFO` rows.

3. Visual policy refinement requested.
   - Requirement: only border should be highlighted for node-info visible area.
   - Final behavior:
     - `ITEM` row keeps selected fill behavior.
     - `INFO` row keeps normal row background.
     - `INFO` row shows highlight as border only when owner item is selected.
   - Implementation: `TreeBackgroundPass` now limits selected/hover fill to item rows and draws border for selected info rows.

4. Test expectation updated to match intended UX.
   - Replaced fill-based expectation with border-based expectation in `TreeViewFxTest.selectedItemUsesBorderHighlightForInlineInfoRow`.

5. Inline node-info content alignment refinement.
   - Requirement: node-info visible area should respect tree indentation.
   - Final behavior:
     - Inline node-info host positions each info node with left inset derived from row depth and theme metrics.
     - Indentation also respects horizontal scroll offset.
   - Implementation touchpoints:
     - `TreeInlineInfoHost.sync(...)` (added indentation-aware placement inputs)
     - `TreeView.syncInlineInfoHost()` (passes indent/icon/scroll metrics)

6. Connector rendering refinement.
   - Requirement: last connector line should not look like continuation line.
   - Final behavior:
     - For non-last siblings, vertical connector spans full row height.
     - For last siblings, vertical connector stops at branch center (`└`-like endpoint).
   - Implementation:
     - `TreeConnectingLinesPass` now checks sibling position and trims vertical segment for the last child.
