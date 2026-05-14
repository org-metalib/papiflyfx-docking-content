# papiflyfx-docking-hugo

A dockable Hugo preview content type for PapiflyFX docking.
It starts `hugo server`, renders pages inside JavaFX `WebView`, and supports session persistence.

## Features

- Internal preview rendering via JavaFX `WebView`/`WebEngine`
- Hugo CLI preflight check (`hugo version`)
- Managed `hugo server` lifecycle (start/stop/cleanup)
- Toolbar actions: start, stop, back, forward, reload, open in browser
- External navigation guard (keeps embedded view on local Hugo origin)
- Docking persistence via `ContentStateAdapter` + `LeafContentData`
- Theme-aware host chrome for toolbar, status, and placeholder surfaces while leaving page content independent
- Ribbon integration via `RibbonProvider` with `Hugo` and contextual `Hugo Editor` tabs

## Requirements

- Java 25
- JavaFX 25.0.2
- Hugo CLI installed and available on `PATH`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-hugo</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register adapter and factory

```java
DockManager dockManager = new DockManager();

ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new HugoPreviewStateAdapter());
dockManager.setContentStateRegistry(registry);

dockManager.setContentFactory(new HugoPreviewFactory(Path.of("/workspace/my-hugo-site")));
```

### 2. Create preview content

```java
HugoPreviewPane preview = new HugoPreviewPane(new HugoPreviewConfig(
    Path.of("/workspace/my-hugo-site"),
    "hugo:docs",
    "/",
    1313,
    true,
    false
));

DockLeaf leaf = dockManager.createLeaf("Docs Preview", preview);
leaf.setContentFactoryId(HugoPreviewFactory.FACTORY_ID);
leaf.setContentData(LeafContentData.of(
    HugoPreviewFactory.FACTORY_ID,
    "hugo:docs",
    HugoPreviewStateAdapter.VERSION
));
```

### 3. Optional: track host theme state

```java
preview.bindThemeProperty(dockManager.themeProperty());
```

Binding is optional and themes only the host chrome. It does not recolor Hugo page content inside `WebView`.

## Theme And UI Standards

The Hugo module participates in the shared UI standardization rollout at the host-chrome layer.

- `Theme` remains the only runtime styling input.
- Toolbar and status-bar density follow the shared `UiMetrics` scale.
- Theme changes restyle the preview host surface, toolbar, status badges, and placeholder state.
- Embedded site content remains controlled by the page itself and is intentionally not recolored by docking theme tokens.

## Persisted State

`HugoPreviewStateAdapter` persists the following keys:

- `siteDir`
- `relativePath`
- `drafts`

State codec: `HugoPreviewStateCodec`

## Main APIs

| Type | Purpose |
|------|---------|
| `HugoPreviewPane` | Main dock content (`DisposableContent`) |
| `HugoPreviewConfig` | Startup configuration (site root, path, port, behavior) |
| `HugoPreviewFactory` | Docking `ContentFactory` integration |
| `HugoPreviewStateAdapter` | Save/restore integration for sessions |
| `HugoServerProcessManager` | Hugo process lifecycle + readiness handling |
| `HugoCliProbe` | Hugo availability/version probing |
| `HugoRibbonProvider` | Ribbon command groups and contextual tab contribution |
| `HugoRibbonActions` | Ribbon action bridge implemented by `HugoPreviewPane` |

## Run Tests

```bash
./mvnw -pl papiflyfx-docking-hugo test
```

## Headless UI Tests

```bash
./mvnw -pl papiflyfx-docking-hugo -Dtestfx.headless=true test
```
