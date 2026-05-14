# papiflyfx-docking-media — Implementation Plan

## Overview

A new Maven module `papiflyfx-docking-media` that provides a dockable media viewer for
PapiflyFX.  It plugs into the framework via `ContentFactory` / `ContentStateAdapter` and
implements `DisposableContent` for safe lifecycle management.

Dependency chain:

```
papiflyfx-docking-api
  └── papiflyfx-docking-media   (compile: api; test: docks)
        └── papiflyfx-docking-samples  (uses media in demo scenes)
```

---

## Phase 1 — Maven Module Scaffold

### 1.1 Create `papiflyfx-docking-media/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.11-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-media</artifactId>
    <name>papiflyfx-docking-media</name>
    <description>JavaFX media viewer component for PapiflyFX docking.</description>

    <dependencies>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-media</artifactId>
            <version>${javafx.version}</version>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
            <version>${javafx.version}</version>
            <classifier>${javafx.platform}</classifier>
        </dependency>

        <!-- test -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>openjfx-monocle</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>
                        --enable-native-access=javafx.graphics
                        --add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                        --add-opens=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    </argLine>
                    <systemPropertyVariables>
                        <testfx.headless>${testfx.headless}</testfx.headless>
                        <testfx.robot>${testfx.robot}</testfx.robot>
                        <testfx.platform>${testfx.platform}</testfx.platform>
                        <monocle.platform>${monocle.platform}</monocle.platform>
                        <prism.order>${prism.order}</prism.order>
                        <prism.text>${prism.text}</prism.text>
                        <java.awt.headless>${java.awt.headless}</java.awt.headless>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.2 Register in root `pom.xml`

Add `<module>papiflyfx-docking-media</module>` to the `<modules>` section before
`papiflyfx-docking-samples`.

### 1.3 Source tree to create

```
papiflyfx-docking-media/src/main/java/org/metalib/papifly/fx/media/
  api/
    MediaViewer.java
    MediaViewerFactory.java
    MediaViewerStateAdapter.java
  model/
    MediaState.java
    MediaStateCodec.java
    UrlKind.java
  player/
    MediaPlayerService.java
    ImageLoaderService.java
  stream/
    StreamUrlDetector.java
    EmbedUrlResolver.java
  viewer/
    ImageViewer.java
    VideoViewer.java
    AudioViewer.java
    EmbedViewer.java
    SvgViewer.java
    ErrorViewer.java
    ViewerFactory.java
  controls/
    TransportBar.java
    ZoomControls.java
  theme/
    MediaThemeMapper.java
```

---

## Phase 2 — Data Model

### 2.1 `UrlKind.java`

Serialised as a lowercase string in the state map so it survives JSON round-trips.

```java
package org.metalib.papifly.fx.media.model;

public enum UrlKind {
    FILE_IMAGE,
    FILE_VIDEO,
    FILE_AUDIO,
    FILE_SVG,
    STREAM_HLS,
    STREAM_HTTP_VIDEO,
    EMBED,
    RTSP,
    UNKNOWN;

    public String key() { return name().toLowerCase().replace('_', '-'); }

    public static UrlKind fromKey(String key) {
        if (key == null) return UNKNOWN;
        for (UrlKind k : values()) {
            if (k.key().equals(key)) return k;
        }
        return UNKNOWN;
    }
}
```

### 2.2 `MediaState.java`

```java
package org.metalib.papifly.fx.media.model;

public record MediaState(
    String  mediaUrl,
    UrlKind urlKind,
    long    currentTimeMs,
    double  volume,
    boolean muted,
    double  zoomLevel,
    double  panX,
    double  panY
) {
    public static MediaState empty() {
        return new MediaState(null, UrlKind.UNKNOWN, 0L, 1.0, false, 1.0, 0.0, 0.0);
    }

    public static MediaState ofUrl(String url, UrlKind kind) {
        return new MediaState(url, kind, 0L, 1.0, false, 1.0, 0.0, 0.0);
    }
}
```

### 2.3 `MediaStateCodec.java`

```java
package org.metalib.papifly.fx.media.model;

import java.util.HashMap;
import java.util.Map;

public final class MediaStateCodec {

    private MediaStateCodec() {}

    public static Map<String, Object> toMap(MediaState s) {
        Map<String, Object> m = new HashMap<>();
        if (s.mediaUrl() != null) m.put("mediaUrl", s.mediaUrl());
        m.put("urlKind",       s.urlKind().key());
        m.put("currentTimeMs", s.currentTimeMs());
        m.put("volume",        s.volume());
        m.put("muted",         s.muted());
        m.put("zoomLevel",     s.zoomLevel());
        m.put("panX",          s.panX());
        m.put("panY",          s.panY());
        return m;
    }

    public static MediaState fromMap(Map<String, Object> m) {
        if (m == null) return MediaState.empty();
        return new MediaState(
            (String)  m.getOrDefault("mediaUrl",     null),
            UrlKind.fromKey((String) m.get("urlKind")),
            toLong(          m.getOrDefault("currentTimeMs", 0L)),
            toDouble(        m.getOrDefault("volume",        1.0)),
            toBoolean(       m.getOrDefault("muted",         false)),
            toDouble(        m.getOrDefault("zoomLevel",     1.0)),
            toDouble(        m.getOrDefault("panX",          0.0)),
            toDouble(        m.getOrDefault("panY",          0.0))
        );
    }

    private static long    toLong(Object v)    { return v instanceof Number n ? n.longValue()   : 0L; }
    private static double  toDouble(Object v)  { return v instanceof Number n ? n.doubleValue() : 0.0; }
    private static boolean toBoolean(Object v) { return v instanceof Boolean b ? b : false; }
}
```

---

## Phase 3 — Services

### 3.1 `MediaPlayerService.java`

The player is kept alive across scene-graph reparenting events.

```java
package org.metalib.papifly.fx.media.player;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class MediaPlayerService {

    private final ObjectProperty<MediaPlayer> playerProperty = new SimpleObjectProperty<>();
    private final DoubleProperty volume = new SimpleDoubleProperty(1.0);
    private MediaView boundView;

    public void load(String url) {
        disposePlayer();
        Media media = new Media(url);
        MediaPlayer player = new MediaPlayer(media);
        player.volumeProperty().bindBidirectional(volume);
        playerProperty.set(player);
        if (boundView != null) {
            boundView.setMediaPlayer(player);
        }
    }

    public void bind(MediaView view) {
        this.boundView = view;
        MediaPlayer player = playerProperty.get();
        if (player != null) {
            view.setMediaPlayer(player);
        }
    }

    public void unbind(MediaView view) {
        view.setMediaPlayer(null);
        if (view == boundView) {
            boundView = null;
        }
    }

    public void play()  { withPlayer(MediaPlayer::play); }
    public void pause() { withPlayer(MediaPlayer::pause); }
    public void stop()  { withPlayer(p -> { p.stop(); p.seek(Duration.ZERO); }); }

    public void seek(Duration d) { withPlayer(p -> p.seek(d)); }

    public void stepForward() {
        withPlayer(p -> p.seek(p.getCurrentTime().add(Duration.seconds(1.0 / 30.0))));
    }

    public void stepBackward() {
        withPlayer(p -> p.seek(p.getCurrentTime().subtract(Duration.seconds(1.0 / 30.0))));
    }

    public void seekRelative(double seconds) {
        withPlayer(p -> p.seek(p.getCurrentTime().add(Duration.seconds(seconds))));
    }

    public DoubleProperty volumeProperty() { return volume; }

    public ReadOnlyObjectProperty<MediaPlayer> playerProperty() { return playerProperty; }

    public void dispose() {
        if (boundView != null) {
            boundView.setMediaPlayer(null);
            boundView = null;
        }
        disposePlayer();
    }

    private void disposePlayer() {
        MediaPlayer old = playerProperty.get();
        if (old != null) {
            old.dispose();
            playerProperty.set(null);
        }
    }

    private void withPlayer(java.util.function.Consumer<MediaPlayer> action) {
        MediaPlayer p = playerProperty.get();
        if (p != null) action.accept(p);
    }
}
```

### 3.2 `ImageLoaderService.java`

```java
package org.metalib.papifly.fx.media.player;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

public class ImageLoaderService {

    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(0.0);
    private final ObjectProperty<Exception> errorProperty = new SimpleObjectProperty<>();
    private Image current;

    public void load(String url) {
        dispose();
        current = new Image(url, 0, 0, true, true, true);
        progressProperty.bind(current.progressProperty());
        current.errorProperty().addListener((obs, o, n) -> {
            if (n) errorProperty.set(current.getException());
        });
        current.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !current.isError()) {
                imageProperty.set(current);
            }
        });
    }

    public ObjectProperty<Image> imageProperty()    { return imageProperty; }
    public ReadOnlyDoubleProperty progressProperty() { return progressProperty; }
    public ObjectProperty<Exception> errorProperty() { return errorProperty; }

    public void dispose() {
        progressProperty.unbind();
        current = null;
        imageProperty.set(null);
        errorProperty.set(null);
        progressProperty.set(0.0);
    }
}
```

---

## Phase 4 — Stream URL Detection

### 4.1 `StreamUrlDetector.java`

Classifies any URL string into a `UrlKind` without performing any network I/O.

```java
package org.metalib.papifly.fx.media.stream;

import org.metalib.papifly.fx.media.model.UrlKind;

import java.util.Locale;
import java.util.Set;

public final class StreamUrlDetector {

    private static final Set<String> EMBED_HOSTS = Set.of(
        "www.youtube.com", "youtube.com", "youtu.be",
        "player.vimeo.com", "vimeo.com",
        "www.twitch.tv", "player.twitch.tv", "twitch.tv"
    );

    private StreamUrlDetector() {}

    public static UrlKind detect(String url) {
        if (url == null || url.isBlank()) return UrlKind.UNKNOWN;

        String lower = url.toLowerCase(Locale.ROOT);

        if (lower.startsWith("rtsp://") || lower.startsWith("rtsps://")
                || lower.startsWith("rtmp://") || lower.startsWith("rtmps://")) {
            return UrlKind.RTSP;
        }

        String host = extractHost(lower);
        if (EMBED_HOSTS.contains(host)) {
            return UrlKind.EMBED;
        }

        if (lower.endsWith(".m3u8") || lower.contains(".m3u8?")) {
            return UrlKind.STREAM_HLS;
        }
        if (lower.endsWith(".svg")) return UrlKind.FILE_SVG;
        if (lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".m4v")) {
            return lower.startsWith("http") ? UrlKind.STREAM_HTTP_VIDEO : UrlKind.FILE_VIDEO;
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav")
                || lower.endsWith(".aac") || lower.endsWith(".aiff")) {
            return UrlKind.FILE_AUDIO;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".bmp")
                || lower.endsWith(".gif")) {
            return UrlKind.FILE_IMAGE;
        }
        return UrlKind.UNKNOWN;
    }

    private static String extractHost(String url) {
        int slashSlash = url.indexOf("//");
        if (slashSlash < 0) return "";
        int start = slashSlash + 2;
        int slash  = url.indexOf('/', start);
        int query  = url.indexOf('?', start);
        int end = slash < 0 ? (query < 0 ? url.length() : query)
                            : (query < 0 ? slash : Math.min(slash, query));
        return url.substring(start, end);
    }
}
```

### 4.2 `EmbedUrlResolver.java`

Converts a canonical watch URL to the embed form, and applies the Twitch `parent` workaround.

```java
package org.metalib.papifly.fx.media.stream;

public final class EmbedUrlResolver {

    private EmbedUrlResolver() {}

    public static String resolve(String url) {
        if (url == null) return url;
        String lower = url.toLowerCase(java.util.Locale.ROOT);

        // YouTube watch → embed
        if (lower.contains("youtube.com/watch")) {
            String id = extractParam(url, "v");
            if (id != null) return "https://www.youtube.com/embed/" + id;
        }
        if (lower.contains("youtu.be/")) {
            int start = lower.indexOf("youtu.be/") + 9;
            int end = url.indexOf('?', start);
            String id = end < 0 ? url.substring(start) : url.substring(start, end);
            return "https://www.youtube.com/embed/" + id;
        }

        // Vimeo watch → embed
        if (lower.contains("vimeo.com/") && !lower.contains("player.vimeo.com")) {
            int start = lower.lastIndexOf('/') + 1;
            String id = url.substring(start).split("\\?")[0];
            return "https://player.vimeo.com/video/" + id;
        }

        // Twitch channel → embed with parent workaround
        if (lower.contains("twitch.tv/")) {
            int start = lower.indexOf("twitch.tv/") + 10;
            String channel = url.substring(start).split("[/?]")[0];
            return "https://player.twitch.tv/?channel=" + channel + "&parent=localhost";
        }

        // Already an embed or unknown — return as-is
        return url;
    }

    private static String extractParam(String url, String param) {
        String key = param + "=";
        int idx = url.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int end = url.indexOf('&', start);
        return end < 0 ? url.substring(start) : url.substring(start, end);
    }
}
```

---

## Phase 5 — Theme Mapper

### `MediaThemeMapper.java`

```java
package org.metalib.papifly.fx.media.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

public final class MediaThemeMapper {

    private MediaThemeMapper() {}

    public static Paint controlBackground(Theme t) { return t.headerBackground(); }
    public static Paint controlForeground(Theme t) { return t.textColor(); }
    public static Paint accent(Theme t)            { return t.accentColor(); }
    public static Paint border(Theme t)            { return t.borderColor(); }

    public static Color toColor(Paint p) {
        return p instanceof Color c ? c : Color.GRAY;
    }
}
```

---

## Phase 6 — Transport Controls

### 6.1 `TransportBar.java`

The HUD bar sits at the bottom of the media area.  It auto-hides during playback.

```java
package org.metalib.papifly.fx.media.controls;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class TransportBar extends HBox {

    private static final double ICON_SIZE = 16.0;
    private static final double AUTO_HIDE_SECS = 2.0;

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final MediaPlayerService service;

    private final Canvas playBtn  = iconCanvas();
    private final Canvas stopBtn  = iconCanvas();
    private final Canvas muteBtn  = iconCanvas();
    private final Slider seekBar  = new Slider(0, 1, 0);
    private final Label  timeLabel = new Label("0:00 / 0:00");
    private final Slider volSlider = new Slider(0, 1, 1);

    private boolean isPlaying = false;
    private boolean seeking   = false;

    private final FadeTransition fadeIn  = new FadeTransition(Duration.millis(300), this);
    private final FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
    private final PauseTransition idleTimer = new PauseTransition(Duration.seconds(AUTO_HIDE_SECS));

    public TransportBar(MediaPlayerService service) {
        this.service = service;
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(8.0);
        setOpacity(0.0);

        HBox.setHgrow(seekBar, Priority.ALWAYS);
        volSlider.setMaxWidth(80.0);
        volSlider.valueProperty().bindBidirectional(service.volumeProperty());

        getChildren().addAll(playBtn, stopBtn, seekBar, timeLabel, muteBtn, volSlider);

        wirePlayButton();
        wireStopButton();
        wireMuteButton();
        wireSeekBar();
        wirePlayerStatus();
        wireAutoHide();
        wireTheme();
    }

    public void showFor(javafx.scene.Node parent) {
        parent.addEventFilter(MouseEvent.MOUSE_MOVED, e -> resetIdle());
        parent.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> fadeInNow());
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void applyTheme(Theme t) {
        Color bg = MediaThemeMapper.toColor(MediaThemeMapper.controlBackground(t));
        setBackground(new javafx.scene.layout.Background(
            new javafx.scene.layout.BackgroundFill(bg.deriveColor(0, 1, 1, 0.85),
                new javafx.scene.layout.CornerRadii(t.cornerRadius()),
                Insets.EMPTY)));
        timeLabel.setFont(t.contentFont());
        timeLabel.setTextFill(MediaThemeMapper.controlForeground(t));
        paintPlayIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
        paintStopIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
        paintMuteIcon(MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t)));
    }

    // --- private wiring ---

    private void wirePlayButton() {
        playBtn.setOnMouseClicked(e -> {
            MediaPlayer p = service.playerProperty().get();
            if (p == null) return;
            if (p.getStatus() == MediaPlayer.Status.PLAYING) {
                service.pause();
            } else {
                service.play();
            }
        });
    }

    private void wireStopButton() {
        stopBtn.setOnMouseClicked(e -> service.stop());
    }

    private void wireMuteButton() {
        muteBtn.setOnMouseClicked(e -> {
            MediaPlayer p = service.playerProperty().get();
            if (p != null) p.setMute(!p.isMute());
        });
    }

    private void wireSeekBar() {
        seekBar.setOnMousePressed(e  -> seeking = true);
        seekBar.setOnMouseReleased(e -> {
            seeking = false;
            service.seek(Duration.seconds(seekBar.getValue()));
        });
    }

    private void wirePlayerStatus() {
        service.playerProperty().addListener((obs, old, player) -> {
            if (old != null) {
                old.currentTimeProperty().removeListener(this::onTimeChanged);
                old.statusProperty().removeListener(this::onStatusChanged);
            }
            if (player != null) {
                player.currentTimeProperty().addListener(this::onTimeChanged);
                player.statusProperty().addListener(this::onStatusChanged);
                player.setOnReady(() -> {
                    Duration total = player.getTotalDuration();
                    seekBar.setMax(total.toSeconds());
                });
            }
        });
    }

    private void onTimeChanged(javafx.beans.value.ObservableValue<? extends Duration> obs,
                               Duration old, Duration now) {
        if (seeking) return;
        seekBar.setValue(now.toSeconds());
        MediaPlayer p = service.playerProperty().get();
        if (p != null) {
            timeLabel.setText(fmt(now) + " / " + fmt(p.getTotalDuration()));
        }
    }

    private void onStatusChanged(javafx.beans.value.ObservableValue<? extends MediaPlayer.Status> obs,
                                 MediaPlayer.Status old, MediaPlayer.Status now) {
        isPlaying = now == MediaPlayer.Status.PLAYING;
        Theme t = themeProperty.get();
        Color c = t != null
            ? MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t))
            : Color.WHITE;
        paintPlayIcon(c);
        if (isPlaying) resetIdle(); else fadeInNow();
    }

    private void wireAutoHide() {
        fadeIn.setToValue(1.0);
        fadeOut.setToValue(0.0);
        idleTimer.setOnFinished(e -> { if (isPlaying) fadeOut.playFromStart(); });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> { if (t != null) applyTheme(t); });
    }

    private void fadeInNow() {
        fadeOut.stop();
        fadeIn.playFromStart();
        resetIdle();
    }

    private void resetIdle() {
        idleTimer.stop();
        idleTimer.playFromStart();
    }

    private static String fmt(Duration d) {
        if (d == null || d.isUnknown()) return "0:00";
        int s = (int) d.toSeconds();
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private static Canvas iconCanvas() {
        return new Canvas(ICON_SIZE, ICON_SIZE);
    }

    private void paintPlayIcon(Color c) {
        GraphicsContext gc = playBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        if (isPlaying) {
            gc.fillRect(2, 2, 4, 12);
            gc.fillRect(10, 2, 4, 12);
        } else {
            gc.fillPolygon(new double[]{2, 14, 2}, new double[]{1, 8, 15}, 3);
        }
    }

    private void paintStopIcon(Color c) {
        GraphicsContext gc = stopBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillRect(2, 2, 12, 12);
    }

    private void paintMuteIcon(Color c) {
        GraphicsContext gc = muteBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        gc.setFill(c);
        gc.fillPolygon(new double[]{2, 8, 8, 2}, new double[]{5, 2, 14, 11}, 4);
        gc.fillRect(9, 5, 5, 6);
    }
}
```

### 6.2 `ZoomControls.java`

```java
package org.metalib.papifly.fx.media.controls;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

import java.util.function.Consumer;

public class ZoomControls extends HBox {

    private final Label zoomLabel = new Label("100%");
    private final Canvas plusBtn  = new Canvas(14, 14);
    private final Canvas minusBtn = new Canvas(14, 14);
    private final Canvas resetBtn = new Canvas(14, 14);

    public ZoomControls(Consumer<Double> onZoom, Runnable onReset) {
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setPadding(new Insets(2, 6, 2, 6));
        getChildren().addAll(minusBtn, zoomLabel, plusBtn, resetBtn);

        minusBtn.setOnMouseClicked(e -> onZoom.accept(-0.1));
        plusBtn.setOnMouseClicked(e  -> onZoom.accept(+0.1));
        resetBtn.setOnMouseClicked(e -> onReset.run());
    }

    public void setZoomLevel(double level) {
        zoomLabel.setText((int)(level * 100) + "%");
    }

    public void applyTheme(Theme t) {
        Color c = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t));
        zoomLabel.setFont(t.contentFont());
        zoomLabel.setTextFill(c);
        paintPlus(c);
        paintMinus(c);
        paintReset(c);
    }

    private void paintPlus(Color c) {
        GraphicsContext gc = plusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.fillRect(6, 1, 2, 12);
        gc.fillRect(1, 6, 12, 2);
    }

    private void paintMinus(Color c) {
        GraphicsContext gc = minusBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.fillRect(1, 6, 12, 2);
    }

    private void paintReset(Color c) {
        GraphicsContext gc = resetBtn.getGraphicsContext2D();
        gc.clearRect(0, 0, 14, 14);
        gc.setFill(c);
        gc.strokeOval(2, 2, 10, 10);
    }
}
```

---

## Phase 7 — Viewer Nodes

### 7.1 `ErrorViewer.java`

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class ErrorViewer extends StackPane {

    private final Canvas icon  = new Canvas(48, 48);
    private final Label  label = new Label();

    public ErrorViewer(String message) {
        setAlignment(Pos.CENTER);
        label.setText(message);
        label.setWrapText(true);
        getChildren().addAll(icon, label);
    }

    public void applyTheme(Theme t) {
        Color c = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(t));
        label.setFont(t.contentFont());
        label.setTextFill(c);
        paintBrokenIcon(c);
    }

    private void paintBrokenIcon(Color c) {
        GraphicsContext gc = icon.getGraphicsContext2D();
        gc.clearRect(0, 0, 48, 48);
        gc.setStroke(c);
        gc.setLineWidth(2.0);
        gc.strokeRect(6, 6, 36, 36);
        gc.strokeLine(6, 24, 42, 24);
        gc.strokeLine(20, 6, 20, 24);
        gc.strokeLine(6, 42, 42, 6);
    }
}
```

### 7.2 `ImageViewer.java`

Provides pan/zoom on top of an `ImageView`.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.ZoomControls;
import org.metalib.papifly.fx.media.player.ImageLoaderService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class ImageViewer extends StackPane {

    private static final double MIN_ZOOM = 0.05;
    private static final double MAX_ZOOM = 16.0;

    private final ImageLoaderService loaderService = new ImageLoaderService();
    private final ImageView imageView = new ImageView();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Scale scaleXform = new Scale(1, 1, 0, 0);
    private final Translate panXform = new Translate(0, 0);

    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    private double dragStartX, dragStartY;
    private final ZoomControls zoomControls;

    public ImageViewer() {
        setAlignment(Pos.CENTER);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getTransforms().addAll(scaleXform, panXform);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());

        zoomControls = new ZoomControls(this::adjustZoom, this::resetZoom);
        StackPane.setAlignment(zoomControls, Pos.TOP_RIGHT);

        getChildren().addAll(imageView, progress, zoomControls);

        wireLoader();
        wireZoom();
        wirePan();
        wireTheme();
    }

    public void load(String url) { loaderService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double z) { zoomLevel.set(clamp(z, MIN_ZOOM, MAX_ZOOM)); }

    public double getPanX() { return panXform.getX(); }
    public double getPanY() { return panXform.getY(); }

    public void dispose() { loaderService.dispose(); }

    private void wireLoader() {
        loaderService.imageProperty().addListener((obs, o, img) -> {
            imageView.setImage(img);
            progress.setVisible(false);
        });
        loaderService.progressProperty().addListener((obs, o, n) -> {
            progress.setVisible(n.doubleValue() < 1.0);
            progress.setProgress(n.doubleValue());
        });
        loaderService.errorProperty().addListener((obs, o, ex) -> {
            if (ex != null) progress.setVisible(false);
        });
    }

    private void wireZoom() {
        zoomLevel.addListener((obs, o, n) -> {
            double z = n.doubleValue();
            scaleXform.setX(z);
            scaleXform.setY(z);
            zoomControls.setZoomLevel(z);
        });

        setOnScroll((ScrollEvent e) -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            adjustZoom(factor - 1.0);
            e.consume();
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                resetZoom();
            }
        });
    }

    private void wirePan() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getSceneX() - panXform.getX();
                dragStartY = e.getSceneY() - panXform.getY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                panXform.setX(e.getSceneX() - dragStartX);
                panXform.setY(e.getSceneY() - dragStartY);
            }
        });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                    t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY,
                    javafx.geometry.Insets.EMPTY)));
            zoomControls.applyTheme(t);
        });
    }

    private void adjustZoom(double delta) {
        setZoomLevel(zoomLevel.get() + delta * zoomLevel.get());
    }

    private void resetZoom() {
        zoomLevel.set(1.0);
        panXform.setX(0);
        panXform.setY(0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
```

### 7.3 `VideoViewer.java`

Detached-controller pattern: `MediaPlayerService` owns the `MediaPlayer`.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;

public class VideoViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final MediaView mediaView = new MediaView();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public VideoViewer() {
        setAlignment(Pos.BOTTOM_CENTER);

        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        mediaView.fitWidthProperty().bind(widthProperty());
        mediaView.fitHeightProperty().bind(heightProperty());

        StackPane.setAlignment(transportBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(transportBar, new Insets(0, 8, 8, 8));

        getChildren().addAll(mediaView, transportBar);

        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        wireSceneReparent();
        wireKeyboard();
        wireTheme();
        wireVisibility();
    }

    public void load(String url) {
        playerService.load(url);
        playerService.bind(mediaView);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public long getCurrentTimeMs() {
        var p = playerService.playerProperty().get();
        return p != null ? (long) p.getCurrentTime().toMillis() : 0L;
    }

    public double getVolume() {
        return playerService.volumeProperty().get();
    }

    public void applyPlaybackState(long timeMs, double volume, boolean muted) {
        playerService.playerProperty().addListener((obs, o, player) -> {
            if (player != null) {
                player.setOnReady(() -> {
                    player.seek(javafx.util.Duration.millis(timeMs));
                    player.volumeProperty().set(volume);
                    player.setMute(muted);
                });
            }
        });
    }

    public void dispose() {
        playerService.dispose();
    }

    private void wireSceneReparent() {
        sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                playerService.bind(mediaView);
            } else {
                playerService.unbind(mediaView);
            }
        });
    }

    private void wireKeyboard() {
        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE -> { playerService.playerProperty().get() != null
                        && playerService.playerProperty().get().getStatus()
                            == javafx.scene.media.MediaPlayer.Status.PLAYING
                    ? playerService.pause() : playerService.play();
                    e.consume(); }
                case M     -> { var p = playerService.playerProperty().get();
                                if (p != null) p.setMute(!p.isMute()); e.consume(); }
                case LEFT  -> { playerService.seekRelative(-5); e.consume(); }
                case RIGHT -> { playerService.seekRelative(+5); e.consume(); }
                case J     -> { playerService.stepBackward();   e.consume(); }
                case L     -> { playerService.stepForward();    e.consume(); }
                default    -> {}
            }
        });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                    t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY,
                    Insets.EMPTY)));
        });
    }

    private void wireVisibility() {
        visibleProperty().addListener((obs, o, visible) -> {
            var p = playerService.playerProperty().get();
            if (p != null) p.setMute(!visible);
        });
    }
}
```

### 7.4 `AudioViewer.java`

Minimal audio viewer with waveform placeholder and transport bar.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

public class AudioViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final Canvas waveformPlaceholder = new Canvas(200, 80);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public AudioViewer() {
        setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(transportBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(transportBar, new Insets(0, 8, 8, 8));

        getChildren().addAll(waveformPlaceholder, transportBar);
        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        waveformPlaceholder.widthProperty().bind(widthProperty().multiply(0.8));
        wireTheme();
    }

    public void load(String url) { playerService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public long getCurrentTimeMs() {
        var p = playerService.playerProperty().get();
        return p != null ? (long) p.getCurrentTime().toMillis() : 0L;
    }

    public void applyPlaybackState(long timeMs, double volume, boolean muted) {
        playerService.playerProperty().addListener((obs, o, player) -> {
            if (player != null) {
                player.setOnReady(() -> {
                    player.seek(javafx.util.Duration.millis(timeMs));
                    player.volumeProperty().set(volume);
                    player.setMute(muted);
                });
            }
        });
    }

    public void dispose() { playerService.dispose(); }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
            paintWaveformPlaceholder(MediaThemeMapper.toColor(MediaThemeMapper.accent(t)));
        });
    }

    private void paintWaveformPlaceholder(Color c) {
        double w = waveformPlaceholder.getWidth();
        double h = waveformPlaceholder.getHeight();
        GraphicsContext gc = waveformPlaceholder.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setStroke(c.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.5);
        double mid = h / 2.0;
        double step = w / 60.0;
        for (int i = 0; i < 60; i++) {
            double amp = mid * (0.2 + 0.8 * Math.abs(Math.sin(i * 0.42)));
            gc.strokeLine(i * step, mid - amp, i * step, mid + amp);
        }
    }
}
```

### 7.5 `EmbedViewer.java`

`WebView`-based viewer for YouTube, Vimeo, and Twitch streams.
`EmbedUrlResolver` converts canonical watch URLs to embed form before loading.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.stream.EmbedUrlResolver;

public class EmbedViewer extends StackPane {

    private final WebView webView = new WebView();
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public EmbedViewer(String url) {
        webView.setContextMenuEnabled(false);
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        getChildren().add(webView);
        wireTheme();
        load(url);
    }

    public void load(String url) {
        String embedUrl = EmbedUrlResolver.resolve(url);
        webView.getEngine().load(embedUrl);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public void dispose() {
        webView.getEngine().load(null);
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }
}
```

### 7.6 `SvgViewer.java`

Uses `SVGPath` for single-path SVGs; falls back to `WebView` for multi-element SVGs.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SvgViewer extends StackPane {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    public SvgViewer(String url) {
        setAlignment(Pos.CENTER);
        load(url);
        wireTheme();
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    private void load(String url) {
        String content = readContent(url);
        if (content == null) {
            getChildren().add(new ErrorViewer("Cannot read: " + url));
            return;
        }
        String pathData = extractSinglePath(content);
        if (pathData != null) {
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.scaleXProperty().bind(widthProperty().divide(100));
            svgPath.scaleYProperty().bind(heightProperty().divide(100));
            getChildren().add(svgPath);
            themeProperty.addListener((obs, o, t) -> {
                if (t != null) svgPath.setFill(MediaThemeMapper.controlForeground(t));
            });
        } else {
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.getEngine().load(url);
            webView.prefWidthProperty().bind(widthProperty());
            webView.prefHeightProperty().bind(heightProperty());
            getChildren().add(webView);
        }
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }

    private static String readContent(String url) {
        try {
            if (url.startsWith("file:")) {
                return Files.readString(Path.of(java.net.URI.create(url)));
            }
        } catch (IOException | IllegalArgumentException ignored) {}
        return null;
    }

    private static String extractSinglePath(String svg) {
        int dIdx = svg.indexOf(" d=\"");
        if (dIdx < 0) dIdx = svg.indexOf("\nd=\"");
        if (dIdx < 0) return null;
        int start = svg.indexOf('"', dIdx + 3) + 1;
        int end   = svg.indexOf('"', start);
        if (start <= 0 || end <= start) return null;
        // accept only if there is a single <path element
        int count = 0;
        int pos = 0;
        while ((pos = svg.indexOf("<path", pos)) >= 0) { count++; pos++; }
        return count == 1 ? svg.substring(start, end) : null;
    }
}
```

### 7.7 `ViewerFactory.java`

Dispatches by `UrlKind` — classification is done once in `MediaViewer.loadMedia()` and
passed in so the factory never re-parses the URL.

```java
package org.metalib.papifly.fx.media.viewer;

import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.media.model.UrlKind;

public final class ViewerFactory {

    private ViewerFactory() {}

    public static StackPane create(String url, UrlKind kind) {
        if (url == null || url.isBlank() || kind == UrlKind.UNKNOWN) {
            return new ErrorViewer("Unsupported or missing media URL: " + url);
        }
        return switch (kind) {
            case FILE_IMAGE                       -> new ImageViewer();
            case FILE_VIDEO, STREAM_HLS,
                 STREAM_HTTP_VIDEO               -> new VideoViewer();
            case FILE_AUDIO                       -> new AudioViewer();
            case FILE_SVG                         -> new SvgViewer(url);
            case EMBED                            -> new EmbedViewer(url);
            case RTSP                             -> createRtspViewer(url);
            default                               -> new ErrorViewer("Unsupported: " + url);
        };
    }

    private static StackPane createRtspViewer(String url) {
        // vlcj integration is optional; show a descriptive error when absent
        try {
            Class.forName("uk.co.caprica.vlcj.player.base.MediaPlayer");
            // TODO: return new VlcjVideoViewer(url);
            return new ErrorViewer("vlcj viewer not yet implemented for: " + url);
        } catch (ClassNotFoundException e) {
            return new ErrorViewer("RTSP/RTMP requires vlcj on the classpath.\n" + url);
        }
    }
}
```

---

## Phase 8 — `MediaViewer` (Public API)

`MediaViewer` calls `StreamUrlDetector.detect()` once per `loadMedia()` call, stores the
`UrlKind` in state, and passes it to `ViewerFactory.create()`.

```java
package org.metalib.papifly.fx.media.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.model.MediaState;
import org.metalib.papifly.fx.media.model.UrlKind;
import org.metalib.papifly.fx.media.stream.StreamUrlDetector;
import org.metalib.papifly.fx.media.viewer.AudioViewer;
import org.metalib.papifly.fx.media.viewer.EmbedViewer;
import org.metalib.papifly.fx.media.viewer.ErrorViewer;
import org.metalib.papifly.fx.media.viewer.ImageViewer;
import org.metalib.papifly.fx.media.viewer.SvgViewer;
import org.metalib.papifly.fx.media.viewer.VideoViewer;
import org.metalib.papifly.fx.media.viewer.ViewerFactory;

public class MediaViewer extends StackPane implements DisposableContent {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private StackPane activeViewer;
    private String currentUrl;
    private UrlKind currentKind = UrlKind.UNKNOWN;

    public MediaViewer() {
        wireTheme();
    }

    public void loadMedia(String url) {
        disposeViewer();
        currentUrl = url;
        currentKind = StreamUrlDetector.detect(url);
        activeViewer = ViewerFactory.create(url, currentKind);
        bindViewerTheme();
        loadUrlInViewer(url);
        getChildren().setAll(activeViewer);
    }

    public void applyState(MediaState state) {
        if (state.mediaUrl() != null && !state.mediaUrl().equals(currentUrl)) {
            disposeViewer();
            currentUrl = state.mediaUrl();
            currentKind = state.urlKind() != UrlKind.UNKNOWN
                ? state.urlKind()
                : StreamUrlDetector.detect(currentUrl);
            activeViewer = ViewerFactory.create(currentUrl, currentKind);
            bindViewerTheme();
            loadUrlInViewer(currentUrl);
            getChildren().setAll(activeViewer);
        }
        if (activeViewer instanceof VideoViewer vv) {
            vv.applyPlaybackState(state.currentTimeMs(), state.volume(), state.muted());
        }
        if (activeViewer instanceof AudioViewer av) {
            av.applyPlaybackState(state.currentTimeMs(), state.volume(), state.muted());
        }
        if (activeViewer instanceof ImageViewer iv) {
            iv.setZoomLevel(state.zoomLevel());
        }
    }

    public void setZoomLevel(double level) {
        if (activeViewer instanceof ImageViewer iv) iv.setZoomLevel(level);
    }

    public MediaState captureState() {
        long timeMs = 0L;
        double volume = 1.0;
        boolean muted = false;
        double zoom = 1.0;
        double panX = 0.0;
        double panY = 0.0;
        if (activeViewer instanceof VideoViewer vv) {
            timeMs = vv.getCurrentTimeMs();
            volume = vv.getVolume();
        }
        if (activeViewer instanceof AudioViewer av) {
            timeMs = av.getCurrentTimeMs();
        }
        if (activeViewer instanceof ImageViewer iv) {
            zoom = iv.getZoomLevel();
            panX = iv.getPanX();
            panY = iv.getPanY();
        }
        return new MediaState(currentUrl, currentKind, timeMs, volume, muted, zoom, panX, panY);
    }

    public void bindThemeProperty(ObjectProperty<Theme> external) {
        themeProperty.bind(external);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    @Override
    public void dispose() {
        disposeViewer();
    }

    private void disposeViewer() {
        if (activeViewer instanceof VideoViewer vv) vv.dispose();
        if (activeViewer instanceof AudioViewer av) av.dispose();
        if (activeViewer instanceof ImageViewer  iv) iv.dispose();
        if (activeViewer instanceof EmbedViewer  ev) ev.dispose();
        activeViewer = null;
        getChildren().clear();
    }

    private void bindViewerTheme() {
        if (activeViewer instanceof VideoViewer vv) vv.themeProperty().bind(themeProperty);
        if (activeViewer instanceof AudioViewer av) av.themeProperty().bind(themeProperty);
        if (activeViewer instanceof ImageViewer  iv) iv.themeProperty().bind(themeProperty);
        if (activeViewer instanceof SvgViewer   sv) sv.themeProperty().bind(themeProperty);
        if (activeViewer instanceof EmbedViewer ev) ev.themeProperty().bind(themeProperty);
        if (activeViewer instanceof ErrorViewer er)
            themeProperty.addListener((obs, o, t) -> { if (t != null) er.applyTheme(t); });
    }

    private void loadUrlInViewer(String url) {
        if (activeViewer instanceof VideoViewer vv) vv.load(url);
        if (activeViewer instanceof AudioViewer av) av.load(url);
        if (activeViewer instanceof ImageViewer  iv) iv.load(url);
        // EmbedViewer and SvgViewer load in their constructors
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        });
    }
}
```

---

## Phase 9 — Factory & State Adapter

### 9.1 `MediaViewerFactory.java`

```java
package org.metalib.papifly.fx.media.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

public class MediaViewerFactory implements ContentFactory {

    public static final String FACTORY_ID = "media-viewer";

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) return null;
        return new MediaViewer();
    }
}
```

### 9.2 `MediaViewerStateAdapter.java`

```java
package org.metalib.papifly.fx.media.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.media.model.MediaState;
import org.metalib.papifly.fx.media.model.MediaStateCodec;

import java.util.Map;

public class MediaViewerStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() { return MediaViewerFactory.FACTORY_ID; }

    @Override
    public int getVersion() { return VERSION; }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof MediaViewer mv)) return Map.of();
        return MediaStateCodec.toMap(mv.captureState());
    }

    @Override
    public Node restore(LeafContentData content) {
        MediaViewer mv = new MediaViewer();
        if (content != null && content.state() != null) {
            MediaState state = MediaStateCodec.fromMap(content.state());
            mv.applyState(state);
        }
        return mv;
    }
}
```

---

## Phase 10 — Samples Integration

### 10.1 pom.xml change in `papiflyfx-docking-samples`

Add to the existing dependencies:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-media</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 10.2 Sample package

```
papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/media/
  ImageViewerSample.java
  VideoPlayerSample.java
  SplitMediaSample.java
  HlsStreamSample.java
  YouTubeEmbedSample.java
  MediaPersistSample.java
```

### 10.3 `ImageViewerSample.java` — standalone, zoom & pan demo

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class ImageViewerSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Image Viewer"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        viewer.loadMedia(url);
        return viewer;
    }
}
```

Bundled resource: `papiflyfx-docking-media/src/main/resources/sample-media/sample.png`
(a ~200 kB high-res PNG so zoom/pan is meaningful).

### 10.4 `VideoPlayerSample.java` — docked, transport bar demo

Uses a `DockManager` so the tab bar and float/minimize controls are visible.

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.media.api.MediaViewerFactory;
import org.metalib.papifly.fx.media.api.MediaViewerStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

public class VideoPlayerSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Video Player"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new MediaViewerStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new MediaViewerFactory());

        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        viewer.loadMedia(url);

        var leaf = dm.createLeaf("sample.mp4", viewer);
        leaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup group = dm.createTabGroup();
        group.addLeaf(leaf);
        dm.setRoot(group);

        return dm.getRootPane();
    }
}
```

Bundled resource: `papiflyfx-docking-media/src/main/resources/sample-media/sample.mp4`
(a ~2 MB short MP4 clip).

### 10.5 `SplitMediaSample.java` — split pane, image + video side by side

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.media.api.MediaViewerFactory;
import org.metalib.papifly.fx.media.api.MediaViewerStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

public class SplitMediaSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Split: Image + Video"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new MediaViewerStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new MediaViewerFactory());

        MediaViewer imageViewer = new MediaViewer();
        imageViewer.bindThemeProperty(themeProperty);
        imageViewer.loadMedia(getClass().getResource("/sample-media/sample.png").toExternalForm());

        MediaViewer videoViewer = new MediaViewer();
        videoViewer.bindThemeProperty(themeProperty);
        videoViewer.loadMedia(getClass().getResource("/sample-media/sample.mp4").toExternalForm());

        var imageLeaf = dm.createLeaf("Image", imageViewer);
        imageLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        var videoLeaf = dm.createLeaf("Video", videoViewer);
        videoLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup leftGroup = dm.createTabGroup();
        leftGroup.addLeaf(imageLeaf);

        DockTabGroup rightGroup = dm.createTabGroup();
        rightGroup.addLeaf(videoLeaf);

        dm.setRoot(dm.createHorizontalSplit(leftGroup, rightGroup, 0.5));

        return dm.getRootPane();
    }
}
```

### 10.6 `HlsStreamSample.java` — HLS live/adaptive stream

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class HlsStreamSample implements SampleScene {

    // Public domain Apple HLS test stream
    private static final String HLS_URL =
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8";

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "HLS Stream"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        viewer.loadMedia(HLS_URL);
        return viewer;
    }
}
```

### 10.7 `YouTubeEmbedSample.java` — YouTube embed via `EmbedViewer`

Shows URL auto-conversion: a canonical `youtube.com/watch?v=` URL is passed to
`MediaViewer.loadMedia()`; `StreamUrlDetector` classifies it as `EMBED`; `EmbedUrlResolver`
converts it to the embed form before loading the `WebView`.

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.samples.SampleScene;

public class YouTubeEmbedSample implements SampleScene {

    private static final String YOUTUBE_WATCH =
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "YouTube Embed"; }

    @Override
    public Node build(Stage stage, ObjectProperty<Theme> themeProperty) {
        MediaViewer viewer = new MediaViewer();
        viewer.bindThemeProperty(themeProperty);
        viewer.loadMedia(YOUTUBE_WATCH);   // auto-converted to embed URL internally
        return viewer;
    }
}
```

### 10.8 `MediaPersistSample.java` — session save/restore with media state

Demonstrates that `currentTimeMs`, `zoomLevel`, and `panX`/`panY` survive a full
`DockManager` session round-trip.  Uses two side-by-side tabs: image (with zoom set
to 2.0×) and video (with a seek position pre-set).

```java
package org.metalib.papifly.fx.samples.media;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.api.MediaViewer;
import org.metalib.papifly.fx.media.api.MediaViewerFactory;
import org.metalib.papifly.fx.media.api.MediaViewerStateAdapter;
import org.metalib.papifly.fx.samples.SampleScene;

public class MediaPersistSample implements SampleScene {

    @Override
    public String category() { return "Media"; }

    @Override
    public String title() { return "Persist & Restore"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = buildDockManager(ownerStage, themeProperty);

        Button saveBtn = new Button("Save Session");
        Button loadBtn = new Button("Load Session");
        saveBtn.setDisable(false);
        loadBtn.setDisable(true);

        final String[] savedSession = {null};

        saveBtn.setOnAction(e -> {
            savedSession[0] = dm.saveSessionToString();
            loadBtn.setDisable(false);
        });
        loadBtn.setOnAction(e -> {
            if (savedSession[0] != null) {
                dm.restoreSessionFromString(savedSession[0]);
            }
        });

        HBox toolbar = new HBox(8, saveBtn, loadBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(6, 10, 6, 10));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(dm.getRootPane());
        return root;
    }

    private DockManager buildDockManager(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        DockManager dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        ContentStateRegistry registry = new ContentStateRegistry();
        registry.register(new MediaViewerStateAdapter());
        dm.setContentStateRegistry(registry);
        dm.setContentFactory(new MediaViewerFactory());

        MediaViewer imageViewer = new MediaViewer();
        imageViewer.bindThemeProperty(themeProperty);
        imageViewer.loadMedia(getClass().getResource("/sample-media/sample.png").toExternalForm());
        imageViewer.setZoomLevel(2.0);  // pre-set zoom; captured by saveSessionToString()

        MediaViewer videoViewer = new MediaViewer();
        videoViewer.bindThemeProperty(themeProperty);
        videoViewer.loadMedia(getClass().getResource("/sample-media/sample.mp4").toExternalForm());

        var imageLeaf = dm.createLeaf("Image (zoom 2×)", imageViewer);
        imageLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        var videoLeaf = dm.createLeaf("Video", videoViewer);
        videoLeaf.setContentFactoryId(MediaViewerFactory.FACTORY_ID);

        DockTabGroup group = dm.createTabGroup();
        group.addLeaf(imageLeaf);
        group.addLeaf(videoLeaf);
        dm.setRoot(group);

        return dm;
    }
}
```

### 10.9 Register all demos in `SampleCatalog`

```java
// existing entries ...
new TreeViewSample(),
new TreeViewNodeInfoSample(),
// media entries
new ImageViewerSample(),
new VideoPlayerSample(),
new SplitMediaSample(),
new HlsStreamSample(),
new YouTubeEmbedSample(),
new MediaPersistSample()
```

The nav tree will automatically group them under the `"Media"` category header because
`SamplesApp.buildNavigationRoot()` groups by `category()` changes.

---

## Phase 11 — Tests

### Unit test: `StreamUrlDetectorTest.java`

```java
package org.metalib.papifly.fx.media.stream;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.media.model.UrlKind;

import static org.junit.jupiter.api.Assertions.*;

class StreamUrlDetectorTest {

    @Test void detectsM3u8AsHls() {
        assertEquals(UrlKind.STREAM_HLS, StreamUrlDetector.detect("https://cdn.example.com/stream.m3u8"));
    }

    @Test void detectsYouTubeAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://www.youtube.com/watch?v=abc123"));
    }

    @Test void detectsVimeoAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://vimeo.com/123456789"));
    }

    @Test void detectsTwitchAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://www.twitch.tv/somechannel"));
    }

    @Test void detectsRtspAsRtsp() {
        assertEquals(UrlKind.RTSP, StreamUrlDetector.detect("rtsp://192.168.1.10/camera"));
    }

    @Test void detectsRtmpAsRtsp() {
        assertEquals(UrlKind.RTSP, StreamUrlDetector.detect("rtmp://live.example.com/stream"));
    }

    @Test void detectsMp4HttpAsStreamVideo() {
        assertEquals(UrlKind.STREAM_HTTP_VIDEO, StreamUrlDetector.detect("https://example.com/video.mp4"));
    }

    @Test void detectsLocalMp4AsFileVideo() {
        assertEquals(UrlKind.FILE_VIDEO, StreamUrlDetector.detect("file:///home/user/video.mp4"));
    }

    @Test void detectsPngAsFileImage() {
        assertEquals(UrlKind.FILE_IMAGE, StreamUrlDetector.detect("file:///home/user/photo.png"));
    }

    @Test void detectsSvgAsFileSvg() {
        assertEquals(UrlKind.FILE_SVG, StreamUrlDetector.detect("file:///icons/logo.svg"));
    }

    @Test void nullReturnsUnknown() {
        assertEquals(UrlKind.UNKNOWN, StreamUrlDetector.detect(null));
    }

    @Test void blankReturnsUnknown() {
        assertEquals(UrlKind.UNKNOWN, StreamUrlDetector.detect("  "));
    }
}
```

### Unit test: `EmbedUrlResolverTest.java`

```java
package org.metalib.papifly.fx.media.stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmbedUrlResolverTest {

    @Test void resolvesYouTubeWatchUrl() {
        String result = EmbedUrlResolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesYouTubeShortUrl() {
        String result = EmbedUrlResolver.resolve("https://youtu.be/dQw4w9WgXcQ");
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesVimeoUrl() {
        String result = EmbedUrlResolver.resolve("https://vimeo.com/123456789");
        assertEquals("https://player.vimeo.com/video/123456789", result);
    }

    @Test void resolvesTwitchChannelUrl() {
        String result = EmbedUrlResolver.resolve("https://www.twitch.tv/somechannel");
        assertTrue(result.contains("player.twitch.tv"));
        assertTrue(result.contains("channel=somechannel"));
        assertTrue(result.contains("parent=localhost"));
    }

    @Test void alreadyEmbedUrlPassedThrough() {
        String embed = "https://www.youtube.com/embed/abc";
        assertEquals(embed, EmbedUrlResolver.resolve(embed));
    }
}
```

### Unit test: `MediaStateCodecTest.java`

```java
package org.metalib.papifly.fx.media.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaStateCodecTest {

    @Test
    void roundTripFullState() {
        MediaState original = new MediaState(
            "file:///video.mp4", UrlKind.FILE_VIDEO, 5000L, 0.75, true, 2.0, -10.0, 5.0);
        MediaState restored = MediaStateCodec.fromMap(MediaStateCodec.toMap(original));
        assertEquals(original, restored);
    }

    @Test
    void roundTripEmbedKind() {
        MediaState original = new MediaState(
            "https://www.youtube.com/embed/abc", UrlKind.EMBED, 0L, 1.0, false, 1.0, 0.0, 0.0);
        MediaState restored = MediaStateCodec.fromMap(MediaStateCodec.toMap(original));
        assertEquals(UrlKind.EMBED, restored.urlKind());
    }

    @Test
    void fromNullMapReturnsEmpty() {
        MediaState s = MediaStateCodec.fromMap(null);
        assertEquals(MediaState.empty(), s);
    }

    @Test
    void missingKeysUseDefaults() {
        MediaState s = MediaStateCodec.fromMap(java.util.Map.of());
        assertEquals(1.0, s.volume());
        assertEquals(1.0, s.zoomLevel());
        assertFalse(s.muted());
        assertEquals(UrlKind.UNKNOWN, s.urlKind());
    }
}
```

### UI smoke test: `MediaViewerFxTest.java`

```java
package org.metalib.papifly.fx.media.api;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class MediaViewerFxTest {

    private MediaViewer viewer;

    @Start
    void start(Stage stage) {
        viewer = new MediaViewer();
        stage.setScene(new Scene(viewer, 400, 300));
        stage.show();
    }

    @Test
    void createsWithoutError() {
        assertNotNull(viewer);
    }

    @Test
    void loadUnknownFormatShowsError() {
        viewer.loadMedia("file:///unknown.xyz");
        assertFalse(viewer.getChildren().isEmpty());
    }

    @Test
    void disposeDoesNotThrow() {
        viewer.loadMedia("file:///sample.png");
        assertDoesNotThrow(() -> viewer.dispose());
    }
}
```

---

## Summary Checklist

| Phase | Deliverable | Status |
|-------|------------|--------|
| 1 | Maven module scaffold, pom.xml, root module registration | completed |
| 2 | `UrlKind`, `MediaState`, `MediaStateCodec` | completed |
| 3 | `MediaPlayerService`, `ImageLoaderService` | completed |
| 4 | `StreamUrlDetector`, `EmbedUrlResolver` | completed |
| 5 | `MediaThemeMapper` | completed |
| 6 | `TransportBar`, `ZoomControls` | completed |
| 7 | `ErrorViewer`, `ImageViewer`, `VideoViewer`, `AudioViewer`, `EmbedViewer`, `SvgViewer`, `ViewerFactory` | completed |
| 8 | `MediaViewer` (public StackPane + DisposableContent) | completed |
| 9 | `MediaViewerFactory`, `MediaViewerStateAdapter` | completed |
| 10 | 6 sample classes (`ImageViewerSample`, `VideoPlayerSample`, `SplitMediaSample`, `HlsStreamSample`, `YouTubeEmbedSample`, `MediaPersistSample`), bundled resources, `SampleCatalog` registration | completed |
| 11 | Unit tests (`MediaStateCodecTest`, `StreamUrlDetectorTest`, `EmbedUrlResolverTest`), UI tests (`MediaViewerFxTest`) | completed |
