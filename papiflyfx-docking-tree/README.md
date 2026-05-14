# papiflyfx-docking-tree

A dockable canvas-based tree content type for PapiflyFX docking. It provides virtualized rendering, keyboard navigation, inline editing, incremental search, and session persistence without relying on FXML.

## Features

- Canvas-based virtualized tree rendering for large hierarchies
- Expand/collapse, keyboard navigation, pointer interaction, and inline rename support
- Optional inline node-info surfaces and toggle policies
- Incremental search overlay with shared popup/compact-control styling
- Drag-and-drop controller for tree interaction scenarios
- Docking integration through `ContentFactory` and `ContentStateAdapter`
- Runtime theme switching through `bindThemeProperty(ObjectProperty<Theme>)`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-tree</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register the factory and adapter

```java
DockManager dockManager = new DockManager();

ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new TreeViewStateAdapter());
dockManager.setContentStateRegistry(registry);

dockManager.setContentFactory(new TreeViewFactory());
```

### 2. Create a tree leaf

```java
TreeView<String> tree = TreeViewFactory.createDefaultTreeView();
tree.bindThemeProperty(dockManager.themeProperty());

DockLeaf leaf = dockManager.createLeaf("Workspace", tree);
leaf.setContentFactoryId(TreeViewFactory.FACTORY_ID);
```

### 3. Provide custom content

```java
TreeView<MyNode> tree = new TreeView<>();
tree.setRoot(rootItem);
tree.setSearchTextExtractor(MyNode::label);
tree.setEditCommitHandler((item, text) -> item.getValue().rename(text));
tree.bindThemeProperty(dockManager.themeProperty());
```

## UI Standards

The tree module follows the shared UI standardization model introduced in `papiflyfx-docking-api`.

- `Theme` remains the only runtime theme source.
- `TreeViewThemeMapper` projects shared colors and dimensions into tree-specific render metrics.
- The search overlay uses shared surface/control classes such as `pf-ui-popup-surface` and `pf-ui-compact-field`.
- Shared spacing and density come from `UiMetrics`, including the 4px grid and compact control sizing.
- Hover, active, and selected row states are validated in headless FX tests.

## Persistence

`TreeViewStateAdapter` saves and restores tree state using `TreeView.captureState()` and `TreeView.applyState(...)`.

- `FACTORY_ID`: `tree-view`
- adapter version: `2`

The default factory creates a sample workspace tree that can be restored through the docking session pipeline.

## Run Tests

```bash
./mvnw -pl papiflyfx-docking-tree -am -Dtestfx.headless=true test
```
