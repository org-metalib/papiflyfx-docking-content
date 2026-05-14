# media-review0-resize — Progress

## Issue

When resizing the docked pane that contains a `MediaViewer`, the displayed media
(video frame, image, embedded web content) did not scale to fill the new size.

## Root Cause

`MediaView` and `ImageView` are plain `Node`s, **not** `Region` subclasses.
A `StackPane` parent **positions** non-Region children but does **not** call
`resize()` on them.  The previous code set `fitWidth/fitHeight` via property
bindings in the constructor:

```java
mediaView.fitWidthProperty().bind(widthProperty());
mediaView.fitHeightProperty().bind(heightProperty());
```

While bindings fire synchronously when `widthProperty()` changes, the firing
happens *during* the parent's layout pass.  In that moment, `MediaView.impl_geomChanged()`
requests a child re-layout of the StackPane which is deferred to the **next**
pulse.  Under some JavaFX 23 / Java 25 conditions this deferred re-layout does
not propagate the updated fit dimensions reliably, leaving the media stuck at the
stale size.

`WebView` (used by `EmbedViewer` and by the multi-path fallback in `SvgViewer`) is also a
`Node`/`Parent`, not a `Region`, and had the same circular binding pattern
(`webView.prefWidthProperty().bind(widthProperty())`).

## Fix

Override `layoutChildren()` in each viewer to **explicitly** set the fit/pref
dimensions **inside** the layout pass, after the container's own size is final.
The property binding approach was removed.

### `VideoViewer`

```java
@Override
protected void layoutChildren() {
    super.layoutChildren();
    mediaView.setFitWidth(getWidth());
    mediaView.setFitHeight(getHeight());
}
```

Removed constructor lines:
```java
// mediaView.fitWidthProperty().bind(widthProperty());
// mediaView.fitHeightProperty().bind(heightProperty());
```

### `ImageViewer`

```java
@Override
protected void layoutChildren() {
    super.layoutChildren();
    imageView.setFitWidth(getWidth());
    imageView.setFitHeight(getHeight());
}
```

Removed constructor lines:
```java
// imageView.fitWidthProperty().bind(widthProperty());
// imageView.fitHeightProperty().bind(heightProperty());
```

### `EmbedViewer`

```java
@Override
protected void layoutChildren() {
    super.layoutChildren();
    webView.setPrefWidth(getWidth());
    webView.setPrefHeight(getHeight());
}
```

Removed constructor lines (replaced by explicit setting above):
```java
// webView.prefWidthProperty().bind(widthProperty());
// webView.prefHeightProperty().bind(heightProperty());
```

### `SvgViewer` (WebView fallback)

When the SVG contains more than one `<path>` element a `WebView` is used instead
of an `SVGPath`.  That `WebView` had the same stale binding pattern.  The viewer
now stores a `webView` field and applies the same `layoutChildren()` override:

```java
private WebView webView;   // null when SVGPath is used

@Override
protected void layoutChildren() {
    super.layoutChildren();
    if (webView != null) {
        webView.setPrefWidth(getWidth());
        webView.setPrefHeight(getHeight());
    }
}
```

Removed from `load()`:
```java
// webView.prefWidthProperty().bind(widthProperty());
// webView.prefHeightProperty().bind(heightProperty());
```

## Test Added

`MediaViewerFxTest.innerViewerFillsContainerAfterResize` — verifies that the
inner viewer always matches the `MediaViewer`'s width/height both at the initial
render size and after a stage resize to 600 × 450.

## Verification

```
./mvnw -pl papiflyfx-docking-media -am -Dtestfx.headless=true test
```

Result: **30 tests, 0 failures, 0 errors** ✅ (both rounds)
