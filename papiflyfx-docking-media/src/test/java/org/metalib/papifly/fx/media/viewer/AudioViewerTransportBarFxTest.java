package org.metalib.papifly.fx.media.viewer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.TransportBar;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class AudioViewerTransportBarFxTest {

    private AudioViewer viewer;
    private RuntimeException mediaLoadFailure;

    @Start
    void start(Stage stage) {
        viewer = new AudioViewer();
        viewer.themeProperty().set(Theme.dark());
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        try {
            viewer.load(url);
        } catch (RuntimeException ex) {
            mediaLoadFailure = ex;
        }
        stage.setScene(new Scene(viewer, 720, 300));
        stage.show();
    }

    @Test
    void transportBarRemainsCompactAndVisibleOnPause(FxRobot robot) {
        assumeMediaBackendAvailable();
        robot.interact(() -> {});
        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            assertTrue(bar.getHeight() < viewer.getHeight() * 0.5);
        });

        robot.interact(() -> {
            MediaPlayer player = viewer.playerForTesting();
            assertNotNull(player);
            Runnable onPaused = player.getOnPaused();
            if (onPaused != null) onPaused.run();
        });

        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            assertEquals(TransportBar.PlaybackState.PAUSED, bar.getPlaybackState());
            assertTrue(bar.isControlsVisible());
        });
    }

    @Test
    void transportBarUsesSharedDensityAndRespondsToThemeSwitch(FxRobot robot) {
        AtomicReference<Color> darkBackground = new AtomicReference<>();

        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            assertEquals(UiMetrics.SPACE_2, bar.getSpacing(), 0.01);
            assertInsetsEquals(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_3, UiMetrics.SPACE_1, UiMetrics.SPACE_3), bar.getPadding());
            darkBackground.set(backgroundColor(bar));
            viewer.themeProperty().set(Theme.light());
            bar.applyCss();
            bar.layout();
        });

        robot.interact(() -> {
            TransportBar bar = viewer.transportBarForTesting();
            Color lightBackground = backgroundColor(bar);
            assertNotNull(darkBackground.get());
            assertNotNull(lightBackground);
            assertFalse(colorsClose(darkBackground.get(), lightBackground, 0.01));
        });
    }

    private static void assertInsetsEquals(Insets expected, Insets actual) {
        assertEquals(expected.getTop(), actual.getTop(), 0.01);
        assertEquals(expected.getRight(), actual.getRight(), 0.01);
        assertEquals(expected.getBottom(), actual.getBottom(), 0.01);
        assertEquals(expected.getLeft(), actual.getLeft(), 0.01);
    }

    private static Color backgroundColor(TransportBar bar) {
        if (bar.getBackground() == null || bar.getBackground().getFills().isEmpty()) {
            return null;
        }
        if (bar.getBackground().getFills().getFirst().getFill() instanceof Color color) {
            return color;
        }
        return null;
    }

    private static boolean colorsClose(Color expected, Color actual, double tolerance) {
        return Math.abs(expected.getRed() - actual.getRed()) <= tolerance
            && Math.abs(expected.getGreen() - actual.getGreen()) <= tolerance
            && Math.abs(expected.getBlue() - actual.getBlue()) <= tolerance
            && Math.abs(expected.getOpacity() - actual.getOpacity()) <= tolerance;
    }

    private void assumeMediaBackendAvailable() {
        RuntimeException failure = mediaLoadFailure;
        Assumptions.assumeTrue(failure == null && !viewer.isErrorState(), () ->
            "JavaFX media backend unavailable in this environment: " + failure);
    }
}
