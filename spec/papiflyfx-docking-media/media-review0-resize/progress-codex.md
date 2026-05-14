# media-review0-resize — progress (codex)

## Problem

Resizing the `MediaViewer` container did not reliably resize the actual media node (`ImageView`, `MediaView`, and `WebView` fallback/embed content). In some cases, media stayed at a stale size after pane resize.

## Changes made

### 1) Switched from size-property bindings to layout-time sizing

Updated viewers to set media size in `layoutChildren()` instead of binding fit/pref properties to parent width/height.

- `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/ImageViewer.java`
- `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/VideoViewer.java`
- `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/EmbedViewer.java`
- `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/SvgViewer.java`

This applies the current container size during the layout pass and keeps media sizing in sync with pane resize.

### 2) Prevented resize lock when shrinking

Set viewer minimum size explicitly to zero in the same classes above:

```java
setMinSize(0, 0);
```

Without this, computed child size could clamp the viewer and block shrinking after a previous grow.

### 3) Added/expanded FX resize coverage

Extended `MediaViewerFxTest` with container-level and media-node-level resize assertions:

- `innerViewerFillsContainerAfterResize`
- `imageNodeFitSizeTracksResize`
- `videoNodeFitSizeTracksResize`

File:

- `papiflyfx-docking-media/src/test/java/org/metalib/papifly/fx/media/api/MediaViewerFxTest.java`

## Verification

Executed:

```bash
./mvnw -pl papiflyfx-docking-media -Dtest=MediaViewerFxTest test
./mvnw -pl papiflyfx-docking-media test
```

Result: all tests passed (`32` tests total in media module, `0` failures, `0` errors).

## Follow-up (YouTube embed resize)

After initial fix, YouTube embeds could still drift from the visible area during aggressive resize cycles.

Additional hardening:

- `EmbedViewer.layoutChildren()` now explicitly `resizeRelocate(...)` the `WebView` to the full content area each layout pass.
- `WebView` min/max sizing is set (`0..Double.MAX_VALUE`) to avoid layout clamping.
- YouTube wrapper HTML now uses a fixed full-viewport iframe (`#player`) plus resize sync (`window.resize` + `ResizeObserver`) to keep iframe bounds aligned while resizing.
- Added `MediaViewerFxTest.embedNodeTracksRepeatedResize` to verify repeated grow/shrink cycles keep `EmbedViewer` and inner `WebView` sizes aligned.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -Dtest=MediaViewerFxTest test
./mvnw -pl papiflyfx-docking-media test
```

Result: all tests passed (`33` tests total in media module, `0` failures, `0` errors).

## Follow-up (Image drag/zoom viewport restriction)

Reported issue: image panning could move content outside the visible viewport during aggressive dragging and zoom changes.

Additional hardening in `ImageViewer`:

- Added viewport-aware pan clamping based on current zoom and fitted image size:
  - `clampPanToViewport()`
  - `setPanClamped(...)`
  - `maxPanX()/maxPanY()`
  - fitted size helpers for preserve-ratio image fit (`fittedImageWidth()/fittedImageHeight()`).
- Applied clamping on all relevant paths:
  - after `layoutChildren()` (resize/layout changes),
  - when image load completes,
  - on every zoom change,
  - during drag updates.
- Added package-private test hooks to support deterministic FX assertions of clamp boundaries.

Added FX coverage:

- `ImageViewerPanZoomFxTest.clampsPanWithinViewportBoundsAtHighZoom`
- `ImageViewerPanZoomFxTest.resetsPanToViewportWhenZoomReturnsToOne`

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=ImageViewerPanZoomFxTest test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`39` tests total in media module, `0` failures, `0` errors).

## Follow-up (Zoom-out drag overlap outside viewport)

Reported issue: after zooming out and dragging, image pixels could render over neighboring controls outside the media view bounds.

Root cause:

- `ImageViewer` applied transforms directly to `ImageView`, but the rendered output was not clipped to viewport bounds. In JavaFX this allows transformed content to paint outside parent bounds.

Fix:

- Added a dedicated clipped viewport layer in `ImageViewer`:
  - introduced `imageViewport` (`StackPane`) containing the `ImageView`;
  - introduced `viewportClip` (`Rectangle`) bound to `imageViewport` width/height;
  - applied `imageViewport.setClip(viewportClip)`.
- Kept existing pan-clamp behavior, so panning remains bounded and now cannot visually bleed into surrounding UI areas.

Additional test coverage:

- Added `ImageViewerPanZoomFxTest.keepsImageClippedToViewportOnResize` to assert:
  - viewport clip is active;
  - clip dimensions track viewer size before and after resize.
- Updated `MediaViewerFxTest` node lookup to use descendant search (instead of direct-child assumptions), so layout wrappers like `imageViewport` do not break resize assertions.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=ImageViewerPanZoomFxTest,MediaViewerFxTest test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`40` tests total in media module, `0` failures, `0` errors).

## Follow-up (Video not centered on resize)

Reported issue: video content drifted from center while resizing the viewport with `preserveRatio` enabled.

Root cause:

- `VideoViewer` set the parent `StackPane` alignment to `BOTTOM_CENTER`, and `MediaView` inherited that alignment, so letterboxed video anchored to the bottom instead of staying centered.

Fix:

- Kept overlay behavior intact, but explicitly centered the media layer:
  - `StackPane.setAlignment(mediaView, Pos.CENTER)` in `VideoViewer`.

Additional test coverage:

- Added `VideoViewerOverlayFxTest.mediaStaysCenteredAfterViewportResize` to assert:
  - `MediaView` alignment is `Pos.CENTER`;
  - rendered media bounds remain centered in the viewer after resize to a portrait-style viewport.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=VideoViewerOverlayFxTest,MediaViewerFxTest -Dtestfx.headless=true test
```

Result: all tests passed (`11` targeted tests, `0` failures, `0` errors).

## Follow-up (Video controls detached after resize)

Reported issue: after resizing into letterboxed layouts, transport controls stayed anchored to the viewport bottom and looked detached from the rendered video frame.

Root cause:

- `controlsOverlay` and bottom scrim were positioned from full viewer bounds.
- In tall/narrow layouts the rendered `MediaView` occupied only a centered sub-rectangle, so overlays drifted into the black bars.

Fix:

- `VideoViewer.layoutChildren()` now computes overlay geometry from rendered media bounds (`mediaView.getBoundsInParent()`), with fallback to full viewer bounds while media metrics are unavailable.
- Bottom scrim is now relocated to the media frame rectangle (same X/width as rendered video, bottom-aligned to rendered video maxY).
- Controls overlay margins are recalculated per layout from media insets, keeping controls attached to the rendered video zone.
- Center affordance sizing/position now uses rendered media bounds instead of full viewer bounds.
- Refined scrim sizing floor to avoid oversized overlay on cinematic aspect ratios:
  - `minHeight = min(68px, 30% of rendered media height)`.
- Anchored `controlsOverlay` internal alignment to `BOTTOM_CENTER` to keep transport row pinned to the overlay baseline.

Test updates:

- `VideoViewerOverlayFxTest.bottomScrimStaysInLowerBoundedArea` now validates scrim bounds against effective rendered media bounds.
- Existing centering and playback overlay tests continue to pass.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=VideoViewerOverlayFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtest=VideoViewerOverlayFxTest,MediaViewerFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`41` media-module tests total, `0` failures, `0` errors).

## Follow-up (Video controls painting outside viewer bounds)

Reported issue: transport controls could visually overlap neighboring UI areas (dock tree/sidebar) when viewport became narrower than control row content.

Root cause:

- JavaFX `StackPane` does not clip child rendering by default.
- In narrow layouts, `TransportBar` could extend beyond `VideoViewer` bounds and still paint outside its pane.

Fix:

- Added explicit viewport clipping in `VideoViewer`:
  - introduced `viewportClip` (`Rectangle`);
  - bound clip width/height to `VideoViewer` width/height;
  - applied `setClip(viewportClip)`.
- Existing overlay-to-rendered-media anchoring remains in place; clip now guarantees no visual bleed outside viewer bounds.

Additional test coverage:

- Added `VideoViewerOverlayFxTest.viewerClipTracksViewportResize` to assert:
  - `VideoViewer` clip is a `Rectangle`;
  - clip width/height track viewer size before and after resize.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=VideoViewerOverlayFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`42` media-module tests total, `0` failures, `0` errors).

## Follow-up (Dragging slider dot does not move footage)

Reported issue: moving the timeline thumb did not visibly scrub video content.

Root cause:

- Seek was primarily finalized on release; drag motion itself did not actively issue seek updates.
- In some interactions this made scrubbing feel non-responsive until after release.

Fix:

- Added live scrub seeking in `TransportBar` while dragging:
  - `seekBar.valueProperty()` now calls throttled `maybeSeekDuringScrub(...)` when `valueChanging` is active.
  - Added throttling guards (`LIVE_SCRUB_INTERVAL_NANOS`, `LIVE_SCRUB_DELTA_SECONDS`) to avoid over-frequent seek calls.
  - Kept final release commit (`commitSeekFromSlider()`) for accurate final position.
- Added scrub tracking helpers:
  - `resetScrubTracking()`
  - `clampSeekValue(...)`
  - `maybeSeekDuringScrub(...)`
- Hardened range sync so slider `max` never regresses while total duration is temporarily unknown (`syncSeekRange(...)` keeps existing max as floor).

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=AudioViewerTransportBarFxTest,VideoViewerOverlayFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`42` media-module tests total, `0` failures, `0` errors).

## Follow-up (macOS CMTimeMakeWithSeconds timescale warnings)

Reported issue: macOS logs during scrubbing such as:
`CMTimeMakeWithSeconds(..., timescale 1): warning: error introduced due to very low timescale`.

Root cause:

- Some seek calls were created with second-based `Duration` constructors, which can map to coarse one-second timescale in AVFoundation-backed media paths.

Fix:

- Switched seek-related durations to millisecond precision:
  - `TransportBar.commitSeekFromSlider()` now calls `service.seek(Duration.millis(...))`.
  - `MediaPlayerService.seek(...)` normalizes incoming durations to millisecond precision and clamps negative inputs to zero.
  - `MediaPlayerService.stepForward()`, `stepBackward()`, and `seekRelative(...)` now use millisecond-based increments.

Result:

- Seeks keep sub-second precision and avoid coarse-timescale warning behavior on macOS.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=AudioViewerTransportBarFxTest,VideoViewerOverlayFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`42` media-module tests total, `0` failures, `0` errors).

## Follow-up (Seek slider resets to start on mouse scrub)

Reported issue: dragging the seek slider with mouse could jump playback back near the beginning instead of scrubbing to the selected point.

Root cause:

- Seek commit relied only on raw mouse release.
- Slider range could remain undersized (`max=1`) if duration metadata lagged, making seeks clamp to near-zero seconds.

Fix:

- Hardened `TransportBar` seek flow:
  - seek commit now supports slider `valueChanging` transitions and mouse release fallback (`commitSeekFromSlider()`),
  - scrub preview updates time label while dragging (`updateScrubPreview(...)`),
  - seek range auto-syncs during playback (`syncSeekRange(...)`) using total duration when known, or current time fallback when duration is still unknown.
- `onTimeChanged(...)` now updates seek range before applying playback progress to slider value.

Verification rerun:

```bash
./mvnw -pl papiflyfx-docking-media -DskipTests test-compile
./mvnw -pl papiflyfx-docking-media -Dtest=AudioViewerTransportBarFxTest,VideoViewerOverlayFxTest -Dtestfx.headless=true test
./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test
```

Result: all tests passed (`42` media-module tests total, `0` failures, `0` errors).
