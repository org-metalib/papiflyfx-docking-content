# PapiflyFX Docking Tree — Implementation Plan

This document outlines the step-by-step implementation plan for the `papiflyfx-docking-tree` module, based on the findings from the research phase. The implementation follows the established architectural patterns of the `papiflyfx-docking-code` module.

## Phase 1: Module Infrastructure Setup
- [x] **Create Maven Module**
  - Create `papiflyfx-docking-tree` directory with `pom.xml`.
  - Add dependencies: `papiflyfx-docking-api` (compile), `papiflyfx-docking-docks` (test only), and JavaFX base/graphics/controls.
  - Register module in root `pom.xml`.
- [x] **Define Package Structure**
  - Create base packages: `org.metalib.papifly.fx.tree.api`, `org.metalib.papifly.fx.tree.model`, `org.metalib.papifly.fx.tree.render`, `org.metalib.papifly.fx.tree.theme`, `org.metalib.papifly.fx.tree.controller`, `org.metalib.papifly.fx.tree.util`.

## Phase 2: Core Data Model & API
- [x] **Implement `TreeItem<T>`**
  - Define recursive data structure with generic type `T`.
  - Add properties: `value`, `expanded` (JavaFX properties for binding).
  - Add `children` list with explicit `addChild`/`removeChild` methods and event notifications.
- [x] **Define `TreeCellRenderer<T>`**
  - Create functional interface: `void render(GraphicsContext gc, T item, TreeRenderContext context, CellState state)`.
  - Define `CellState` record (selected, focused, hovered, expanded, leaf/branch, depth, row bounds).
- [x] **Implement `TreeSelectionModel<T>` and `TreeExpansionModel<T>`**
  - Selection model: single/multi-selection modes, tracking selected items and focused item.
  - Expansion model: track expanded paths/nodes.

## Phase 3: Viewport & Rendering Pipeline
- [x] **Create `TreeViewport` (Canvas Region)**
  - Implement `Region`-owns-`Canvas` pattern.
  - Setup dirty-bit invalidation (`dirty`, `fullRedrawRequired`).
  - Implement `layoutChildren()` to manage canvas size and trigger `redraw()`.
- [x] **Implement Flattened Row Cache (`FlattenedTree<T>`)**
  - Map recursive `TreeItem` structure to a flat list of visible rows based on expansion state.
  - Provide O(1) lookup for index ↔ row mapping.
- [x] **Define Render Context & Passes**
  - Create immutable `TreeRenderContext` and `TreeRenderRow`.
  - Implement `TreeRenderPass` interface.
  - Implement `TreeBackgroundPass` (backgrounds, selection, hover highlights).
  - Implement `TreeContentPass` (disclosure chevrons, indents, icons, labels).
  - Implement `TreeScrollbarPass` (vertical and horizontal virtual scrollbars).
  - *(Optional)* Implement `TreeConnectingLinesPass`.
- [x] **Setup Glyph & Icon Rendering**
  - Extract or duplicate `GlyphCache` for text measurements.
  - Implement basic `Function<T, Image>` icon resolver.

## Phase 4: Controllers & Interaction
- [x] **Implement `TreeInputController`**
  - Keyboard navigation: Up/Down, Left/Right (expand/collapse), Enter, Space, Home/End.
- [x] **Implement `TreePointerController`**
  - Mouse clicks (selection, toggle expand/collapse via disclosure chevron hit-testing).
  - Hover state management.
  - Scrolling (mouse wheel integration with virtual scroll offset).
- [x] **Implement `TreeView` Top-Level Component**
  - Extend `StackPane`, implement `DisposableContent`.
  - Assemble Viewport, Models, and Controllers.
  - Bind to DockManager `Theme`.

## Phase 5: Theme Integration & Persistence
- [x] **Implement Theme Mapping**
  - Define `TreeViewTheme` record with colors, fonts, and dimensions (e.g., `rowHeight`, `indentWidth`).
  - Create mapper from generic `Theme` to `TreeViewTheme`.
- [x] **Implement Content Factory & Adapter**
  - Create `TreeViewFactory` implementing `ContentFactory`.
  - Create `TreeViewStateAdapter` for saving/restoring expansion and selection state.
  - Register adapter via `META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter`.

## Phase 6: Advanced Features (Drag & Drop, Editing)
- [x] **Implement Drag and Drop (`TreeDragDropController`)**
  - Internal reordering with visual drop hints (overlay canvas).
  - External payload support.
- [x] **Implement In-place Editing (`TreeEditController`)**
  - Support F2 / double-click.
  - Float a `TextField` overlay precisely over the canvas cell.

## Phase 7: Testing & Samples Integration
- [x] **Unit & Headless Tests**
  - Test data models, flattened cache, and selection logic.
  - Add headless UI tests for rendering loops and invalidation.
- [x] **Sample Application**
  - Add a tree view demo to `papiflyfx-docking-samples`.
  - Demonstrate custom cell renderers, 10k+ node performance, and DnD.
