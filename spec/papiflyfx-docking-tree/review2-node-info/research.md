# Collapsible node info in `papiflyfx-docking-tree` (review2)

## What exists today

`TreeView` is canvas-virtualized and row-based:

- Rendering is done by `TreeViewport` + render passes (`TreeBackgroundPass`, `TreeContentPass`, etc.).
- Visible rows come from `FlattenedTree` as `FlattenedRow(item, depth)`.
- Geometry assumes **fixed row height** from `TreeViewTheme.rowHeight`.
- Hit-testing, scrolling, keyboard navigation, editing, and drag/drop all depend on row index math based on fixed height.
- Custom painting is available (`TreeCellRenderer`), but rich JavaFX nodes are not embedded in the canvas.

This means: a true collapsible “info section under a node” is not just a renderer tweak; it affects model, layout, hit-testing, and interaction layers.

---

## Requirement implications

You asked for collapsible node info that can display:

- rich text (HTML-like),
- table,
- card/form.

Those are best represented as JavaFX `Node`s (e.g., `WebView`, `TableView`, `GridPane/VBox`), not pure canvas drawing.  
So implementation needs a **Node host layer** in addition to the existing canvas row rendering.

---

## Feasible implementation options

### Option A (lowest risk): external details panel (not inline)

Show node info in a collapsible panel below or beside the tree, tied to selected/focused node.

**Pros**
- Minimal impact to current virtualization and controllers.
- Full support for rich JavaFX content immediately.
- Very fast to deliver.

**Cons**
- Not an inline section under each row.
- Usually one node’s info shown at a time.

---

### Option B (full inline behavior): virtualized info rows + Node overlay host

Insert a collapsible info “row” after an item row and render its content as JavaFX nodes in a managed overlay layer.

**Pros**
- Matches expected “collapsible section under node” behavior.
- Supports HTML/table/form content properly.

**Cons**
- Medium/high refactor: fixed-row assumptions are widespread.
- Must keep canvas virtualization + node overlay in sync.

If inline behavior is required, this is the correct architecture.

---

## What changes are required for inline implementation

### 1) New node-info model and API

Add dedicated state separate from `TreeItem`:

- `TreeNodeInfoModel<T>` with expanded info items (`Set<TreeItem<T>>`) and mode:
  - `SINGLE` (recommended default),
  - `MULTIPLE`.
- Provider API for rich content:

```java
public interface TreeNodeInfoProvider<T> {
    Node createContent(TreeItem<T> item);
    default double preferredHeight(TreeItem<T> item, double availableWidth) { return 160.0; }
    default void disposeContent(TreeItem<T> item, Node content) {}
}
```

Expose in `TreeView`:

- `setNodeInfoProvider(...)`
- `toggleNodeInfo(TreeItem<T>)`
- `collapseNodeInfo(TreeItem<T>)`
- `setNodeInfoMode(...)`

### 2) Flattened data must represent row kinds

Current `FlattenedRow` only describes tree items.  
Inline info needs row kind support, for example:

- item row,
- info row (linked to parent item, with dynamic height).

This likely means replacing `FlattenedRow(item, depth)` with a richer row record/sealed hierarchy and updating `FlattenedTree`.

### 3) Replace fixed row-height math in `TreeViewport`

Today these methods assume `index * rowHeight`:

- `rowIndexAtY`
- `hitTest`
- `rowBounds`
- `ensureItemVisible`
- `buildVisibleRows`
- max scroll/content height calculations

For info rows, use cumulative row offsets (prefix sums) and binary search for y->row lookup.

### 4) Add Node overlay layer for rich content

Canvas can still paint item rows, disclosure, lines, and selection backgrounds.  
Info content should be hosted in a dedicated overlay `Pane` aligned to visible info rows.

Expected behavior:

- only mount nodes for visible info rows (virtualized),
- recycle/cache nodes per item where possible,
- reposition on scroll/layout,
- hide/unmount when item or info is collapsed.

### 5) Input/hit-testing updates

`TreeViewport.HitInfo` currently has only `disclosureHit`.  
Inline info needs additional hit metadata (e.g., info toggle zone) and controller updates:

- `TreePointerController` (toggle info, avoid conflicts with selection),
- `TreeInputController` (platform keyboard toggle shortcut, e.g., `⌘I` on macOS and `ALT+ENTER` on Windows/Linux),
- drag/drop should ignore or constrain drops on info rows,
- inline edit should target item rows only.

### 6) Persistence/state updates

Current state persists expanded/selected/focused paths + scroll offsets only.

Add info state to:

- `TreeViewStateData`
- `TreeStateCodec`
- `TreeView.captureStateData()/applyState(...)`
- bump `TreeViewStateAdapter.VERSION` (with backward-compatible decode).

Suggested persisted field: `expandedInfoPaths`.

### 7) Search behavior alignment

When search reveals a match, parent expansion already happens.  
Decide and codify whether info should:

- stay unchanged (recommended default), or
- auto-open for matched node.

---

## Content-type specifics

### Rich text / HTML

- If true HTML rendering is needed, use `WebView` (requires `javafx-web` in consuming module).
- Keep tree module generic by accepting `Node` from provider instead of hard-depending on `WebView`.

### Table

- Use `TableView<?>` in info provider.
- Constrain height (fixed or preferred) to keep virtualization predictable.

### Card / form

- Use `VBox`/`GridPane` with controls.
- Decide interaction policy (editable vs read-only) and focus behavior when collapsing.

---

## Files directly impacted (inline approach)

- `api/TreeView.java`
- `model/FlattenedTree.java`, `model/FlattenedRow.java`
- `render/TreeViewport.java`, `render/TreeRenderRow.java`, `render/TreeRenderContext.java`
- `controller/TreePointerController.java`, `controller/TreeInputController.java`, `controller/TreeEditController.java`, `controller/TreeDragDropController.java`
- `util/TreeViewStateData.java`, `util/TreeStateCodec.java`
- `api/TreeViewStateAdapter.java`
- tests in `src/test/java/...`

---

## Recommended delivery path

1. **Phase 1 (safe MVP):** external collapsible details panel in `TreeView` + `TreeNodeInfoProvider`.
2. **Phase 2 (true inline):** introduce row kinds + variable row metrics + node overlay virtualization.
3. **Phase 3:** persistence, keyboard polish, and interaction hardening (DnD/edit/search coexistence).

This phased path keeps risk controlled while still enabling rich text/table/card content early.

---

## Test coverage to add

- Model tests for mixed row kinds and expansion/collapse behavior.
- Viewport tests for variable-height row mapping, visibility, and scroll bounds.
- FX tests for:
  - toggling info open/closed,
  - preserving selection/focus behavior,
  - search + info coexistence,
  - drag/drop and edit interactions with info rows,
  - state save/restore including info expansion.

---

## Bottom line

For rich content (HTML/table/form), a canvas-only change is insufficient.  
If inline sections are required, implement virtualized info rows plus a synchronized Node overlay layer; if faster delivery is preferred, start with an external collapsible details panel and reuse the same provider API.
