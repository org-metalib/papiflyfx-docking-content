# Plan (Copilot): Fix oversized play-control background and deliver YouTube-like video controls

## 1) Inputs consolidated

- `spec/papiflyfx-docking-media/media-review0-play-control/fix-gemini.md`
- `spec/papiflyfx-docking-media/media-review0-play-control/fix-grok.md`
- Current implementation:
  - `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/VideoViewer.java`
  - `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/controls/TransportBar.java`
  - `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/player/MediaPlayerService.java`
  - `papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/viewer/AudioViewer.java`

## 2) Problem summary

The video play controls are functionally present, but the control background can visually occupy too much of the video area.  
The desired behavior is a modern YouTube-like overlay:

- controls anchored to bottom;
- background limited to lower area (gradient/scrim), not full-screen;
- controls auto-hide while playing and reappear on interaction;
- clear playback-state feedback (playing/paused/stalled/end/error);
- centered large play affordance when paused/start/end.

## 3) Current-state findings from source

1. `VideoViewer` uses `StackPane` and already positions `TransportBar` at `BOTTOM_CENTER`.
2. `TransportBar` is an `HBox` with background, fade transitions, and idle timer (`AUTO_HIDE_SECS = 2.0`), but it is directly used as the overlay node and can be resized by parent layout.
3. `TransportBar` listens to `statusProperty()` and `currentTimeProperty()`, and sets `setOnReady` only for duration max.
4. `AudioViewer` also reuses `TransportBar`, so any heavy visual changes in `TransportBar` will affect audio UI too.
5. Existing FX tests verify media/viewer sizing but do not validate overlay sizing/visibility behavior.

## 4) Design goals

1. **Fix root layout issue first** (no oversized control background).
2. **Keep maximal video visibility** (overlay only in bottom strip).
3. **Use event-driven playback UI** (`setOnReady`, `setOnPlaying`, `setOnPaused`, `setOnStalled`, `setOnEndOfMedia`, `setOnError`).
4. **Preserve shared-component safety** (`AudioViewer` should not regress).
5. **Keep implementation programmatic (no FXML/CSS dependency)**, aligned with repo conventions.

## 5) Non-goals

- Full redesign of audio/waveform UX.
- Playlist/chapter system or marker authoring.
- New styling framework or external animation library.

## 6) Consolidated solution (all ideas merged)

### A. Overlay layering model (VideoViewer)

Use a layered structure in `VideoViewer`:

1. `MediaView` as base layer.
2. Bottom **scrim/gradient region** (transparent -> dark), constrained height (roughly lower 15-30%).
3. Bottom `TransportBar` row above the scrim.
4. Optional centered large play/pause/replay control (visible at start/paused/end, hidden while actively playing).

Key rules:

- `StackPane.setAlignment(..., Pos.BOTTOM_CENTER)` for bottom controls.
- Gradient node should be `mouseTransparent=true`.
- Overlay container should use `setPickOnBounds(false)` where appropriate so transparent regions do not block interaction.

### B. Transport bar sizing and containment

Apply hard sizing constraints to prevent vertical stretch:

- `setMaxHeight(Region.USE_PREF_SIZE)` (critical);
- ensure preferred/min heights stay tight to content;
- keep slider horizontal growth (`HBox.setHgrow(seekBar, Priority.ALWAYS)` already in place);
- if needed, cap bar width or use insets/margins so visual mass stays compact.

### C. Visual style (YouTube-like, adapted to theme)

- Replace broad opaque bar look with:
  - bottom gradient scrim (darkest at bottom, transparent toward center/top);
  - semi-transparent, rounded control row;
  - subtle shadow/contrast for readability over bright frames.
- Keep colors derived from theme mapping (`MediaThemeMapper`) and use alpha blending.

### D. Auto-hide + motion behavior

Unify and harden current hide/show flow:

- idle timeout near **3 seconds** (from recommendations; existing is 2s);
- on mouse move/enter: show controls, stop fade-out, restart idle timer;
- on idle while `PLAYING`: fade out and then set hidden state;
- when paused/stalled/end/error: keep controls visible;
- optionally hide immediately on mouse exit only when still playing.

### E. Playback event wiring (MediaPlayer)

Adopt explicit event handlers in addition to status listeners:

- `onReady`: initialize duration/seek max, initial UI state.
- `onPlaying`: switch icon state to pause, enable hide timer.
- `onPaused`: switch icon state to play, show controls and center button.
- `onStopped`: reset timeline to zero-facing state.
- `onStalled`: show buffering indicator / prevent hide.
- `onEndOfMedia`: show replay-ready state, keep controls visible.
- `onError` (and `HALTED` via status): surface error state and disable risky actions.

## 7) File-by-file implementation plan

## Phase 1 — Correct the oversized background bug (minimal safe fix)

Status: completed

1. **`TransportBar.java`**
   - Add strict height constraints to prevent StackPane-driven stretch.
   - Ensure bar background applies only to bar bounds, not parent-sized area.
   - Keep `seekBar` growth horizontal only.

2. **`VideoViewer.java`**
   - Wrap controls in a dedicated overlay container if needed for clean separation.
   - Keep existing bottom alignment and margins.

Deliverable: control background no longer appears to fill the full video surface.

## Phase 2 — Add YouTube-like overlay visuals

Status: completed

1. **`VideoViewer.java` (preferred location)**
   - Introduce a dedicated bottom gradient scrim `Region/Pane` under transport row.
   - Constrain scrim height (e.g., dynamic or fixed range) so only lower zone is shaded.

2. **`TransportBar.java`**
   - Refine row visuals (rounded corners, semi-transparent background, readable icon/text contrast).
   - Keep it generic enough for `AudioViewer`; isolate video-specific heavy visuals in `VideoViewer`.

Deliverable: controls visually match modern bottom-overlay style without excessive obstruction.

## Phase 3 — Interaction and playback-state UX

Status: completed

1. **`TransportBar.java`**
   - Upgrade idle/show/hide behavior (fade in/out and post-fade hidden state).
   - Ensure hide rules are conditional on playback state (`PLAYING` only).

2. **`VideoViewer.java`**
   - Add large centered play affordance layer.
   - Show centered control on start/paused/end, hide on playing and after idle.

3. **`MediaPlayerService.java` or viewer/control wiring**
   - Register/refresh all key `MediaPlayer` event handlers when player instance changes.

Deliverable: polished interaction parity with expected video-player behavior.

## Phase 4 — Test coverage and validation

Status: completed

1. Extend FX tests (likely in `MediaViewerFxTest` or new focused test class) to cover:
   - transport overlay height bounded (not full-height);
   - controls visible on pause / hidden after idle when playing;
   - centered play affordance visibility transitions;
   - no regressions for resize behavior already covered.

2. Validate `AudioViewer` still behaves correctly with shared `TransportBar`.

3. Run module tests:
   - `./mvnw -pl papiflyfx-docking-media -am -Dtestfx.headless=true test`

Deliverable: objective verification for the bug and new UX rules.

## 8) Acceptance criteria

1. Bottom controls no longer paint a near-fullscreen background in video mode.
2. Bottom scrim is limited to lower part of video (roughly 15-30% height).
3. Controls auto-hide after inactivity only during playback and reappear on interaction.
4. Paused/start/end states keep controls visible and show prominent centered play/replay affordance.
5. Playback UI uses explicit `MediaPlayer` lifecycle handlers for robust state sync.
6. Existing media sizing behavior remains correct; tests pass in headless mode.

## 9) Risks and mitigations

- **Risk:** Shared `TransportBar` changes degrade audio viewer UX.  
  **Mitigation:** Keep video-specific overlay/gradient/center-button in `VideoViewer`; avoid forcing these into `AudioViewer`.

- **Risk:** Multiple listener/event registrations across reloads may leak or duplicate actions.  
  **Mitigation:** Detach old listeners/handlers when player instance changes and centralize wiring.

- **Risk:** Hidden controls still consume mouse events.  
  **Mitigation:** Use `setPickOnBounds(false)`, `mouseTransparent` on non-interactive layers, and hidden-state visibility control.

- **Risk:** Contrast issues across themes and bright frames.  
  **Mitigation:** Derive colors from theme + alpha, verify against at least dark/light theme variants.

## 10) Idea coverage map (Gemini + Grok)

- Bottom-aligned stacked overlay: **included**.
- Max-height/pref-height constraint to prevent stretch: **included**.
- Gradient lower scrim (transparent -> dark): **included**.
- `pickOnBounds` / mouse transparency for overlay layers: **included**.
- Auto-hide with timer + fade transitions + debounce: **included**.
- Show controls on pause/start/end; hide mainly while playing: **included**.
- Larger centered play/pause/replay affordance: **included**.
- Explicit `MediaPlayer` event handler usage (`onReady/onPlaying/onPaused/onStalled/onEndOfMedia/onError`): **included**.
- Slider growth and polished row layout: **included**.
