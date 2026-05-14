package org.metalib.papifly.fx.media.viewer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;
import org.metalib.papifly.fx.ui.UiMetrics;

public class AudioViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final Canvas waveformPlaceholder = new Canvas(200, 80);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private long pendingTimeMs = 0L;
    private double pendingVolume = 1.0;
    private boolean pendingMuted = false;
    private boolean playbackStatePending;
    private MediaPlayer restorePlayer;
    private ChangeListener<MediaPlayer.Status> restoreStatusListener;
    private boolean errorState = false;

    public AudioViewer() {
        setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(transportBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(transportBar, new Insets(0, UiMetrics.SPACE_2, UiMetrics.SPACE_2, UiMetrics.SPACE_2));

        getChildren().addAll(waveformPlaceholder, transportBar);
        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        waveformPlaceholder.widthProperty().bind(widthProperty().multiply(0.8));
        wirePlayerStateRestore();
        wireTheme();
        wireError();
    }

    public void load(String url) { playerService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public boolean isErrorState() { return errorState; }

    public long getCurrentTimeMs() {
        MediaPlayer player = playerService.playerProperty().get();
        return player != null ? (long) player.getCurrentTime().toMillis() : 0L;
    }

    public void applyPlaybackState(long timeMs, double volume, boolean muted) {
        pendingTimeMs = Math.max(0L, timeMs);
        pendingVolume = volume;
        pendingMuted = muted;
        playbackStatePending = true;
        MediaPlayer player = playerService.playerProperty().get();
        if (player != null) {
            applyPendingPlaybackState(player);
        }
    }

    public void dispose() {
        detachRestoreListener();
        playerService.dispose();
    }

    private void wireTheme() {
        themeProperty.addListener((obs, o, t) -> {
            if (t == null) return;
            setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(t.background(),
                    javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
            paintWaveformPlaceholder(MediaThemeMapper.toColor(MediaThemeMapper.accent(t)));
        });
    }

    private void wireError() {
        playerService.setOnError(() -> {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> errorState = true);
            } else {
                errorState = true;
            }
        });
    }

    private void wirePlayerStateRestore() {
        playerService.playerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            detachRestoreListener();
            if (newPlayer == null) return;
            restorePlayer = newPlayer;
            restoreStatusListener = (statusObs, oldStatus, newStatus) -> {
                if (newStatus == MediaPlayer.Status.READY) {
                    applyPendingPlaybackState(newPlayer);
                }
            };
            newPlayer.statusProperty().addListener(restoreStatusListener);
            applyPendingPlaybackState(newPlayer);
        });
    }

    private void applyPendingPlaybackState(MediaPlayer player) {
        if (!playbackStatePending) return;
        MediaPlayer.Status status = player.getStatus();
        if (status != MediaPlayer.Status.READY
            && status != MediaPlayer.Status.PAUSED
            && status != MediaPlayer.Status.PLAYING
            && status != MediaPlayer.Status.STOPPED) {
            return;
        }
        player.seek(Duration.millis(pendingTimeMs));
        player.volumeProperty().set(pendingVolume);
        player.setMute(pendingMuted);
        playbackStatePending = false;
    }

    private void detachRestoreListener() {
        if (restorePlayer != null && restoreStatusListener != null) {
            restorePlayer.statusProperty().removeListener(restoreStatusListener);
        }
        restorePlayer = null;
        restoreStatusListener = null;
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

    TransportBar transportBarForTesting() {
        return transportBar;
    }

    MediaPlayer playerForTesting() {
        return playerService.playerProperty().get();
    }
}
