# papiflyfx-docking-media

A dockable media content type for PapiflyFX docking. It hosts image, SVG, audio, video, and embedded viewers behind a single `MediaViewer` API with session persistence and runtime theme binding.

## Features

- Unified `MediaViewer` API for image, SVG, audio, video, and embedded content
- Session persistence through `MediaViewerStateAdapter`
- Runtime theme propagation from the host `Theme` into the active viewer
- Shared transport-bar density aligned to the 4px UI grid
- Viewer-specific state restore for playback position, volume, zoom, and pan

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-media</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register the factory and adapter

```java
DockManager dockManager = new DockManager();

ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new MediaViewerStateAdapter());
dockManager.setContentStateRegistry(registry);

dockManager.setContentFactory(new MediaViewerFactory());
```

### 2. Create and load media

```java
MediaViewer viewer = new MediaViewer();
viewer.bindThemeProperty(dockManager.themeProperty());
viewer.loadMedia("https://example.com/demo.mp4");

DockLeaf leaf = dockManager.createLeaf("Preview", viewer);
leaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);
```

## UI Standards

The media module participates in the shared UI standardization rollout while keeping viewer-specific rendering local.

- `Theme` remains the single styling input exposed to callers.
- `MediaViewer.bindThemeProperty(...)` propagates theme changes to the active viewer instance.
- Audio and video transport controls use the shared density model from `UiMetrics`.
- Host chrome follows the shared compact-vs-regular spacing system even when the underlying media surface is custom rendered.
- Embedded page/video content remains viewer-specific and is not recolored by the docking framework.

## Persistence

`MediaViewerStateAdapter` stores and restores `MediaState` through `MediaStateCodec`.

- `FACTORY_ID`: `media-viewer`
- adapter version: `1`

Persisted state includes URL, detected URL kind, playback position, volume, zoom, and pan offsets where the active viewer supports them.

## Run Tests

```bash
./mvnw -pl papiflyfx-docking-media -am -Dtestfx.headless=true test
```
