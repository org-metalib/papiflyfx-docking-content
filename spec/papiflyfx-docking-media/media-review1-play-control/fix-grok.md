The main issue you're describing — play controls centered but with a background that covers almost the entire screen — usually happens in JavaFX when the control bar (or overlay pane containing the buttons) has:

- A semi-transparent or opaque background that spans the full width/height
- Is added directly as a child of the root pane without proper sizing/positioning
- Uses a layout pane that stretches instead of anchoring to the bottom

YouTube's current video player (as of 2025–2026) has these key characteristics for the controls:

- Controls appear mostly at the **bottom** (progress bar + main buttons row)
- Background is a **subtle vertical gradient** (black/very dark → fully transparent toward the top), usually covering only the lower 15–25% of the video
- Main play/pause button is large and centered **only during mouse hover / first seconds / when paused**
- Most of the time (during playback), controls auto-hide after a few seconds
- When visible, the bar is semi-transparent / pill-shaped / rounded in newer designs, but the gradient fade is still very common
- Very little permanent overlay — goal is maximal video visibility

### Recommended Fix – Modern YouTube-like Controls in JavaFX

Use a **StackPane** as the container for your video + controls (most common and clean approach).

```java
// Assuming something like this is your current structure
StackPane videoContainer = new StackPane();

// MediaView should fill the area
MediaView mediaView = new MediaView(mediaPlayer);
mediaView.setPreserveRatio(true);
mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
mediaView.fitHeightProperty().bind(videoContainer.heightProperty());

// ────────────────────────────────────────────────
// Controls overlay (this is what you want to fix)
Pane controlsOverlay = createControlsOverlay();  // ← your current control bar / pane

// Most common mistake → controlsOverlay has -fx-background-color or is too large
// Fix: make it anchored to bottom + gradient background
controlsOverlay.setPickOnBounds(false);           // mouse events go through transparent parts
controlsOverlay.setMouseTransparent(true);        // until you hover the controls area

// Better: use StackPane alignment + small gradient pane
```

#### Improved structure (recommended)

```java
StackPane playerRoot = new StackPane();
playerRoot.getChildren().addAll(mediaView, controlsOverlay);

// Position controls at bottom
StackPane.setAlignment(controlsOverlay, Pos.BOTTOM_CENTER);

// Optional: constrain height so it doesn't stretch full screen
controlsOverlay.setMaxHeight(140);          // or Region.USE_PREF_SIZE
controlsOverlay.setMinHeight(0);
controlsOverlay.prefHeightProperty().bind(Bindings.when(hoveredProperty())
    .then(140).otherwise(80));             // example: bigger when mouse is over
```

#### Add YouTube-style semi-transparent gradient background

Create a dedicated background pane (only for the fade effect) and put the actual buttons inside it.

```java
Pane gradientBar = new Pane();
gradientBar.setStyle("""
    -fx-background-color: linear-gradient(
        to bottom,
        transparent 0%,
        rgba(0,0,0,0.4) 40%,
        rgba(0,0,0,0.85) 75%,
        rgba(0,0,0,0.95) 100%
    );
    -fx-background-radius: 8 8 0 0;     /* optional rounded top */
    -fx-padding: 8 16 16 16;
    """);

// Make sure gradient doesn't take mouse events
gradientBar.setMouseTransparent(true);

// Real controls go in a separate HBox / BorderPane
HBox buttonBar = new HBox(16);
buttonBar.setAlignment(Pos.CENTER);
buttonBar.getChildren().addAll(playPauseBtn, volumeSlider, timeLabel, fullScreenBtn, ...);

// Put both inside a VBox so gradient is behind buttons
VBox controlsVBox = new VBox(gradientBar, buttonBar);
controlsVBox.setAlignment(Pos.BOTTOM_CENTER);
controlsVBox.setPickOnBounds(false);

// Then use controlsVBox instead of your old controlsOverlay
```

#### Auto-hide behavior (very YouTube-like)

```java
// Add mouse listener on the whole playerRoot
playerRoot.setOnMouseMoved(e -> {
    // show controls
    controlsVBox.setOpacity(1);
    fadeInTimeline.play();
    // reset timer
    hideTimer.restart();
});

Timeline hideTimer = new Timeline(new KeyFrame(Duration.seconds(3), ae ->
    fadeOutTimeline.play()
));

// Simple fade animations
FadeTransition fadeInTimeline = new FadeTransition(Duration.millis(200), controlsVBox);
fadeInTimeline.setToValue(1.0);

FadeTransition fadeOutTimeline = new FadeTransition(Duration.millis(400), controlsVBox);
fadeOutTimeline.setToValue(0.0);
```

#### Quick checklist of fixes to apply right now

1. **Remove any full-size background** on the controls pane (`-fx-background-color: rgba(0,0,0,0.6)` spanning whole area → delete or limit height)
2. Use `StackPane.setAlignment(…, Pos.BOTTOM_CENTER)`
3. Add gradient only to bottom 15–30% (see CSS above)
4. Set `controls.setPickOnBounds(false)` and/or `setMouseTransparent(true)` on background layers
5. Implement **auto-hide** after ~3 seconds (pause → always show)
6. Make play/pause button **larger** (scale ~1.4–1.8×) and centered only when hovered/paused
7. Consider rounded corners + subtle shadow on button bar for 2025+ YouTube feel

This should make your player feel much cleaner and closer to modern YouTube while still being fully custom in JavaFX.

If you can share a code snippet / screenshot of your current player setup (especially the FXML or the part where you add the controls), I can give a more targeted adjustment.

JavaFX's `MediaPlayer` provides several convenient **event handler properties** (of type `ObjectProperty<Runnable>`) that let you react to key playback state changes without manually polling the `statusProperty()` or listening to low-level `ChangeListener`s.

These are the main status-related event handlers (most commonly used in media players):

| Event Handler Property | When it fires (Runnable.run() is called) | Typical use case |
|------------------------|-------------------------------------------|------------------|
| `onReady`              | Status changes to `READY` (media metadata loaded, buffering complete, player can start) | Initialize UI (enable play button, show duration, set up seek bar) |
| `onPlaying`            | Status changes to `PLAYING`               | Update UI (change button to "Pause", start progress timer, hide loading spinner) |
| `onPaused`             | Status changes to `PAUSED`                | Update UI (change button to "Play") |
| `onStopped`            | Status changes to `STOPPED`               | Reset UI (seek to start, disable controls if needed) |
| `onStalled`            | Status changes to `STALLED` (buffering issue, playback paused automatically) | Show "buffering" indicator |
| `onHalted`             | Status changes to `HALTED` (fatal error, player unusable) | Show error message, reset player |
| `onEndOfMedia`         | Playback reaches the end of the media (or stopTime if set) | Loop video (`player.seek(startTime); player.play();`), play next track, show replay button |
| `onRepeat`             | Media repeats (when cycleCount > 1 and a cycle completes) | Less common, useful for playlists/loops |
| `onError`              | Error occurs (status → HALTED usually follows) | Log/show error details (`player.getError()`) |
| `onMarker`             | `currentTime` reaches a `MediaMarker` time (from `Media.getMarkers()`) | Trigger chapter events, subtitles, ads, etc. (handler receives `MediaMarkerEvent`) |

### How to Use Them (Modern JavaFX Style)

```java
MediaPlayer player = new MediaPlayer(new Media("file:///path/to/video.mp4"));

// Recommended: use lambda or method reference
player.setOnReady(() -> {
    // Safe to call play() if autoPlay=false, update duration label, etc.
    durationLabel.setText(formatDuration(player.getTotalDuration()));
    playButton.setDisable(false);
});

player.setOnPlaying(() -> {
    playButton.setText("Pause");
    // Start a Timeline to update seek slider every 200–500ms if needed
});

player.setOnPaused(() -> {
    playButton.setText("Play");
});

player.setOnEndOfMedia(() -> {
    // Example: loop single video
    player.seek(Duration.ZERO);
    player.play();
    // Or for playlist: loadNextMedia();
});

player.setOnStalled(() -> {
    statusLabel.setText("Buffering...");
});

player.setOnError(() -> {
    System.err.println("Media error: " + player.getError());
    statusLabel.setText("Error: " + player.getError().getMessage());
});

// Optional: react to status changes more generally (if you need UNKNOWN → READY etc.)
player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
    System.out.println("Status changed: " + newStatus);
    // You can still do fine-grained logic here if the onXXX handlers aren't enough
});
```

### Quick Tips for Your Media Player (Especially with YouTube-like Controls)

- **Auto-hide controls** → Show overlay on `onPlaying` + mouse move, then fade out after 3s inactivity (use `Timeline`).
- **Big centered play button** → Show it when paused or at start (`onPaused`, `onReady`, `onEndOfMedia`), hide on `onPlaying`.
- **Progress / seek bar** → Bind slider value to `currentTimeProperty()` (but use a separate `Timeline` to poll every ~250ms — direct binding can be too frequent).
- **Buffering** → Listen to `bufferProgressTimeProperty()` or just use `onStalled` + `onPlaying`.
- **Error handling** → Always set `onError` and check `player.getError()`.

These handlers have been stable since early JavaFX versions and remain the same in JavaFX 23+ (as of 2026). They're simpler and more readable than full `ChangeListener` on `statusProperty()` for most cases.

If you're having trouble with a specific event (e.g., `onEndOfMedia` not firing, or status jumping unexpectedly), share the code snippet — common issues include forgetting to call `player.play()` after `READY`, or media format problems. Let me know what exactly you're trying to achieve with events in your docking media player!
