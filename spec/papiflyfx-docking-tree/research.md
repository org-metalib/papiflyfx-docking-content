# PapiflyFX Docking Tree — Research Document

## 1. Executive Summary

This document captures research findings for designing and implementing a new `papiflyfx-docking-tree` module — a canvas-based, virtualized `TreeView` component that integrates with the PapiflyFX docking framework. The component must follow the same architectural patterns as the existing `papiflyfx-docking-code` module: programmatic JavaFX (no FXML, no CSS), canvas-based rendering, dirty-bit invalidation, and integration via `ContentFactory`/`ContentStateAdapter`.

---

## 2. Codebase Architecture Analysis

### 2.1 Module Dependency Pattern

```
api ← docks ← code (test only) ← samples
                ↑
          tree (new, same pattern as code)
```

The `papiflyfx-docking-code` module sets the precedent:
- **Compile dependency** on `papiflyfx-docking-api` for interfaces (`DisposableContent`, `ContentFactory`, `ContentStateAdapter`, `Theme`, `LeafContentData`)
- **Test-only dependency** on `papiflyfx-docking-docks` for integration testing with `DockManager`
- The new `papiflyfx-docking-tree` module should follow the identical pattern

### 2.2 Canvas-Based Rendering Architecture (from CodeEditor)

The code editor uses a `Region`-owns-`Canvas` pattern, **not** extending `Canvas` directly. Key design:

- **Viewport extends Region**: Owns a `Canvas` child. Resize logic lives in `layoutChildren()`.
- **Dirty-bit hierarchy**: Three levels — `dirty` (global flag), `fullRedrawRequired` (entire canvas), `dirtyLines` (per-line `BitSet`). Every mutation calls `requestLayout()`, which triggers `layoutChildren()` → `redraw()`.
- **RenderPass pipeline**: Ordered list of stateless passes (`BackgroundPass`, `SearchPass`, `SelectionPass`, `TextPass`, `CaretPass`, `ScrollbarPass`). Each receives an immutable `RenderContext` record.
- **RenderLine**: Per-visual-row record built fresh each frame. Supports word-wrap where one logical line maps to multiple visual rows.
- **GlyphCache**: Off-screen `Text` node for font metric measurement. Metrics computed once on font change, read many times during rendering.

### 2.3 Theme Integration

All styling is programmatic — no CSS. The `Theme` record (21 fields) carries colors, fonts, and dimensions. Components bind via:

```java
component.bindThemeProperty(dockManager.themeProperty());
```

Theme changes trigger a listener that maps `Theme` → component-specific theme (e.g., `CodeEditorTheme`), then calls `setTheme()` on sub-components (viewport, gutter, overlays), which mark themselves dirty.

### 2.4 ContentFactory / ContentStateAdapter Pattern

For session persistence and restore:

1. `ContentFactory.create(factoryId)` — recreates the component from a string ID
2. `ContentStateAdapter` — saves/restores component state as `Map<String, Object>` (no external JSON library)
3. Registration order matters: register `ContentStateAdapter` **before** setting the `ContentFactory`
4. ServiceLoader discovery via `META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter`

### 2.5 Document Model Patterns (from CodeEditor)

The editor's `Document` class separates concerns:
- `TextSource` — raw character storage with line-ending normalization
- `LineIndex` — maps offsets ↔ (line, column) with incremental updates on edits
- Command pattern for undo/redo (`EditCommand`, `CompoundEdit`)
- `CopyOnWriteArrayList<DocumentChangeListener>` for safe observer notification

### 2.6 Render Pass Architecture (from Viewport)

The rendering pipeline is a fixed ordered list of stateless passes:

```java
private final List<RenderPass> renderPasses = List.of(
    new BackgroundPass(),    // fills background + current-line highlight
    new SearchPass(),        // search match highlights
    new SelectionPass(),     // selection rectangles
    new TextPass(),          // text + syntax coloring
    new CaretPass(),         // blinking caret
    new ScrollbarPass()      // scrollbar overlays
);
```

Each pass implements:
```java
interface RenderPass {
    default void renderFull(RenderContext context) {}
    default void renderLine(RenderContext context, RenderLine renderLine) {}
}
```

The immutable `RenderContext` record is constructed once per frame and passed to all passes. It captures: `GraphicsContext`, theme, glyph metrics, selection state, scroll offsets, viewport dimensions, and all computed geometry.

### 2.7 Viewport Invalidation & Redraw Cycle

```
mutation → requestLayout() → layoutChildren() → if (dirty) redraw()
```

Three invalidation levels:
1. `markDirty()` — sets `dirty = true`, `fullRedrawRequired = true`
2. `markLinesDirty(start, end)` — sets individual bits in `dirtyLines` BitSet
3. Caret change → marks old and new caret lines dirty

Incremental redraw skips unchanged lines when possible, falling back to full redraw on scroll jumps.

---

## 3. Feature Specification Analysis

### 3.1 MVP Features (from docking-tree-features.md)

| Feature | Description | Complexity |
|---------|-------------|------------|
| Hierarchical data model | Recursive `TreeItem<T>` structure | Medium |
| Virtualization | Only render visible rows | High |
| Expand/Collapse | Disclosure chevron toggle | Medium |
| Modular cell factory | 5-zone layout (disclosure, prefix, core, spacer, suffix) | Medium |
| Single selection & focus | Keyboard navigation, highlight states | Medium |
| Auto-scrolling & indentation | Depth-based indent, scrollbar scaling | Medium |

### 3.2 Enterprise Features

| Feature | Description | Priority |
|---------|-------------|----------|
| Lazy loading | On-demand child population | High |
| Prefix controls | Checkbox, eye, lock toggles | Medium |
| Hover actions | Edit/Delete buttons on hover | Medium |
| Badges/counters | Right-aligned child count | Low |
| Multi-selection | Shift+click, Ctrl+click | High |
| Drag and drop | Internal tree reordering | High |
| In-place editing | Double-click to rename | Medium |
| Context menus | Right-click popup | Medium |
| Search/filtering | Find nodes, hide non-matching | Medium |
| Connecting lines | Parent-child visual lines | Low |

### 3.3 Application Diversity (from docking-tree-applications.md)

The 30+ use cases span:
- **IDE tools**: project explorer, dependency tree, JSON/XML viewer, database browser
- **Content management**: file directories, document outlines, wiki hierarchies
- **Design tools**: layer panels, scene graphs, animation timelines
- **Business**: org charts, budgets, inventory categories

Key insight: the component must be **generic** (`TreeView<T>`) with a pluggable cell renderer, not hardcoded to any specific domain.

---

## 4. Design Decisions & Findings

### 4.1 Canvas vs. Node-Based Rendering

**Decision: Canvas-based**, consistent with CodeEditor.

Rationale:
- Virtualization is trivial — only paint visible rows, no node creation/destruction
- Consistent with the codebase's established pattern
- Better performance for large trees (10K+ nodes)
- Full control over pixel-level rendering (connecting lines, custom icons, hover states)
- Trade-off: more complex hit testing and accessibility, but aligns with project philosophy

### 4.2 Data Model: TreeItem<T> with Flattened Visible-Row Cache

The data model should be a recursive `TreeItem<T>` tree, but rendering operates on a **flattened list of visible rows** (like how CodeEditor's `VisibleLineMap` maps logical lines through fold regions). When a node is collapsed, its descendants are excluded from the flat list. This gives O(1) row-to-item lookup during rendering.

### 4.3 Cell Rendering: TreeCellRenderer<T> Functional Interface

Rather than creating heavyweight `TreeCell` JavaFX nodes (as the spec's reference code does), the canvas approach needs a **TreeCellRenderer<T>** that paints into a `GraphicsContext` at a given (x, y, width, height):

```java
@FunctionalInterface
public interface TreeCellRenderer<T> {
    void render(GraphicsContext gc, T item, TreeRenderContext context, CellState state);
}
```

Where `CellState` carries: selected, focused, hovered, expanded, leaf-vs-branch, depth, and row bounds.

### 4.4 Scroll Architecture

Following the Viewport pattern:
- Vertical scroll via `double scrollOffset` (pixel-based, not row-based)
- Fixed row height for O(1) scroll-to-row mapping (configurable, default 28px)
- Scrollbar rendered as a canvas pass (no JavaFX ScrollBar nodes)
- Mouse wheel scrolls by `SCROLL_LINE_FACTOR * rowHeight` pixels

### 4.5 Selection Model

Two modes:
- **Single selection**: click selects, arrow keys navigate
- **Multi-selection**: Ctrl+click toggles, Shift+click extends range

Selection state stored as a `Set<TreeItem<T>>` (identity-based) plus a `focusedItem` property.

### 4.6 Keyboard Navigation

| Key | Action |
|-----|--------|
| ↑/↓ | Move focus up/down in visible rows |
| ← | Collapse if expanded, else move to parent |
| → | Expand if collapsed, else move to first child |
| Enter | Toggle expand/collapse |
| Space | Toggle selection (multi-select mode) |
| Home/End | Jump to first/last visible row |
| Page Up/Down | Scroll by viewport height |
| F2 | Begin in-place edit |
| Delete | Trigger delete callback (if handler provided) |

### 4.7 Drag and Drop

Internal DnD for tree reordering:
- Drag starts on mouse press + drag threshold
- Drop zones: before, after, inside (as child) — indicated by visual hints
- Ghost image painted on overlay during drag
- External DnD: accept `DataFormat` payloads (files, text, custom)

### 4.8 Theme Mapping

A `TreeViewTheme` record maps from the docking `Theme`:

```java
record TreeViewTheme(
    Paint background,
    Paint rowBackground,
    Paint rowBackgroundAlternate,
    Paint selectedBackground,
    Paint selectedBackgroundUnfocused,
    Paint focusedBorder,
    Paint hoverBackground,
    Paint textColor,
    Paint textColorSelected,
    Paint disclosureColor,
    Paint connectingLineColor,
    Paint scrollbarTrackColor,
    Paint scrollbarThumbColor,
    Paint scrollbarThumbHoverColor,
    Paint scrollbarThumbActiveColor,
    Font font,
    double rowHeight,
    double indentWidth,
    double iconSize
)
```

---

## 5. Architectural Patterns to Reuse

### 5.1 From Viewport

- `Region`-owns-`Canvas` layout
- `layoutChildren()` → `dirty` check → `redraw()`
- `requestLayout()` as the sole invalidation trigger
- Immutable `RenderContext` record per frame
- Stateless `RenderPass` pipeline
- `GlyphCache` for font metrics (can share the same class)
- Scrollbar rendering pass with geometry record + hover/active state tracking
- Convergence loop for scrollbar visibility resolution

### 5.2 From Document

- Observable properties with `Simple*Property` backing
- `CopyOnWriteArrayList` for listeners
- Incremental updates where possible

### 5.3 From CodeEditor

- `StackPane` as top-level container implementing `DisposableContent`
- `bindThemeProperty(ObjectProperty<Theme>)` for DockManager integration
- `captureState()` / `applyState()` for session persistence
- Controller decomposition: separate input, pointer, navigation concerns
- `EditorLifecycleService` pattern: centralized listener binding/unbinding

### 5.4 From DockManager

- Overlay canvas for drag-drop hints (`setMouseTransparent(true)`)
- Coordinate transforms via `sceneToLocal()`

---

## 6. Risk Analysis

| Risk | Mitigation |
|------|------------|
| Large tree performance (100K+ nodes) | Fixed row height, virtualization, lazy child loading |
| Complex hit testing on canvas | Row = `floor((mouseY + scrollOffset) / rowHeight)`, zone detection by x-coordinate ranges |
| Accessibility (screen readers) | Out of scope for MVP; can add AccessibleRole later |
| Drag-drop visual complexity | Dedicated overlay canvas pass, separate from content rendering |
| In-place editing on canvas | Float a TextField overlay at the cell position during edit mode |
| Generic type erasure | Store `TreeItem<T>` references; renderer casts are the caller's responsibility |
| Horizontal scrolling for deep trees | Track max visible indent depth, compute content width dynamically |

---

## 7. Estimated Module Structure

```
papiflyfx-docking-tree/
  pom.xml
  src/main/java/org/metalib/papifly/fx/tree/
    api/
      TreeView.java                  ← top-level StackPane + DisposableContent
      TreeItem.java                  ← recursive data model node
      TreeCellRenderer.java          ← functional interface for custom cell painting
      TreeViewFactory.java           ← ContentFactory implementation
      TreeViewStateAdapter.java      ← ContentStateAdapter implementation
    model/
      FlattenedTree.java             ← visible-row cache (expand/collapse aware)
      TreeSelectionModel.java        ← single + multi selection
      TreeExpansionModel.java        ← tracks expanded node set
    render/
      TreeViewport.java              ← Region + Canvas, dirty-bit, render loop
      TreeRenderContext.java         ← immutable frame snapshot record
      TreeRenderRow.java             ← per-visible-row data record
      TreeRenderPass.java            ← interface
      TreeBackgroundPass.java        ← background + selection + hover highlights
      TreeContentPass.java           ← disclosure chevrons, indentation, icons, labels
      TreeConnectingLinesPass.java   ← optional parent-child lines
      TreeScrollbarPass.java         ← vertical + horizontal scrollbar
    theme/
      TreeViewTheme.java             ← component-specific theme record
      TreeViewThemeMapper.java       ← Theme → TreeViewTheme conversion
    controller/
      TreeInputController.java       ← keyboard handling
      TreePointerController.java     ← mouse click, hover, scroll, right-click
      TreeDragDropController.java    ← internal + external DnD logic
      TreeEditController.java        ← in-place rename overlay
    util/
      TreeStateCodec.java            ← Map<String,Object> serialization helpers
      GlyphCache.java                ← font metrics (or reuse from code module)
  src/main/resources/
    META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter
  src/test/java/org/metalib/papifly/fx/tree/
    TreeItemTest.java                ← data model unit tests
    FlattenedTreeTest.java           ← flattened cache tests
    TreeSelectionModelTest.java      ← selection logic tests
    TreeViewTest.java                ← component integration tests
    TreeViewFxTest.java              ← headless UI tests
```

---

## 8. Key Open Questions

1. **Should TreeItem be observable?** JavaFX's standard `TreeItem` has `valueProperty()`, `expandedProperty()`, `children` as `ObservableList`. For canvas rendering, we could simplify to plain fields with a change notification mechanism. **Recommendation**: Use JavaFX properties for `value` and `expanded` to support binding, but keep children as a plain `List` with explicit `addChild`/`removeChild` methods that fire tree structure change events.

2. **Multi-column support (TreeTableView)?** The spec mentions it as an advanced feature. **Recommendation**: Defer to a separate `TreeTableView` component. The base `TreeView` should handle single-column trees only.

3. **Context menu integration?** Should the component own a `ContextMenu` or delegate to the cell renderer? **Recommendation**: Provide a `setContextMenuFactory(Function<TreeItem<T>, ContextMenu>)` hook; the component positions and shows it on right-click.

4. **Icon rendering?** Canvas cannot render JavaFX `Node` icons directly. **Recommendation**: Use `Image` objects for icons, drawn via `gc.drawImage()`. Provide a `Function<T, Image>` icon resolver in the default cell renderer.

5. **Shared GlyphCache?** The code editor has its own `GlyphCache`. **Recommendation**: Either extract to a shared utility in the api module, or duplicate the small class in the tree module to avoid coupling.

---

## 9. Comparison with JavaFX Built-in TreeView

| Aspect | JavaFX TreeView | Our Canvas TreeView |
|--------|----------------|---------------------|
| Rendering | Scene graph nodes per cell | Canvas pixel painting |
| Virtualization | VirtualFlow (node recycling) | Flat row list (no node creation) |
| Styling | CSS | Programmatic Theme record |
| Cell customization | TreeCell subclass | TreeCellRenderer functional interface |
| Performance (large trees) | Degrades with complex cells | Constant — only visible rows rendered |
| Accessibility | Built-in ARIA | Manual (future work) |
| FXML support | Yes | No (by design) |
| Icon support | Any Node | Image only |

---

## 10. Conclusion

The new `papiflyfx-docking-tree` module is a natural extension of the framework, following the same canvas-based, programmatic-styling architecture established by `papiflyfx-docking-code`. The core challenge is virtualization of hierarchical data with expand/collapse semantics, but the patterns for this (flattened visible-row list, dirty-bit invalidation, render pass pipeline) are well-established in the codebase. The modular design (data model → flattened cache → render passes → controllers) ensures each concern can be developed and tested independently.

---

## 11. Implementation Alignment Notes

- Disclosure rendering is implemented with **chevrons** (right/down) instead of filled triangles.
- Row hit-testing now explicitly ignores scrollbar track/thumb regions, preventing item drag start while dragging scrollbars.
- A regression test (`TreeViewFxTest.hitTestIgnoresVerticalScrollbarArea`) validates the scrollbar hit-testing behavior.
