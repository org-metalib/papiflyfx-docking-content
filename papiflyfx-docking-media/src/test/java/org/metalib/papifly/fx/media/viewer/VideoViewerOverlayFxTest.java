package org.metalib.papifly.fx.media.viewer;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.geometry.Bounds;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class VideoViewerOverlayFxTest {

    private VideoViewer viewer;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        viewer = new VideoViewer();
        viewer.themeProperty().set(Theme.dark());
        stage.setScene(new Scene(viewer, 640, 360));
        stage.show();
    }

    @AfterEach
    void cleanup() {
        if (viewer != null) {
            Platform.runLater(viewer::dispose);
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    @Test
    void bottomScrimStaysInLowerBoundedArea(FxRobot robot) {
        robot.interact(() -> {});
        robot.interact(() -> {
            Region scrim = viewer.bottomScrimForTesting();
            MediaView mediaView = findDescendant(viewer, MediaView.class);
            assertNotNull(mediaView);
            Bounds mediaBounds = viewer.mediaBoundsForTesting();
            double ratio = scrim.getHeight() / mediaBounds.getHeight();
            assertTrue(ratio >= 0.14);
            assertTrue(ratio <= 0.31);
            assertEquals(mediaBounds.getWidth(), scrim.getWidth(), 1.0);
            assertEquals(mediaBounds.getMinX(), scrim.getLayoutX(), 1.0);
            assertEquals(mediaBounds.getMaxY(), scrim.getLayoutY() + scrim.getHeight(), 1.0);
        });
    }

    @Test
    void controlsAutoHideOnlyWhenPlaying(FxRobot robot) {
        AtomicReference<TransportBar> barRef = new AtomicReference<>();
        robot.interact(() -> {
            barRef.set(viewer.transportBarForTesting());
        });
        TransportBar bar = barRef.get();
        assertNotNull(bar);

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.PAUSED);
            bar.triggerIdleTimeout();
        });
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PAUSED, bar.getPlaybackState());
            assertTrue(bar.isControlsVisible());
        });

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.PLAYING);
            bar.triggerIdleTimeout();
        });
        robot.interact(() -> {});
        robot.interact(() -> {
            assertEquals(TransportBar.PlaybackState.PLAYING, bar.getPlaybackState());
            assertFalse(bar.isControlsVisible());
        });
    }

    @Test
    void centerAffordanceTracksPlaybackState(FxRobot robot) {
        AtomicReference<TransportBar> barRef = new AtomicReference<>();
        AtomicReference<StackPane> centerRef = new AtomicReference<>();
        robot.interact(() -> {
            barRef.set(viewer.transportBarForTesting());
            centerRef.set(viewer.centerAffordanceForTesting());
        });
        TransportBar bar = barRef.get();
        StackPane center = centerRef.get();
        assertNotNull(bar);
        assertNotNull(center);

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.READY);
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u25B6", glyph.getText());
        });

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.PLAYING);
            assertFalse(center.isVisible());
        });

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.PAUSED);
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u25B6", glyph.getText());
        });

        robot.interact(() -> {
            viewer.setPlaybackStateForTesting(TransportBar.PlaybackState.ENDED);
            assertTrue(center.isVisible());
            Label glyph = (Label) center.getChildren().get(0);
            assertEquals("\u21BA", glyph.getText());
        });
    }

    @Test
    void mediaStaysCenteredAfterViewportResize(FxRobot robot) {
        robot.interact(() -> {
            stage.setWidth(360);
            stage.setHeight(720);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            MediaView mediaView = findDescendant(viewer, MediaView.class);
            assertNotNull(mediaView);
            assertEquals(Pos.CENTER, StackPane.getAlignment(mediaView));
            Bounds mediaBounds = viewer.mediaBoundsForTesting();
            double expectedX = (viewer.getWidth() - mediaBounds.getWidth()) / 2.0;
            double expectedY = (viewer.getHeight() - mediaBounds.getHeight()) / 2.0;
            assertEquals(expectedX, mediaBounds.getMinX(), 1.5);
            assertEquals(expectedY, mediaBounds.getMinY(), 1.5);
        });
    }

    @Test
    void viewerClipTracksViewportResize(FxRobot robot) {
        robot.interact(() -> {
            assertTrue(viewer.getClip() instanceof Rectangle);
            Rectangle clip = (Rectangle) viewer.getClip();
            assertEquals(viewer.getWidth(), clip.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), clip.getHeight(), 1.0);
        });

        robot.interact(() -> {
            stage.setWidth(380);
            stage.setHeight(760);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            assertTrue(viewer.getClip() instanceof Rectangle);
            Rectangle clip = (Rectangle) viewer.getClip();
            assertEquals(viewer.getWidth(), clip.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), clip.getHeight(), 1.0);
        });
    }

    private static <T> T findDescendant(Node root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = findDescendant(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

}
