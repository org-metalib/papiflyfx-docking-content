# Implementation Plan: Feature-Flagged Node-Info Toggle Navigation

## Objective

Implement configurable node-info toggle navigation for `TreeView` so inline node info can be toggled by:

1. Keyboard only
2. Mouse only
3. Keyboard and mouse
4. Disabled

Also implement explicit focus behavior for mouse-triggered toggles.

## Scope

In scope:

- Add toggle-mode and focus-policy models
- Wire runtime configuration through `TreeView`
- Gate keyboard path in `TreeInputController`
- Gate mouse path in `TreePointerController`
- Align draw/hit-test behavior with active mode
- Add unit/FX coverage for behavior matrix

Out of scope:

- Persisting feature-flag settings in tree state
- Replacing current platform key mapping (`Meta+I` on macOS, `Alt+Enter` on Windows/Linux)
- Changing search, edit, and drag-drop semantics unrelated to info toggle mode

## Progress Snapshot

- [x] Phase 1: Add feature-flag models
- [x] Phase 2: Expose mode/policy on `TreeView`
- [x] Phase 3: Gate keyboard behavior
- [x] Phase 4: Gate mouse behavior + focus policy
- [x] Phase 5: Align render and hit-test with mode
- [x] Phase 6: Add tests for mode matrix and focus policy
- [x] Phase 7: Validate and polish

## Target Behavior

Mode matrix:

| Toggle mode | Keyboard shortcut | Mouse toggle icon click |
| --- | --- | --- |
| `DISABLED` | no-op | no-op |
| `KEYBOARD_ONLY` | enabled | disabled |
| `MOUSE_ONLY` | disabled | enabled |
| `KEYBOARD_AND_MOUSE` | enabled | enabled |

Focus policy for mouse toggles:

| Focus policy | On mouse toggle |
| --- | --- |
| `KEEP_CURRENT_FOCUS` | preserve current focused/selected item |
| `FOCUS_TOGGLED_ITEM` | focus/select/anchor the toggled item |

## Detailed Design

### Phase 1: Add Feature-Flag Models

Files to add:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoToggleMode.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoFocusPolicy.java`

Tasks:

- [x] Add toggle mode enum with helper methods:
  - `allowsKeyboard()`
  - `allowsMouse()`
- [x] Add focus policy enum:
  - `KEEP_CURRENT_FOCUS`
  - `FOCUS_TOGGLED_ITEM`

Snippet:

```java
package org.metalib.papifly.fx.tree.model;

public enum TreeNodeInfoToggleMode {
    DISABLED,
    KEYBOARD_ONLY,
    MOUSE_ONLY,
    KEYBOARD_AND_MOUSE;

    public boolean allowsKeyboard() {
        return this == KEYBOARD_ONLY || this == KEYBOARD_AND_MOUSE;
    }

    public boolean allowsMouse() {
        return this == MOUSE_ONLY || this == KEYBOARD_AND_MOUSE;
    }
}
```

```java
package org.metalib.papifly.fx.tree.model;

public enum TreeNodeInfoFocusPolicy {
    KEEP_CURRENT_FOCUS,
    FOCUS_TOGGLED_ITEM
}
```

### Phase 2: Expose Runtime Configuration on `TreeView`

Files to update:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`

Tasks:

- [x] Add properties:
  - `ObjectProperty<TreeNodeInfoToggleMode> nodeInfoToggleMode`
  - `ObjectProperty<TreeNodeInfoFocusPolicy> nodeInfoFocusPolicy`
- [x] Add getters/setters/property accessors
- [x] Set defaults in constructor:
  - `KEYBOARD_AND_MOUSE`
  - `FOCUS_TOGGLED_ITEM`
- [x] Wire these values into input/pointer controllers during construction
- [x] Add listeners that trigger `viewport.markDirty()` when mode changes

Snippet:

```java
private final ObjectProperty<TreeNodeInfoToggleMode> nodeInfoToggleMode =
    new SimpleObjectProperty<>(this, "nodeInfoToggleMode", TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE);
private final ObjectProperty<TreeNodeInfoFocusPolicy> nodeInfoFocusPolicy =
    new SimpleObjectProperty<>(this, "nodeInfoFocusPolicy", TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM);

public TreeNodeInfoToggleMode getNodeInfoToggleMode() {
    return nodeInfoToggleMode.get();
}

public void setNodeInfoToggleMode(TreeNodeInfoToggleMode mode) {
    nodeInfoToggleMode.set(mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode);
}

public ObjectProperty<TreeNodeInfoToggleMode> nodeInfoToggleModeProperty() {
    return nodeInfoToggleMode;
}
```

Wiring snippet:

```java
inputController.setNodeInfoToggleModeSupplier(this::getNodeInfoToggleMode);
pointerController.setNodeInfoToggleModeSupplier(this::getNodeInfoToggleMode);
pointerController.setNodeInfoFocusPolicySupplier(this::getNodeInfoFocusPolicy);

nodeInfoToggleMode.addListener((obs, oldMode, newMode) -> viewport.markDirty());
```

### Phase 3: Gate Keyboard Toggle Path

Files to update:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreeInputController.java`

Tasks:

- [x] Inject toggle mode supplier (or property reference) into controller
- [x] Guard `isToggleFocusedInfoShortcut` path by `mode.allowsKeyboard()`
- [x] Keep existing key mapping and existing non-toggle key handling unchanged

Proposed constructor extension:

```java
private Supplier<TreeNodeInfoToggleMode> nodeInfoToggleMode = () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE;

public void setNodeInfoToggleModeSupplier(Supplier<TreeNodeInfoToggleMode> supplier) {
    this.nodeInfoToggleMode = supplier == null
        ? () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE
        : supplier;
}
```

Key gate snippet:

```java
if (isToggleFocusedInfoShortcut(event) && nodeInfoToggleMode.get().allowsKeyboard()) {
    return consume(event, toggleFocusedInfo());
}
```

### Phase 4: Gate Mouse Toggle Path and Apply Focus Policy

Files to update:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreePointerController.java`

Tasks:

- [x] Inject toggle mode supplier
- [x] Inject focus policy supplier
- [x] Gate `hitInfo.infoToggleHit()` branch by `mode.allowsMouse()`
- [x] Apply focus policy when mouse toggle executes

Focus behavior:

- `KEEP_CURRENT_FOCUS`: toggle only
- `FOCUS_TOGGLED_ITEM`: select/focus/anchor toggled item, then toggle

Snippet:

```java
if (hitInfo.infoToggleHit() && nodeInfoToggleMode.get().allowsMouse()) {
    if (nodeInfoFocusPolicy.get() == TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM) {
        selectionModel.selectOnly(item);
        selectionModel.setFocusedItem(item);
        selectionModel.setAnchorItem(item);
    }
    nodeInfoModel.toggle(item);
    viewport.ensureItemVisible(item);
    viewport.markDirty();
    event.consume();
    return true;
}
```

### Phase 5: Align Render and Hit-Test with Active Mode

Files to update:

- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeViewport.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeRenderRow.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/render/TreeContentPass.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`

Tasks:

- [x] Propagate toggle-mode visibility into render rows
- [x] Only draw info-toggle icon when mouse toggles are enabled
- [x] Only return `infoToggleHit=true` in `hitTest` when mouse toggles are enabled

Recommended wiring:

1. Add viewport field and setter:
   - `boolean nodeInfoMouseToggleEnabled`
2. Set from `TreeView` when mode changes
3. Use flag in both:
   - `TreeViewport.hitTest(...)`
   - `TreeViewport.buildVisibleRows(...)` for `infoAvailable`

Snippet:

```java
private boolean nodeInfoMouseToggleEnabled = true;

public void setNodeInfoMouseToggleEnabled(boolean enabled) {
    if (nodeInfoMouseToggleEnabled == enabled) {
        return;
    }
    nodeInfoMouseToggleEnabled = enabled;
    markDirty();
}
```

Hit-test gate:

```java
boolean infoToggleHit = flattenedRow.isItemRow()
    && nodeInfoMouseToggleEnabled
    && flattenedTree.getNodeInfoProvider() != null
    && localX >= infoToggleX()
    && localX <= infoToggleX() + INFO_TOGGLE_SIZE;
```

Visible row gate:

```java
boolean infoAvailable = isItemRow
    && nodeInfoMouseToggleEnabled
    && flattenedTree.getNodeInfoProvider() != null;
```

### Phase 6: Test Plan

Files to update:

- `papiflyfx-docking-tree/src/test/java/org/metalib/papifly/fx/tree/api/TreeViewFxTest.java`
- Optional targeted unit tests:
  - `.../controller/TreeInputControllerTest.java` (new)
  - `.../controller/TreePointerControllerTest.java` (new)

FX scenarios to add:

- [x] `disabledModePreventsKeyboardAndMouseToggle`
- [x] `keyboardOnlyModeAllowsKeyboardButBlocksMouseToggle`
- [x] `mouseOnlyModeAllowsMouseButBlocksKeyboardToggle`
- [x] `bothModeAllowsKeyboardAndMouseToggle`
- [x] `mouseToggleFocusPolicyKeepsCurrentFocus`
- [x] `mouseToggleFocusPolicyFocusesToggledItem`
- [x] `mouseDisabledHidesInfoToggleAffordanceAndHitZone`

Snippet (pattern):

```java
runOnFx(() -> {
    treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
    treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.KEYBOARD_ONLY);
    treeView.requestFocus();
    treeView.fireEvent(toggleInfoShortcutEvent());
});
assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(target)));
```

Mouse disabled assertion sketch:

```java
Bounds rowBounds = callOnFx(() -> treeView.getViewport().rowBounds(0));
double toggleX = ...; // existing helper pattern from pointer toggle test
double toggleY = rowBounds.getMinY() + rowBounds.getHeight() * 0.5;
runOnFx(() -> treeView.getViewport().fireEvent(mousePressed(toggleX, toggleY)));
assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(target)));
```

### Phase 7: Validation and Documentation Updates

Files to update:

- `spec/papiflyfx-docking-tree/review2-node-info/navigation/progress.md` (if implementation starts later)
- `spec/papiflyfx-docking-tree/review2-node-info/navigation/README.md` (optional status line)

Tasks:

- [x] Run module tests:
  - `./mvnw -pl papiflyfx-docking-tree test -Dtestfx.headless=true`
- [x] Run compile pass:
  - `./mvnw -pl papiflyfx-docking-tree -am -DskipTests compile`
- [x] Confirm no regressions in existing node-info tests
- [x] Update progress artifacts during implementation

## File-Level Change Map

Add:

- `src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoToggleMode.java`
- `src/main/java/org/metalib/papifly/fx/tree/model/TreeNodeInfoFocusPolicy.java`

Modify:

- `src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `src/main/java/org/metalib/papifly/fx/tree/controller/TreeInputController.java`
- `src/main/java/org/metalib/papifly/fx/tree/controller/TreePointerController.java`
- `src/main/java/org/metalib/papifly/fx/tree/render/TreeViewport.java`
- `src/main/java/org/metalib/papifly/fx/tree/render/TreeRenderRow.java`
- `src/main/java/org/metalib/papifly/fx/tree/render/TreeContentPass.java`
- `src/test/java/org/metalib/papifly/fx/tree/api/TreeViewFxTest.java`

## Risks and Mitigations

Risk: mode gating in controllers but not in hit-test/render creates clickable dead area.
Mitigation: gate all three layers together (`TreeContentPass`, `TreeViewport.hitTest`, `TreePointerController`).

Risk: focus policy changes unintentionally alter selection semantics.
Mitigation: add explicit FX tests for both focus policies.

Risk: search/edit keyboard handling order may be impacted by new keyboard gating.
Mitigation: keep `TreeView.installHandlers` ordering untouched and rerun existing search tests.

## Acceptance Criteria

1. `TreeView` exposes runtime mode and focus policy properties.
2. Keyboard and mouse toggles obey the configured mode matrix.
3. Mouse toggle focus behavior obeys policy.
4. When mouse toggle is disabled, toggle affordance is not interactive (and not rendered per plan).
5. Existing node-info persistence and search-related behavior still passes.
6. New and existing tests in `papiflyfx-docking-tree` pass headless.

## Execution Sequence

1. Add new enums (`TreeNodeInfoToggleMode`, `TreeNodeInfoFocusPolicy`).
2. Wire `TreeView` properties and defaults.
3. Add supplier-based config injection in controllers.
4. Gate keyboard toggle path.
5. Gate mouse toggle path and apply focus policy.
6. Propagate mouse-toggle-enabled state into viewport rendering/hit-test.
7. Add tests for mode matrix and focus policy.
8. Run compile/tests and finalize docs.

Rationale for sequence:

- Steps 1-3 establish compile-time plumbing.
- Steps 4-6 implement runtime behavior.
- Step 7 captures regressions only after behavior is complete.

## Class-by-Class Patch Checklist

### `TreeView`

- [x] Add two properties and accessors:
  - `nodeInfoToggleMode`
  - `nodeInfoFocusPolicy`
- [x] Initialize defaults in constructor.
- [x] Wire suppliers into `inputController` and `pointerController`.
- [x] Apply viewport mouse-toggle visibility on startup and mode changes.
- [x] Keep all existing search/edit wiring unchanged.

Snippet:

```java
private void applyNodeInfoToggleModeToViewport(TreeNodeInfoToggleMode mode) {
    TreeNodeInfoToggleMode safeMode = mode == null
        ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE
        : mode;
    viewport.setNodeInfoMouseToggleEnabled(safeMode.allowsMouse());
}
```

```java
nodeInfoToggleMode.addListener((obs, oldMode, newMode) -> {
    applyNodeInfoToggleModeToViewport(newMode);
    viewport.markDirty();
});
```

### `TreeInputController`

- [x] Add supplier field with safe default.
- [x] Add setter for supplier.
- [x] Guard shortcut handling with `allowsKeyboard()`.
- [x] Do not modify non-info key branches.

### `TreePointerController`

- [x] Add mode supplier and focus-policy supplier with safe defaults.
- [x] Guard `infoToggleHit` branch with `allowsMouse()`.
- [x] Apply focus policy branch before toggling.
- [x] Keep existing selection, disclosure, edit, and scroll behavior unchanged.

### `TreeViewport`

- [x] Add `nodeInfoMouseToggleEnabled` field and setter.
- [x] Use it in `hitTest` for `infoToggleHit`.
- [x] Use it in `buildVisibleRows` for `infoAvailable`.

### `TreeRenderRow`

- [x] No structural change required if `infoAvailable` is already passed from viewport.
- [x] Confirm call sites compile and carry gated value.

### `TreeContentPass`

- [x] No code path change required if `infoAvailable` is already false when mouse disabled.
- [x] Keep draw logic unchanged except honoring `row.infoAvailable()`.

## Test Specification (Detailed)

### T1: Disabled mode blocks both input routes

Setup:

- Tree with one visible item and node info provider.
- `setNodeInfoToggleMode(DISABLED)`.

Assertions:

- Keyboard shortcut does not expand info.
- Mouse click on toggle zone does not expand info.

### T2: Keyboard-only mode

Setup:

- `setNodeInfoToggleMode(KEYBOARD_ONLY)`.

Assertions:

- Shortcut toggles info open/close.
- Mouse click in toggle zone does nothing.

### T3: Mouse-only mode

Setup:

- `setNodeInfoToggleMode(MOUSE_ONLY)`.

Assertions:

- Mouse click toggles info open/close.
- Shortcut does nothing.

### T4: Keyboard-and-mouse mode

Setup:

- `setNodeInfoToggleMode(KEYBOARD_AND_MOUSE)`.

Assertions:

- Both interaction types toggle correctly.

### T5: Focus policy keep current focus

Setup:

- Two items.
- Focus item A.
- `setNodeInfoFocusPolicy(KEEP_CURRENT_FOCUS)`.
- Click info toggle for item B.

Assertions:

- Item B info toggles.
- Focus remains on item A.

### T6: Focus policy focus toggled item

Setup:

- Two items.
- Focus item A.
- `setNodeInfoFocusPolicy(FOCUS_TOGGLED_ITEM)`.
- Click info toggle for item B.

Assertions:

- Item B info toggles.
- Focus becomes item B.
- Selected item is item B.

### T7: Mouse-disabled affordance hidden

Setup:

- `setNodeInfoToggleMode(KEYBOARD_ONLY)` or `DISABLED`.

Assertions:

- Render row reports `infoAvailable=false` (indirectly by no icon behavior).
- Clicking former icon zone never triggers toggle.

## Verification Commands

Run from repository root:

```bash
./mvnw -pl papiflyfx-docking-tree -am -DskipTests compile
./mvnw -pl papiflyfx-docking-tree test -Dtestfx.headless=true
```

Focused test run during development:

```bash
./mvnw -pl papiflyfx-docking-tree -Dtest=TreeViewFxTest test -Dtestfx.headless=true
```

## Definition of Done

- All phases in this plan are checked complete.
- New enums and `TreeView` properties are in place.
- Controllers are mode-aware and focus-policy-aware.
- Viewport draw/hit-test affordance is mode-aware.
- Mode matrix and focus policy tests pass.
- Existing tree node-info and search tests still pass.

## Post-Implementation Follow-Ups

### F1: SamplesApp integration

- [x] Add node-info navigation controls to `TreeViewNodeInfoSample`.
- [x] Wire controls to:
  - `setNodeInfoMode(...)`
  - `setNodeInfoToggleMode(...)`
  - `setNodeInfoFocusPolicy(...)`
- [x] Add inline hint text for keyboard and mouse behavior.

### F2: Inline node-info highlight parity

- [x] Apply border-only highlight for selected info rows (no selected fill).
- [x] Keep item-row selected fill behavior unchanged.
- [x] Keep focused border on item rows and add selected border for info rows.
- [x] Update FX test expectation to verify border-only highlight.

### F3: Follow-up verification

- [x] `./mvnw -pl papiflyfx-docking-tree -am -DskipTests compile`
- [x] `./mvnw -pl papiflyfx-docking-tree -am -Dtest=TreeViewFxTest -Dsurefire.failIfNoSpecifiedTests=false test -Dtestfx.headless=true`
- [x] `./mvnw -pl papiflyfx-docking-samples -am -DskipTests compile`
- [x] `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test -Dtestfx.headless=true`

### F4: Inline node-info indentation

- [x] Indent inline info content based on tree depth.
- [x] Keep indentation stable with horizontal scrolling.
- [x] Add FX test to verify info content is offset from left edge.

### F5: Last-child connector shape

- [x] Stop last child vertical connector at branch center.
- [x] Keep non-last child connector behavior unchanged.
- [x] Add FX regression test to verify non-continuation rendering for last child.
