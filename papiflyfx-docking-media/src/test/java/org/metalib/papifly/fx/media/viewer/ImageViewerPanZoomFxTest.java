package org.metalib.papifly.fx.media.viewer;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ImageViewerPanZoomFxTest {

    private ImageViewer viewer;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        viewer = new ImageViewer();
        viewer.themeProperty().set(Theme.dark());
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        viewer.load(url);
        stage.setScene(new Scene(viewer, 640, 360));
        stage.show();
    }

    @Test
    void clampsPanWithinViewportBoundsAtHighZoom(FxRobot robot) throws Exception {
        waitForImage(robot);

        robot.interact(() -> {
            viewer.setZoomLevel(2.8);
            viewer.setPanForTesting(10_000.0, 10_000.0);
        });
        robot.interact(() -> {
            assertEquals(viewer.maxPanXForTesting(), viewer.getPanX(), 0.1);
            assertEquals(viewer.maxPanYForTesting(), viewer.getPanY(), 0.1);
        });

        robot.interact(() -> viewer.setPanForTesting(-10_000.0, -10_000.0));
        robot.interact(() -> {
            assertEquals(-viewer.maxPanXForTesting(), viewer.getPanX(), 0.1);
            assertEquals(-viewer.maxPanYForTesting(), viewer.getPanY(), 0.1);
        });
    }

    @Test
    void resetsPanToViewportWhenZoomReturnsToOne(FxRobot robot) throws Exception {
        waitForImage(robot);

        robot.interact(() -> {
            viewer.setZoomLevel(3.0);
            viewer.setPanForTesting(10_000.0, 10_000.0);
        });
        robot.interact(() -> {
            assertTrue(viewer.maxPanXForTesting() > 0.0 || viewer.maxPanYForTesting() > 0.0);
            assertTrue(Math.abs(viewer.getPanX()) > 0.0 || Math.abs(viewer.getPanY()) > 0.0);
        });

        robot.interact(() -> viewer.setZoomLevel(1.0));
        robot.interact(() -> {
            assertEquals(0.0, viewer.maxPanXForTesting(), 0.1);
            assertEquals(0.0, viewer.maxPanYForTesting(), 0.1);
            assertEquals(0.0, viewer.getPanX(), 0.1);
            assertEquals(0.0, viewer.getPanY(), 0.1);
        });
    }

    @Test
    void keepsImageClippedToViewportOnResize(FxRobot robot) throws Exception {
        waitForImage(robot);

        robot.interact(() -> {
            assertTrue(viewer.hasViewportClipForTesting());
            assertEquals(viewer.getWidth(), viewer.viewportClipWidthForTesting(), 0.1);
            assertEquals(viewer.getHeight(), viewer.viewportClipHeightForTesting(), 0.1);
        });

        robot.interact(() -> {
            stage.setWidth(820.0);
            stage.setHeight(520.0);
        });

        robot.interact(() -> {
            assertEquals(viewer.getWidth(), viewer.viewportClipWidthForTesting(), 0.1);
            assertEquals(viewer.getHeight(), viewer.viewportClipHeightForTesting(), 0.1);
        });
    }

    private void waitForImage(FxRobot robot) throws Exception {
        for (int i = 0; i < 80; i++) {
            AtomicBoolean loaded = new AtomicBoolean(false);
            robot.interact(() -> loaded.set(viewer.hasImageForTesting()));
            if (loaded.get()) return;
            Thread.sleep(25L);
        }
        throw new AssertionError("Image did not load in time");
    }
}
