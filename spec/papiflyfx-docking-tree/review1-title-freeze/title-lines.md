# Tree navigation freeze option (folder/title lines)

## Assumption
I assume **"freeze folders to be selected through navigation"** means: certain tree nodes (for example category/title lines)
 should stay visible but **must not become selected/focused via keyboard navigation**.

## Current behavior (why this is needed)
- `TreeInputController` currently selects every visible row it lands on (`focusByIndex(...)` -> `focusItem(...)` -> `selectionModel.selectOnly(item)`).
- There is no selection filter/predicate in `TreeView` or `TreeSelectionModel` for keyboard movement.
- `SamplesApp` works around this by manually reverting category selections in `onNavigationSelectionChanged(...)`, which is app-specific and noisy.

## Minimal implementation path
1. **Add a navigation-selection filter API in `TreeView`**
   - Example shape:
     - `setNavigationSelectable(Predicate<TreeItem<T>> predicate)`  
     - Default: always `true` (backward compatible).
2. **Inject that predicate into `TreeInputController`**
   - Update Up/Down/Home/End/PageUp/PageDown logic to move to the next item that passes the predicate.
   - Keep expand/collapse behavior on Left/Right for frozen folders, but when moving focus to another row, skip non-selectable rows.
   - Ensure bootstrap focus (`ensureFocusedItem`) picks the first selectable row.
3. **Guard edge cases**
   - If all visible rows are frozen, navigation should no-op without clearing valid existing selection.
   - If focus/selection is restored to a frozen row, normalize to nearest selectable row (or keep focus null if none).
4. **Replace app workaround where relevant**
   - `SamplesApp` can set predicate like `item -> !item.getValue().isCategory()` and remove selection-revert code.

## Files likely touched
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/api/TreeView.java`
- `papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/controller/TreeInputController.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java` (optional cleanup/consumption)
- Tests:
  - `papiflyfx-docking-tree/src/test/java/org/metalib/papifly/fx/tree/api/TreeViewFxTest.java`
  - potentially a focused unit test around navigation filtering behavior.

## State/persistence impact
- If frozen behavior is driven by runtime predicate only, no persistence schema change is required.
- If users can configure frozen folders by path and expect restoration, then `TreeViewStateData` / `TreeStateCodec` would need an added `frozenPaths` field.

## Effort/risk
- **Core feature (API + keyboard navigation + tests):** small-to-medium change.
- **Risk level:** low, if default behavior stays unchanged and predicate defaults to "all selectable".
- **Main regression risk:** navigation loops/anchors in multi-select mode; this is manageable with a few targeted tests.
