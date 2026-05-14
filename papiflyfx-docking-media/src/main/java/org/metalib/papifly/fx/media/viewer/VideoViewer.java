package org.metalib.papifly.fx.media.viewer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.media.player.MediaPlayerService;
import org.metalib.papifly.fx.media.theme.MediaThemeMapper;
import org.metalib.papifly.fx.ui.UiMetrics;

public class VideoViewer extends StackPane {

    private final MediaPlayerService playerService = new MediaPlayerService();
    private final MediaView mediaView = new MediaView();
    private final Region bottomScrim = new Region();
    private final TransportBar transportBar = new TransportBar(playerService);
    private final StackPane controlsOverlay = new StackPane();
    private final StackPane centerAffordance = new StackPane();
    private final Label centerGlyph = new Label("\u25B6");
    private final Rectangle viewportClip = new Rectangle();
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private long pendingTimeMs = 0L;
    private double pendingVolume = 1.0;
    private boolean pendingMuted = false;
    private boolean playbackStatePending;
    private MediaPlayer restorePlayer;
    private ChangeListener<MediaPlayer.Status> restoreStatusListener;
    private boolean errorState = false;

    public VideoViewer() {
        setMinSize(0, 0);
        setAlignment(Pos.BOTTOM_CENTER);
        viewportClip.widthProperty().bind(widthProperty());
        viewportClip.heightProperty().bind(heightProperty());
        setClip(viewportClip);

        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        StackPane.setAlignment(mediaView, Pos.CENTER);

        bottomScrim.setManaged(false);
        bottomScrim.setMouseTransparent(true);
        bottomScrim.setPickOnBounds(false);

        controlsOverlay.setPickOnBounds(false);
        controlsOverlay.setAlignment(Pos.BOTTOM_CENTER);
        controlsOverlay.setMinHeight(Region.USE_PREF_SIZE);
        controlsOverlay.setMaxHeight(Region.USE_PREF_SIZE);
        controlsOverlay.getChildren().add(transportBar);
        StackPane.setAlignment(controlsOverlay, Pos.BOTTOM_CENTER);
        StackPane.setMargin(controlsOverlay, new Insets(0, UiMetrics.SPACE_2, UiMetrics.SPACE_2, UiMetrics.SPACE_2));

        centerAffordance.setManaged(false);
        centerAffordance.setPickOnBounds(false);
        centerAffordance.getChildren().add(centerGlyph);
        centerAffordance.setOnMouseClicked(e -> onCenterAffordanceClicked());

        getChildren().addAll(mediaView, bottomScrim, controlsOverlay, centerAffordance);

        transportBar.themeProperty().bind(themeProperty);
        transportBar.showFor(this);

        wireSceneReparent();
        wirePlayerStateRestore();
        wireCenterAffordance();
        wireKeyboard();
        wireTheme();
        wireVisibility();
        wireError();
    }

    public void load(String url) {
        playerService.load(url);
        playerService.bind(mediaView);
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public boolean isErrorState() { return errorState; }

    public long getCurrentTimeMs() {
        MediaPlayer player = playerService.playerProperty().get();
        return player != null ? (long) player.getCurrentTime().toMillis() : 0L;
    }

    public double getVolume() {
        return playerService.volumeProperty().get();
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

    @Override
    protected void layoutChildren() {
        mediaView.setFitWidth(getWidth());
        mediaView.setFitHeight(getHeight());
        super.layoutChildren();
        Bounds mediaBounds = mediaBoundsInViewer();
        layoutBottomScrim(mediaBounds);
        layoutControlsOverlay(mediaBounds);
        layoutCenterAffordance(mediaBounds);
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

    private void wireCenterAffordance() {
        transportBar.playbackStateProperty().addListener((obs, oldState, newState) -> {
            applyCenterAffordanceState(newState);
        });
        applyCenterAffordanceState(transportBar.getPlaybackState());
    }

    private void wireKeyboard() {
        setFocusTraversable(true);
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE -> { if (playerService.playerProperty().get() != null
                        && playerService.playerProperty().get().getStatus()
                            == javafx.scene.media.MediaPlayer.Status.PLAYING) {
                        playerService.pause();
                    } else {
                        playerService.play();
                    }
                    e.consume(); }
                case M     -> { MediaPlayer player = playerService.playerProperty().get();
                                if (player != null) player.setMute(!player.isMute()); e.consume(); }
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
            applyScrimTheme(t);
            applyCenterTheme(t);
        });
    }

    private void wireVisibility() {
        visibleProperty().addListener((obs, o, visible) -> {
            MediaPlayer player = playerService.playerProperty().get();
            if (player != null) player.setMute(!visible);
        });
    }

    private void wireError() {
        playerService.setOnError(() -> {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(this::handleError);
            } else {
                handleError();
            }
        });
    }

    private void handleError() {
        errorState = true;
        controlsOverlay.setVisible(false);
        centerAffordance.setVisible(false);
    }

    private void layoutBottomScrim(Bounds mediaBounds) {
        double width = mediaBounds.getWidth();
        double height = mediaBounds.getHeight();
        double controlFootprint = transportBar.prefHeight(-1) + 30.0;
        double targetHeight = Math.max(height * 0.22, controlFootprint);
        double minHeight = Math.min(68.0, height * 0.30);
        double maxHeight = Math.max(minHeight, height * 0.30);
        double scrimHeight = clamp(targetHeight, minHeight, maxHeight);
        bottomScrim.resizeRelocate(
            mediaBounds.getMinX(),
            mediaBounds.getMaxY() - scrimHeight,
            width,
            scrimHeight
        );
    }

    private void layoutControlsOverlay(Bounds mediaBounds) {
        double leftInset = Math.max(8.0, mediaBounds.getMinX() + 8.0);
        double rightInset = Math.max(8.0, (getWidth() - mediaBounds.getMaxX()) + 8.0);
        double bottomInset = Math.max(8.0, (getHeight() - mediaBounds.getMaxY()) + 8.0);
        StackPane.setMargin(controlsOverlay, new Insets(0.0, rightInset, bottomInset, leftInset));
    }

    private void layoutCenterAffordance(Bounds mediaBounds) {
        if (!centerAffordance.isVisible()) return;
        double size = clamp(Math.min(mediaBounds.getWidth(), mediaBounds.getHeight()) * 0.18, 64.0, 94.0);
        double centerX = mediaBounds.getMinX() + (mediaBounds.getWidth() / 2.0);
        double centerY = mediaBounds.getMinY() + (mediaBounds.getHeight() / 2.0);
        centerAffordance.resizeRelocate(
            centerX - (size / 2.0),
            centerY - (size / 2.0),
            size,
            size
        );
    }

    private Bounds mediaBoundsInViewer() {
        Bounds bounds = mediaView.getBoundsInParent();
        if (bounds.getWidth() <= 0.0 || bounds.getHeight() <= 0.0) {
            return new javafx.geometry.BoundingBox(0.0, 0.0, getWidth(), getHeight());
        }
        return bounds;
    }

    private void applyScrimTheme(Theme theme) {
        Color base = MediaThemeMapper.toColor(MediaThemeMapper.controlBackground(theme));
        Color top = Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.0);
        Color mid = Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.34);
        Color bottom = Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.86);
        LinearGradient gradient = new LinearGradient(
            0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, top),
            new Stop(0.45, mid),
            new Stop(1.0, bottom)
        );
        bottomScrim.setBackground(new javafx.scene.layout.Background(
            new javafx.scene.layout.BackgroundFill(
                gradient,
                javafx.scene.layout.CornerRadii.EMPTY,
                Insets.EMPTY
            )));
    }

    private void applyCenterTheme(Theme theme) {
        Color bg = MediaThemeMapper.toColor(MediaThemeMapper.controlBackground(theme));
        Color fg = MediaThemeMapper.toColor(MediaThemeMapper.controlForeground(theme));
        centerAffordance.setBackground(new javafx.scene.layout.Background(
            new javafx.scene.layout.BackgroundFill(
                Color.color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0.64),
                new javafx.scene.layout.CornerRadii(999.0),
                Insets.EMPTY
            )
        ));
        centerAffordance.setBorder(new javafx.scene.layout.Border(
            new javafx.scene.layout.BorderStroke(
                Color.color(fg.getRed(), fg.getGreen(), fg.getBlue(), 0.26),
                javafx.scene.layout.BorderStrokeStyle.SOLID,
                new javafx.scene.layout.CornerRadii(999.0),
                new javafx.scene.layout.BorderWidths(1.0)
            )
        ));
        centerGlyph.setTextFill(fg);
        Font baseFont = theme.contentFont();
        centerGlyph.setFont(Font.font(baseFont.getFamily(), FontWeight.BOLD, 34));
    }

    private void applyCenterAffordanceState(TransportBar.PlaybackState state) {
        if (state == TransportBar.PlaybackState.PLAYING) {
            centerAffordance.setVisible(false);
            return;
        }
        if (state == TransportBar.PlaybackState.ENDED) {
            centerGlyph.setText("\u21BA");
            centerAffordance.setVisible(true);
            return;
        }
        if (state == TransportBar.PlaybackState.PAUSED
            || state == TransportBar.PlaybackState.READY
            || state == TransportBar.PlaybackState.START
            || state == TransportBar.PlaybackState.STOPPED) {
            centerGlyph.setText("\u25B6");
            centerAffordance.setVisible(true);
            return;
        }
        centerAffordance.setVisible(false);
    }

    private void onCenterAffordanceClicked() {
        TransportBar.PlaybackState state = transportBar.getPlaybackState();
        if (state == TransportBar.PlaybackState.ERROR) return;
        if (state == TransportBar.PlaybackState.ENDED || state == TransportBar.PlaybackState.STOPPED) {
            playerService.seek(Duration.ZERO);
        }
        if (state == TransportBar.PlaybackState.PLAYING) {
            playerService.pause();
        } else {
            playerService.play();
        }
        transportBar.showControlsNow();
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    TransportBar transportBarForTesting() {
        return transportBar;
    }

    Region bottomScrimForTesting() {
        return bottomScrim;
    }

    StackPane centerAffordanceForTesting() {
        return centerAffordance;
    }

    MediaPlayer playerForTesting() {
        return playerService.playerProperty().get();
    }

    Bounds mediaBoundsForTesting() {
        return mediaBoundsInViewer();
    }

    void setPlaybackStateForTesting(TransportBar.PlaybackState state) {
        transportBar.setPlaybackStateForTesting(state);
    }
}
