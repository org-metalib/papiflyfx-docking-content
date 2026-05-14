package org.metalib.papifly.fx.media.controls;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.ArcType;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;
import org.metalib.papifly.fx.ui.UiMetrics;

public class TransportBar extends HBox {

    public enum PlaybackState {
        START,
        READY,
        PLAYING,
        PAUSED,
        STOPPED,
        STALLED,
        ENDED,
        ERROR
    }

    private static final double ICON_SIZE = UiMetrics.SPACE_4;
    private static final double AUTO_HIDE_SECS = 3.0;
    private static final long LIVE_SCRUB_INTERVAL_NANOS = 40_000_000L;
    private static final double LIVE_SCRUB_DELTA_SECONDS = 0.04;

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final MediaPlayerService service;

    private final Canvas playBtn = iconCanvas();
    private final Canvas stopBtn = iconCanvas();
    private final Canvas muteBtn = iconCanvas();
    private final Slider seekBar = new Slider(0, 1, 0);
    private final Label timeLabel = new Label("0:00 / 0:00");
    private final Slider volSlider = new Slider(0, 1, 1);
    private final Label stateLabel = new Label();

    private boolean seeking;
    private boolean muted;
    private MediaPlayer currentPlayer;
    private double lastScrubSeekSeconds = Double.NaN;
    private long lastScrubSeekNanos;

    private final ReadOnlyObjectWrapper<PlaybackState> playbackState =
        new ReadOnlyObjectWrapper<>(PlaybackState.START);
    private final ReadOnlyBooleanWrapper controlsVisible =
        new ReadOnlyBooleanWrapper(true);

    private final FadeTransition fadeIn = new FadeTransition(Duration.millis(220), this);
    private final FadeTransition fadeOut = new FadeTransition(Duration.millis(350), this);
    private final PauseTransition idleTimer = new PauseTransition(Duration.seconds(AUTO_HIDE_SECS));

    private final ChangeListener<Duration> timeListener = this::onTimeChanged;
    private final ChangeListener<MediaPlayer.Status> statusListener = this::onStatusChanged;
    private final ChangeListener<Boolean> muteListener = (obs, oldValue, newValue) -> {
        muted = Boolean.TRUE.equals(newValue);
        repaintIcons();
    };

    public TransportBar(MediaPlayerService service) {
        this.service = service;
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_3, UiMetrics.SPACE_1, UiMetrics.SPACE_3));
        setSpacing(UiMetrics.SPACE_2);
        setFillHeight(false);
        setMinHeight(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
        setPickOnBounds(false);
        setOpacity(1.0);

        seekBar.setMinWidth(UiMetrics.SPACE_6 * 4.0);
        timeLabel.setMinWidth(UiMetrics.SPACE_6 * 4.0);
        volSlider.setMaxWidth(UiMetrics.SPACE_5 * 4.0);

        stateLabel.setVisible(false);
        stateLabel.setManaged(false);
        stateLabel.setMinWidth(UiMetrics.SPACE_5 * 4.0);

        HBox.setHgrow(seekBar, Priority.ALWAYS);
        volSlider.valueProperty().bindBidirectional(service.volumeProperty());

        getChildren().addAll(playBtn, stopBtn, seekBar, timeLabel, muteBtn, volSlider, stateLabel);

        wirePlayButton();
        wireStopButton();
        wireMuteButton();
        wireSeekBar();
        wirePlayerLifecycle();
        wireAutoHide();
        wireTheme();
        repaintIcons();
    }

    public void showFor(javafx.scene.Node parent) {
        parent.addEventFilter(MouseEvent.MOUSE_MOVED, e -> onUserInteraction());
        parent.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> onUserInteraction());
        parent.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> onUserInteraction());
        parent.addEventFilter(MouseEvent.MOUSE_EXITED, e -> onMouseExitedParent());
    }

    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    public ReadOnlyObjectProperty<PlaybackState> playbackStateProperty() {
        return playbackState.getReadOnlyProperty();
    }

    public PlaybackState getPlaybackState() {
        return playbackState.get();
    }

    public ReadOnlyBooleanProperty controlsVisibleProperty() {
        return controlsVisible.getReadOnlyProperty();
    }

    public boolean isControlsVisible() {
        return controlsVisible.get();
    }

    public void showControlsNow() {
        showNow();
    }

    public void triggerIdleTimeout() {
        if (playbackState.get() != PlaybackState.PLAYING) return;
        hideImmediately();
        Platform.runLater(() -> {
            if (playbackState.get() == PlaybackState.PLAYING) {
                hideImmediately();
            }
        });
    }

    public void applyTheme(Theme theme) {
        Color bg = MediaThemeMapper.toColor(MediaThemeMapper.controlBackground(theme));
        Color fg = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(theme));
        double radius = Math.max(UiMetrics.RADIUS_MD, theme.cornerRadius() + UiMetrics.SPACE_1);

        setBackground(new Background(new BackgroundFill(
            Color.color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0.72),
            new CornerRadii(radius),
            Insets.EMPTY
        )));

        setBorder(new Border(new BorderStroke(
            Color.color(fg.getRed(), fg.getGreen(), fg.getBlue(), 0.22),
            BorderStrokeStyle.SOLID,
            new CornerRadii(radius),
            new BorderWidths(1.0)
        )));

        setEffect(new DropShadow(UiMetrics.SPACE_3, Color.color(0.0, 0.0, 0.0, 0.35)));

        timeLabel.setFont(theme.contentFont());
        timeLabel.setTextFill(fg);
        stateLabel.setFont(theme.contentFont());
        stateLabel.setTextFill(Color.color(fg.getRed(), fg.getGreen(), fg.getBlue(), 0.85));
        repaintIcons();
    }

    private void wirePlayButton() {
        playBtn.setOnMouseClicked(e -> {
            if (playbackState.get() == PlaybackState.ERROR) return;
            if (playbackState.get() == PlaybackState.ENDED || playbackState.get() == PlaybackState.STOPPED) {
                service.seek(Duration.ZERO);
            }
            if (playbackState.get() == PlaybackState.PLAYING) {
                service.pause();
            } else {
                service.play();
            }
            onUserInteraction();
        });
    }

    private void wireStopButton() {
        stopBtn.setOnMouseClicked(e -> {
            if (playbackState.get() == PlaybackState.ERROR) return;
            service.stop();
            onUserInteraction();
        });
    }

    private void wireMuteButton() {
        muteBtn.setOnMouseClicked(e -> {
            MediaPlayer player = service.playerProperty().get();
            if (player == null || playbackState.get() == PlaybackState.ERROR) return;
            player.setMute(!player.isMute());
            onUserInteraction();
        });
    }

    private void wireSeekBar() {
        seekBar.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (Boolean.TRUE.equals(isChanging)) {
                seeking = true;
                resetScrubTracking();
                double targetSeconds = clampSeekValue(seekBar.getValue());
                updateScrubPreview(targetSeconds);
                maybeSeekDuringScrub(targetSeconds);
                onUserInteraction();
                return;
            }
            if (Boolean.TRUE.equals(wasChanging) && seeking) {
                commitSeekFromSlider();
            }
        });
        seekBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (seeking && newValue != null) {
                double targetSeconds = clampSeekValue(newValue.doubleValue());
                updateScrubPreview(targetSeconds);
                if (seekBar.isValueChanging()) {
                    maybeSeekDuringScrub(targetSeconds);
                }
            }
        });
        seekBar.setOnMousePressed(e -> {
            seeking = true;
            resetScrubTracking();
            double targetSeconds = clampSeekValue(seekBar.getValue());
            updateScrubPreview(targetSeconds);
            onUserInteraction();
        });
        seekBar.setOnMouseReleased(e -> {
            if (seeking) {
                commitSeekFromSlider();
            }
        });
    }

    private void wirePlayerLifecycle() {
        service.playerProperty().addListener((obs, oldPlayer, newPlayer) -> {
            detachPlayer(oldPlayer);
            currentPlayer = newPlayer;
            if (newPlayer == null) {
                setPlaybackState(PlaybackState.START, "");
                setInteractionEnabled(true);
                seekBar.setValue(0.0);
                timeLabel.setText("0:00 / 0:00");
                showNow();
                return;
            }
            attachPlayer(newPlayer);
        });
    }

    private void attachPlayer(MediaPlayer player) {
        player.currentTimeProperty().addListener(timeListener);
        player.statusProperty().addListener(statusListener);
        player.muteProperty().addListener(muteListener);
        muted = player.isMute();
        installPlayerHandlers(player);
        setInteractionEnabled(true);
        setPlaybackState(PlaybackState.START, "");
        Duration total = player.getTotalDuration();
        if (total != null && !total.isUnknown()) {
            seekBar.setMax(Math.max(1.0, total.toSeconds()));
        } else {
            seekBar.setMax(1.0);
        }
        timeLabel.setText(fmt(player.getCurrentTime()) + " / " + fmt(total));
        showNow();
    }

    private void detachPlayer(MediaPlayer player) {
        if (player == null) return;
        player.currentTimeProperty().removeListener(timeListener);
        player.statusProperty().removeListener(statusListener);
        player.muteProperty().removeListener(muteListener);
        player.setOnReady(null);
        player.setOnPlaying(null);
        player.setOnPaused(null);
        player.setOnStopped(null);
        player.setOnStalled(null);
        player.setOnEndOfMedia(null);
        player.setOnError(null);
    }

    private void installPlayerHandlers(MediaPlayer player) {
        player.setOnReady(() -> {
            Duration total = player.getTotalDuration();
            seekBar.setMax(total == null || total.isUnknown() ? 1.0 : Math.max(1.0, total.toSeconds()));
            timeLabel.setText(fmt(player.getCurrentTime()) + " / " + fmt(total));
            setPlaybackState(PlaybackState.READY, "Ready");
            setInteractionEnabled(true);
            showNow();
        });
        player.setOnPlaying(() -> {
            setPlaybackState(PlaybackState.PLAYING, "");
            setInteractionEnabled(true);
            showNow();
            restartIdleTimer();
        });
        player.setOnPaused(() -> {
            setPlaybackState(PlaybackState.PAUSED, "Paused");
            setInteractionEnabled(true);
            showNow();
        });
        player.setOnStopped(() -> {
            setPlaybackState(PlaybackState.STOPPED, "Stopped");
            setInteractionEnabled(true);
            seekBar.setValue(0.0);
            timeLabel.setText("0:00 / " + fmt(player.getTotalDuration()));
            showNow();
        });
        player.setOnStalled(() -> {
            setPlaybackState(PlaybackState.STALLED, "Buffering");
            setInteractionEnabled(true);
            showNow();
        });
        player.setOnEndOfMedia(() -> {
            setPlaybackState(PlaybackState.ENDED, "Ended");
            setInteractionEnabled(true);
            showNow();
        });
        player.setOnError(() -> {
            setPlaybackState(PlaybackState.ERROR, "Error");
            setInteractionEnabled(false);
            showNow();
        });
    }

    private void onTimeChanged(javafx.beans.value.ObservableValue<? extends Duration> obs,
                               Duration oldTime,
                               Duration nowTime) {
        if (nowTime == null) return;
        MediaPlayer player = service.playerProperty().get();
        if (player != null) {
            syncSeekRange(player, nowTime);
        }
        if (seeking) return;
        seekBar.setValue(Math.min(seekBar.getMax(), Math.max(0.0, nowTime.toSeconds())));
        if (player == null) return;
        timeLabel.setText(fmt(nowTime) + " / " + fmt(player.getTotalDuration()));
    }

    private void onStatusChanged(javafx.beans.value.ObservableValue<? extends MediaPlayer.Status> obs,
                                 MediaPlayer.Status oldStatus,
                                 MediaPlayer.Status newStatus) {
        if (newStatus == null) return;
        if (newStatus == MediaPlayer.Status.HALTED) {
            setPlaybackState(PlaybackState.ERROR, "Error");
            setInteractionEnabled(false);
            showNow();
            return;
        }
        if (newStatus == MediaPlayer.Status.PLAYING) {
            setPlaybackState(PlaybackState.PLAYING, "");
            restartIdleTimer();
            return;
        }
        if (newStatus == MediaPlayer.Status.PAUSED) {
            setPlaybackState(PlaybackState.PAUSED, "Paused");
            showNow();
            return;
        }
        if (newStatus == MediaPlayer.Status.STALLED) {
            setPlaybackState(PlaybackState.STALLED, "Buffering");
            showNow();
            return;
        }
        if (newStatus == MediaPlayer.Status.STOPPED) {
            setPlaybackState(PlaybackState.STOPPED, "Stopped");
            showNow();
        }
    }

    private void wireAutoHide() {
        fadeIn.setToValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (getOpacity() > 0.01) return;
            setVisible(false);
            setManaged(false);
            controlsVisible.set(false);
        });
        idleTimer.setOnFinished(e -> {
            if (playbackState.get() == PlaybackState.PLAYING) {
                hideNow();
            }
        });
    }

    private void wireTheme() {
        themeProperty.addListener((obs, oldTheme, newTheme) -> {
            if (newTheme != null) applyTheme(newTheme);
        });
    }

    private void onUserInteraction() {
        showNow();
        if (playbackState.get() == PlaybackState.PLAYING) {
            restartIdleTimer();
        }
    }

    private void onMouseExitedParent() {
        if (playbackState.get() != PlaybackState.PLAYING) return;
        hideNow();
    }

    private void showNow() {
        idleTimer.stop();
        fadeOut.stop();
        if (!isVisible()) {
            setVisible(true);
            setManaged(true);
        }
        controlsVisible.set(true);
        if (getOpacity() < 0.99) {
            fadeIn.setFromValue(Math.max(0.0, getOpacity()));
            fadeIn.playFromStart();
        } else {
            setOpacity(1.0);
        }
        if (playbackState.get() == PlaybackState.PLAYING) {
            restartIdleTimer();
        }
    }

    private void hideNow() {
        if (playbackState.get() != PlaybackState.PLAYING) return;
        idleTimer.stop();
        fadeIn.stop();
        if (!isVisible()) return;
        fadeOut.setFromValue(Math.max(0.0, getOpacity()));
        fadeOut.playFromStart();
    }

    private void hideImmediately() {
        idleTimer.stop();
        fadeIn.stop();
        fadeOut.stop();
        setOpacity(0.0);
        setVisible(false);
        setManaged(false);
        controlsVisible.set(false);
    }

    private void restartIdleTimer() {
        idleTimer.stop();
        idleTimer.playFromStart();
    }

    private void setPlaybackState(PlaybackState state, String label) {
        playbackState.set(state);
        boolean showLabel = label != null && !label.isBlank();
        stateLabel.setText(showLabel ? label : "");
        stateLabel.setVisible(showLabel);
        stateLabel.setManaged(showLabel);
        if (state != PlaybackState.PLAYING) {
            idleTimer.stop();
        }
        repaintIcons();
    }

    // Test hook for overlay-state assertions without relying on a live media backend.
    public void setPlaybackStateForTesting(PlaybackState state) {
        String label = switch (state) {
            case READY -> "Ready";
            case PAUSED -> "Paused";
            case STOPPED -> "Stopped";
            case STALLED -> "Buffering";
            case ENDED -> "Ended";
            case ERROR -> "Error";
            default -> "";
        };
        setPlaybackState(state, label);
        setInteractionEnabled(state != PlaybackState.ERROR);
        showNow();
    }

    private void setInteractionEnabled(boolean enabled) {
        playBtn.setDisable(!enabled);
        stopBtn.setDisable(!enabled);
        seekBar.setDisable(!enabled);
        muteBtn.setDisable(!enabled);
        volSlider.setDisable(!enabled);
    }

    private void commitSeekFromSlider() {
        double targetSeconds = clampSeekValue(seekBar.getValue());
        seeking = false;
        service.seek(Duration.millis(targetSeconds * 1000.0));
        lastScrubSeekSeconds = targetSeconds;
        lastScrubSeekNanos = System.nanoTime();
        updateScrubPreview(targetSeconds);
        onUserInteraction();
    }

    private void maybeSeekDuringScrub(double targetSeconds) {
        long now = System.nanoTime();
        boolean enoughTime = now - lastScrubSeekNanos >= LIVE_SCRUB_INTERVAL_NANOS;
        boolean enoughDelta = Double.isNaN(lastScrubSeekSeconds)
            || Math.abs(targetSeconds - lastScrubSeekSeconds) >= LIVE_SCRUB_DELTA_SECONDS;
        if (!enoughTime && !enoughDelta) return;
        lastScrubSeekSeconds = targetSeconds;
        lastScrubSeekNanos = now;
        service.seek(Duration.millis(targetSeconds * 1000.0));
    }

    private void resetScrubTracking() {
        lastScrubSeekSeconds = Double.NaN;
        lastScrubSeekNanos = 0L;
    }

    private double clampSeekValue(double seconds) {
        return Math.min(seekBar.getMax(), Math.max(0.0, seconds));
    }

    private void updateScrubPreview(double targetSeconds) {
        MediaPlayer player = service.playerProperty().get();
        Duration total = player == null ? Duration.UNKNOWN : player.getTotalDuration();
        timeLabel.setText(fmt(Duration.seconds(targetSeconds)) + " / " + fmt(total));
    }

    private void syncSeekRange(MediaPlayer player, Duration nowTime) {
        double totalSeconds = durationToSeconds(player.getTotalDuration());
        double nowSeconds = durationToSeconds(nowTime);
        double targetMax = Double.isNaN(totalSeconds) || totalSeconds < 1.0
            ? Math.max(seekBar.getMax(), Math.max(1.0, nowSeconds))
            : Math.max(1.0, totalSeconds);
        if (Math.abs(seekBar.getMax() - targetMax) > 0.01) {
            seekBar.setMax(targetMax);
        }
    }

    private static double durationToSeconds(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return Double.NaN;
        }
        return Math.max(0.0, duration.toSeconds());
    }

    private static String fmt(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) return "0:00";
        int totalSeconds = (int) Math.max(0.0, duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private static Canvas iconCanvas() {
        return new Canvas(ICON_SIZE, ICON_SIZE);
    }

    private void repaintIcons() {
        Theme theme = themeProperty.get();
        Color color = theme == null
            ? Color.WHITE
            : MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(theme));
        paintPlayIcon(color);
        paintStopIcon(color);
        paintMuteIcon(color);
    }

    private void paintPlayIcon(Color color) {
        GraphicsContext graphics = playBtn.getGraphicsContext2D();
        graphics.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        graphics.setFill(color);
        if (playbackState.get() == PlaybackState.ENDED) {
            graphics.setStroke(color);
            graphics.setLineWidth(2.0);
            graphics.strokeArc(2, 2, 12, 12, 40, 300, ArcType.OPEN);
            graphics.fillPolygon(new double[]{11, 14, 10}, new double[]{2, 6, 6}, 3);
            return;
        }
        if (playbackState.get() == PlaybackState.PLAYING) {
            graphics.fillRect(2, 2, 4, 12);
            graphics.fillRect(10, 2, 4, 12);
            return;
        }
        graphics.fillPolygon(new double[]{2, 14, 2}, new double[]{1, 8, 15}, 3);
    }

    private void paintStopIcon(Color color) {
        GraphicsContext graphics = stopBtn.getGraphicsContext2D();
        graphics.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        graphics.setFill(color);
        graphics.fillRect(2, 2, 12, 12);
    }

    private void paintMuteIcon(Color color) {
        GraphicsContext graphics = muteBtn.getGraphicsContext2D();
        graphics.clearRect(0, 0, ICON_SIZE, ICON_SIZE);
        graphics.setFill(color);
        graphics.fillPolygon(new double[]{2, 8, 8, 2}, new double[]{5, 2, 14, 11}, 4);
        graphics.fillRect(9, 5, 5, 6);
        if (muted) {
            graphics.setStroke(color);
            graphics.setLineWidth(2.0);
            graphics.strokeLine(10.5, 4, 14.5, 12);
        }
    }
}
