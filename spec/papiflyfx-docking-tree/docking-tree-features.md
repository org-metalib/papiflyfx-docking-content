# Docking Tree — Feature Specification

This document tracks the implemented and deferred feature scope for `papiflyfx-docking-tree`.

## Implemented Features

### Core architecture
- Generic component API: `TreeView<T>`, `TreeItem<T>`, `TreeCellRenderer<T>`, `CellState`.
- Canvas-based, virtualized rendering through `TreeViewport` and a render pass pipeline.
- Flattened visible-row cache (`FlattenedTree`) with expansion-aware indexing.
- Theme mapping from docking `Theme` to `TreeViewTheme`.

### Interaction and navigation
- Expand/collapse with disclosure chevrons.
- Pointer selection with single and multi-selection support.
- Keyboard navigation:
  - Up/Down, Home/End, Page Up/Page Down
  - Left/Right for collapse/expand and parent/child navigation
  - Enter/Space for expansion/selection behavior
  - F2 for in-place edit
- Hover tracking and row highlighting.

### Scrolling and hit-testing
- Virtual vertical and horizontal scrollbars rendered on canvas.
- Scrollbar thumb drag and track-click navigation.
- Mouse wheel scrolling with Shift-assisted horizontal scrolling.
- Row hit-testing constrained to content area (scrollbar regions are excluded).

### Editing, drag-and-drop, and persistence
- In-place editing via `TreeEditController` with an overlay `TextField`.
- Internal drag-and-drop reordering (before/inside/after drop positions).
- External drop hook via `TreeDragDropController#setExternalDropHandler`.
- State capture/restore (`TreeViewStateAdapter`) for expanded paths, selection, focus, and scroll offsets.
- Factory integration via `TreeViewFactory` (`factoryId = "tree-view"`).

### Test and sample coverage
- Unit tests for model, flattening, and selection behavior.
- Headless FX coverage for viewport behavior and scrollbar hit-test regression.
- Sample integration with a 10k-node demo and custom renderer/icon usage.

## Deferred / Out of Scope (Current Module)
- Built-in lazy child loading hooks.
- Built-in search/filter API.
- Built-in context menu factory API.
- Built-in prefix/suffix action widgets (checkbox/eye/lock/badges) as first-class APIs.
- Multi-column `TreeTable` variant.
- Accessibility role/screen-reader integration.
