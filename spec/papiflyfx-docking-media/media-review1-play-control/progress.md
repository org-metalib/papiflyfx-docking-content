# media-review0-play-control — progress

## 2026-03-03

### Phase 1 completed

- Added strict vertical sizing constraints to `TransportBar` (`setFillHeight(false)`, `setMinHeight(USE_PREF_SIZE)`, `setMaxHeight(USE_PREF_SIZE)`) and `setPickOnBounds(false)`.
- Wrapped video controls into a dedicated bottom `controlsOverlay` container in `VideoViewer` to isolate transport layout from full viewer area.
- Verified compile/typecheck:
  - `./mvnw -pl papiflyfx-docking-media -DskipTests compile`

### Phase 2 completed

- Added a dedicated bottom scrim `Region` in `VideoViewer` with constrained lower-zone gradient sizing (bounded to lower ~20-30%% of viewer height).
- Applied theme-driven gradient colors via `MediaThemeMapper` so overlay contrast adapts to active theme.
- Refined `TransportBar` row visuals: rounded semi-transparent background, subtle border, and shadow while keeping shared usage with `AudioViewer`.
- Verified compile/typecheck:
  - `./mvnw -pl papiflyfx-docking-media -DskipTests compile`

### Phase 3 completed

- Reworked `TransportBar` playback wiring around explicit `MediaPlayer` lifecycle handlers:
  - `setOnReady`, `setOnPlaying`, `setOnPaused`, `setOnStopped`, `setOnStalled`, `setOnEndOfMedia`, `setOnError`.
- Added robust per-player attach/detach lifecycle in `TransportBar` to avoid stale listeners.
- Implemented explicit playback-state model (`START/READY/PLAYING/PAUSED/STOPPED/STALLED/ENDED/ERROR`) and surfaced it via read-only properties.
- Upgraded interaction behavior:
  - idle timeout moved to `3s`;
  - hide path active only while `PLAYING`;
  - controls remain visible for paused/stalled/ended/error;
  - fade-in/fade-out transitions preserved.
- Added centered large video affordance in `VideoViewer` with state transitions:
  - play icon for start/ready/paused/stopped;
  - replay icon for ended;
  - hidden while playing.
- Fixed `VideoViewer.applyPlaybackState(...)` to avoid overriding control callbacks by switching restore logic to status-based application instead of `setOnReady` replacement.
- Applied the same non-overriding restore approach in `AudioViewer.applyPlaybackState(...)` to keep shared `TransportBar` lifecycle callbacks intact.
- Verified compile/typecheck continuously while implementing:
  - `./mvnw -pl papiflyfx-docking-media -DskipTests compile`
  - `./mvnw -pl papiflyfx-docking-media -DskipTests test-compile`

### Phase 4 completed

- Added focused FX tests:
  - `VideoViewerOverlayFxTest.bottomScrimStaysInLowerBoundedArea`
  - `VideoViewerOverlayFxTest.controlsAutoHideOnlyWhenPlaying`
  - `VideoViewerOverlayFxTest.centerAffordanceTracksPlaybackState`
  - `AudioViewerTransportBarFxTest.transportBarRemainsCompactAndVisibleOnPause`
- Extended existing embed resize test sizing sequence to remain headless-safe while preserving repeated resize assertions.
- Added test-access hooks in viewers for deterministic FX assertions.
- Validation commands executed:
  - `./mvnw -pl papiflyfx-docking-media -DskipTests test-compile`
  - `./mvnw -pl papiflyfx-docking-media -Dtest=VideoViewerOverlayFxTest,AudioViewerTransportBarFxTest test`
  - `./mvnw -pl papiflyfx-docking-media test`
  - `./mvnw -pl papiflyfx-docking-media -Dtestfx.headless=true test`
  - `./mvnw -pl papiflyfx-docking-media -am -Dtestfx.headless=true test`
- Final status: all module and aggregated headless tests passed.
