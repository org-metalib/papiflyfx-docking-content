# papiflyfx-docking-media — Research Report

## 1. Source Analysis

The design brief (`docking-media-gemini.md`) describes a media viewer component that plugs into
the PapiflyFX docking framework.  It covers three content domains:

| Domain | Core JavaFX type | Notes |
|--------|-----------------|-------|
| Raster images (PNG, JPG, GIF, BMP) | `ImageView` + `javafx.scene.image.Image` | Async background loading via `Image(url, background=true)` |
| Video / audio (MP4, MOV, HLS) | `MediaView` + `MediaPlayer` + `Media` | Native GStreamer; hardware-accelerated |
| Vector graphics (SVG) | `SVGPath` / Apache Batik / WebView | No native SVG loader in JavaFX |

---

## 2. Fit with Existing Framework APIs

### 2.1 ContentFactory / ContentStateAdapter

The framework already provides the two-step plug-in contract:

```
ContentFactory.create(factoryId)           → Node
ContentStateAdapter.saveState(id, node)    → Map<String,Object>
ContentStateAdapter.restore(LeafContentData) → Node
```

`CodeEditorFactory` + `CodeEditorStateAdapter` are the reference implementation for a
complex content type.  The media module needs:

- `MediaViewerFactory`  — dispatches on `"media-viewer"` factory id
- `MediaViewerStateAdapter` — serializes `mediaUrl`, `currentTimeMs`, `zoomLevel`, `volume`

### 2.2 DisposableContent

`CodeEditor` implements `DisposableContent.dispose()` to stop background workers and unbind
listeners.  A `MediaViewer` **must** do the same — calling `MediaPlayer.dispose()` and
clearing cached `Image` references is mandatory to prevent native-heap leaks.

### 2.3 Theme

The `Theme` record (all programmatic, no CSS) carries `background`, `accentColor`,
`textColor`, `contentFont`, etc.  The media transport controls (play/pause, seek bar, volume
knob) must be painted using these values, not hardcoded colors.

### 2.4 Docking Lifecycle Hazard — Re-parenting

When the user drags a tab between split panes, PapiflyFX removes the content `Node` from the
scene graph and re-inserts it elsewhere.  For a `MediaView`, this causes `MediaPlayer` to
stall or restart because the player is tightly coupled to the view in JavaFX internals.

**Solution (Detached Controller pattern):**
Keep `MediaPlayer` in a separate service object (`MediaPlayerService`).  The `MediaView` is
just a rendering surface — it can be re-bound to the existing player after any reparent via
`mediaView.setMediaPlayer(player)`.

---

## 3. Format & Stream Strategy

### 3.1 Raster Images

`javafx.scene.image.Image` supports PNG, JPG, GIF (animated), BMP out of the box.
Background loading: `new Image(url, 0, 0, true, true, true)` — last `true` triggers async
load; `image.progressProperty()` drives a `ProgressIndicator`.

Error detection: `image.isError()` / `image.exceptionProperty()` → show a broken-image
placeholder.

### 3.2 Video / Direct Streams

`javafx.scene.media.Media` → `MediaPlayer` → `MediaView` handles MP4/H.264, MOV, and HLS
(.m3u8).  Hardware acceleration is automatic when Prism uses the GPU backend.

**HLS (.m3u8)** is the most robust streaming option: JavaFX handles adaptive-bitrate
quality switching automatically.  **Progressive HTTP** (plain `.mp4` on a server) also
works — playback starts while the remainder downloads.  Both map to `VideoViewer`.

Key settings for docking:
- `mediaView.setPreserveRatio(true)` — prevents distortion on split resize
- `mediaView.setSmooth(true)` — bilinear filtering
- `mediaView.fitWidthProperty().bind(container.widthProperty())`
- `mediaView.fitHeightProperty().bind(container.heightProperty())`

Transport controls needed: play/pause, stop, seek slider, time label, volume slider, mute
toggle, frame-step (±1 frame, ±5 s).

Auto-mute on visibility change: listen to `Node.visibleProperty()` (or the `DockState`
observable from the framework) and mute when the leaf is hidden/minimized.

### 3.3 Web-Embedded Streams (YouTube, Vimeo, Twitch)

Major consumer platforms do not expose direct `.mp4` links due to ads and DRM.
The only viable JavaFX approach is a `WebView` loaded with the platform's embed URL:

| Platform | Embed pattern | Notes |
|----------|--------------|-------|
| YouTube | `https://www.youtube.com/embed/{VIDEO_ID}` | Works reliably |
| Vimeo | `https://player.vimeo.com/video/{VIDEO_ID}` | Works reliably |
| Twitch | Twitch Embed Player via `WebView` | Requires `parent` query param workaround |

These are handled by a new **`EmbedViewer`** (`WebView`-based).  The `StreamUrlDetector`
utility classifies a URL before the factory decides which viewer to instantiate — the
user never has to choose the player type manually.

**Netflix / Disney+** are explicitly out of scope: they require Widevine DRM, which
neither `MediaView` nor `WebView` supports.

### 3.4 Professional / Low-Latency Streams (RTSP, RTMP, MPEG-DASH)

Security cameras, drone feeds, and broadcast workflows use protocols JavaFX does not speak
natively.  **vlcj** (wrapping LibVLC) is the recommended third-party path: it handles
RTSP, RTMP, MPEG-DASH, MKV, WebM, and virtually every other codec.

The framework will leave a **`VlcjVideoViewer`** slot in the factory dispatch that is
populated only when vlcj is present on the classpath, keeping the core module free of
hard runtime dependencies.

### 3.5 SVG — Chosen Approach: `SVGPath` (single-path) with `WebView` fallback

For the initial version use the pure-JavaFX `SVGPath` node for simple icon-level SVGs and
fall back to a lightweight `WebView` wrapper for multi-element SVGs.  Apache Batik is
deliberately deferred (heavy dependency, slow startup).

`WebView` SVG limitations to document:
- `WebView` is heavyweight (OpenGL compositing surface); do not use more than one per stage
- Drag-and-drop events may be absorbed by the WebView's internal event handling

### 3.6 Stream Handler Strategy — Factory Dispatch

The `StreamUrlDetector` classifies every URL before any viewer is created:

```
URL classification → viewer node
──────────────────────────────────────────────────────
.jpg .jpeg .png .bmp .gif          →  ImageViewer    (ImageView + zoom/pan)
.mp4 .mov .m4v .m3u8 .mp3 .wav    →  VideoViewer /  (MediaView + transport bar)
  .aac .aiff                          AudioViewer
YouTube / Vimeo embed host         →  EmbedViewer    (WebView + embed URL)
Twitch embed host                  →  EmbedViewer    (WebView, parent-param workaround)
rtsp:// rtmp:// .mpd               →  VlcjVideoViewer (optional; ErrorViewer if absent)
.svg                               →  SvgViewer      (SVGPath or WebView)
unknown / error                    →  ErrorViewer    (icon + message label)
```

This classification is deterministic and happens entirely within `ViewerFactory` so
`MediaViewer` is always returned as the stable public type.

---

## 4. State Persistence Contract

Following the `LeafContentData` / `ContentStateAdapter` pattern already in use:

```json
{
  "mediaUrl": "https://www.youtube.com/embed/dQw4w9WgXcQ",
  "urlKind":  "embed",
  "currentTimeMs": 12340,
  "volume": 0.8,
  "muted": false,
  "zoomLevel": 1.5,
  "panX": 0.0,
  "panY": 0.0
}
```

`urlKind` is a String enum stored alongside `mediaUrl` so that on restore the factory can
skip re-classification and construct the correct viewer immediately.  Values:
`"file-image"`, `"file-video"`, `"file-audio"`, `"file-svg"`, `"stream-hls"`,
`"stream-http-video"`, `"embed"`, `"rtsp"`, `"unknown"`.

Schema version starts at `1`.  Future additions (e.g. loop flag) increment the version.

---

## 5. UX Requirements Derived from Brief

### Loading states
- `ProgressIndicator` centered over the content area while `Image.progressProperty() < 1.0`
  or `MediaPlayer.status == UNKNOWN / STALLED`

### Zoom & Pan (images)
- Mouse wheel → scale transform on the `ImageView`; clamp between 0.1× and 10×
- Click-drag → translate; constrained to keep at least one pixel of image visible
- Double-click → reset zoom to fit

### Auto-hiding HUD
- Transport controls live in a `VBox` pinned to the bottom of the content `StackPane`
- On mouse-enter: fade in (0.3 s)
- After 2 s of no mouse movement during playback: fade out (0.5 s)
- `PauseTransition` handles the idle timer, reset on `MouseEvent.MOUSE_MOVED`

### Fullscreen toggle
- Double-click on image/video area OR a button in the HUD
- Creates a new `Stage` (secondary window), moves content into it
- On close, re-parents content back into the dock leaf

### Keyboard shortcuts (when focused)
- `Space` — play/pause
- `M` — mute toggle
- `←` / `→` — seek ±5 s
- `J` / `L` — frame step back/forward
- `F` — fullscreen toggle
- `+` / `-` — zoom in/out (image mode)
- `0` — reset zoom

---

## 6. Architecture Ideas

### 6.1 Package Layout

```
org.metalib.papifly.fx.media/
  api/
    MediaViewer.java            — StackPane implements DisposableContent
    MediaViewerFactory.java     — ContentFactory
    MediaViewerStateAdapter.java — ContentStateAdapter
  model/
    MediaState.java             — record (mediaUrl, urlKind, currentTimeMs, volume, muted, zoomLevel, panX, panY)
    MediaStateCodec.java        — Map<String,Object> ↔ MediaState
    UrlKind.java                — enum: FILE_IMAGE, FILE_VIDEO, FILE_AUDIO, FILE_SVG,
                                         STREAM_HLS, STREAM_HTTP_VIDEO, EMBED, RTSP, UNKNOWN
  player/
    MediaPlayerService.java     — owns MediaPlayer lifecycle; detached from view
    ImageLoaderService.java     — async Image load with progress/error callbacks
  stream/
    StreamUrlDetector.java      — classifies a URL to a UrlKind
    EmbedUrlResolver.java       — maps a canonical video URL to an embed URL
  viewer/
    ImageViewer.java            — StackPane with ImageView, zoom/pan, HUD
    VideoViewer.java            — StackPane with MediaView, transport controls
    AudioViewer.java            — StackPane with waveform placeholder, transport controls
    EmbedViewer.java            — StackPane with WebView for YouTube/Vimeo/Twitch embeds
    SvgViewer.java              — StackPane with SVGPath or WebView
    ErrorViewer.java            — StackPane with error label
    ViewerFactory.java          — picks viewer by UrlKind; vlcj slot optional
  controls/
    TransportBar.java           — HUD: play/pause, stop, seek, time, volume, mute, fullscreen
    ZoomControls.java           — reset zoom button, zoom label
  theme/
    MediaThemeMapper.java       — Theme → Paint/Color for transport controls
```

### 6.2 MediaPlayerService (Detached Controller)

```java
class MediaPlayerService {
    private MediaPlayer player;

    void load(String url) { /* stop old, create Media, create MediaPlayer */ }
    void bind(MediaView view) { view.setMediaPlayer(player); }
    void unbind(MediaView view) { view.setMediaPlayer(null); }
    void dispose() { if (player != null) { player.dispose(); player = null; } }
}
```

The `VideoViewer` calls `service.bind(mediaView)` in `sceneProperty()` listener and
`service.unbind(mediaView)` before reparent.

### 6.3 ImageLoaderService

```java
class ImageLoaderService {
    private Image image;
    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>();

    void load(String url) {
        image = new Image(url, 0, 0, true, true, true); // background=true
        progress.bind(image.progressProperty());
        error.bind(/* image.exceptionProperty() map null-safe */);
        imageProperty.bind(/* conditional on progress == 1.0 */);
    }
    void dispose() { image = null; }
}
```

### 6.4 Theme Integration

`TransportBar` exposes `void applyTheme(Theme t)` called by `MediaViewer.bindThemeProperty(ObjectProperty<Theme>)`.

Controls use:
- `theme.background()` → transport bar background fill
- `theme.accentColor()` → seek slider track, play button accent
- `theme.textColor()` → time label text fill
- `theme.contentFont()` → time label font

---

## 7. SamplesApp Integration

### 7.1 How SamplesApp works

`SamplesApp` builds a `TreeView` navigation from `SampleCatalog.all()`.  Each
`SampleScene` contributes a `category()` and `title()` for the nav tree and a
`build(Stage, ObjectProperty<Theme>)` that returns the root `Node` to show in the content
area.  Disposal is automatic: `disposeContentArea()` walks the node tree and calls
`DisposableContent.dispose()` on any node that implements it, and `DockManager.dispose()`
on any `DockManager` found via the root pane property.

`MediaViewer` implements `DisposableContent`, so no special cleanup wiring is needed in
any sample.

### 7.2 Planned media demos (category "Media")

Six demos cover the full surface of the component:

| Title | What it shows | Viewer type |
|-------|--------------|-------------|
| **Image Viewer** | Zoom, pan, double-click reset; bundled PNG | `ImageViewer` standalone |
| **Video Player** | Transport bar, seek, volume, mute, frame-step; bundled MP4 | `VideoViewer` in `DockManager` |
| **Split: Image + Video** | Two `MediaViewer` panels in a `DockSplitGroup` | `ImageViewer` + `VideoViewer` |
| **HLS Stream** | Live/on-demand stream from a public `.m3u8` URL | `VideoViewer` via HLS |
| **YouTube Embed** | Canonical watch URL auto-converted to embed; `EmbedViewer` | `EmbedViewer` |
| **Persist & Restore** | Session save/load; video resume time and image zoom restored | `VideoViewer` + `ImageViewer` docked |

### 7.3 Registration changes in `SampleCatalog`

After adding the media module dependency to `papiflyfx-docking-samples/pom.xml`, the
catalog gains six new entries under the `"Media"` group:

```java
new ImageViewerSample(),
new VideoPlayerSample(),
new SplitMediaSample(),
new HlsStreamSample(),
new YouTubeEmbedSample(),
new MediaPersistSample()
```

### 7.4 Sample package

```
papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/media/
  ImageViewerSample.java
  VideoPlayerSample.java
  SplitMediaSample.java
  HlsStreamSample.java
  YouTubeEmbedSample.java
  MediaPersistSample.java
```

### 7.5 Wiring pattern for docked media samples

Docked samples follow the same three-step setup used by code editor samples:

```java
ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new MediaViewerStateAdapter());   // first
dm.setContentStateRegistry(registry);
dm.setContentFactory(new MediaViewerFactory());     // then

MediaViewer viewer = new MediaViewer();
viewer.bindThemeProperty(themeProperty);
viewer.loadMedia("file:///...or https://...");

var leaf = dm.createLeaf("Title", viewer);
leaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);
```

---

## 8. Open Questions / Risks

| Topic | Risk | Mitigation |
|-------|------|-----------|
| `MediaView` reparent glitch | Medium — player may pause or restart | Detached controller pattern |
| `WebView` for SVG / embed in small docks | Medium — heavyweight node | Limit to one per stage; prefer `SVGPath` for SVG |
| OS codec availability | Medium — H.265 needs Windows 11 / modern macOS | Document; fallback to H.264 |
| Memory with many image docks | Low-Medium | Explicit `dispose()` + `ImageLoaderService` nulling reference |
| Frame-step accuracy | Low | Use `MediaPlayer.seek(Duration)` + single-frame duration estimate |
| Twitch `parent` domain requirement | Medium — stream blocked without correct header | `EmbedUrlResolver` appends `?parent=localhost` as workaround |
| RTSP / RTMP with no vlcj | Low — shows `ErrorViewer` | Factory detects vlcj absence at startup and skips RTSP classification |
| DRM streams (Netflix, Disney+) | High — impossible in JavaFX/WebView | Explicitly document as out of scope; show descriptive error |
