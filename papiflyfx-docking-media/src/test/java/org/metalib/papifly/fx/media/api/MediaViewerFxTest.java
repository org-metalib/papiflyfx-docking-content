package org.metalib.papifly.fx.media.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.viewer.EmbedViewer;
import org.metalib.papifly.fx.media.viewer.ImageViewer;
import org.metalib.papifly.fx.media.viewer.VideoViewer;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class MediaViewerFxTest {

    private MediaViewer viewer;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        viewer = new MediaViewer();
        stage.setScene(new Scene(viewer, 400, 300));
        stage.show();
    }

    @Test
    void createsWithoutError() {
        assertNotNull(viewer);
    }

    @Test
    void loadUnknownFormatShowsError(FxRobot robot) {
        robot.interact(() -> viewer.loadMedia("file:///unknown.xyz"));
        robot.interact(() -> assertFalse(viewer.getChildren().isEmpty()));
    }

    @Test
    void disposeDoesNotThrow(FxRobot robot) {
        robot.interact(() -> viewer.loadMedia("file:///sample.png"));
        assertDoesNotThrow(() -> robot.interact(() -> viewer.dispose()));
    }

    @Test
    void innerViewerFillsContainerAfterResize(FxRobot robot) {
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        robot.interact(() -> viewer.loadMedia(url));
        robot.interact(() -> {});  // allow layout pass

        robot.interact(() -> {
            assertFalse(viewer.getChildren().isEmpty());
            Region inner = (Region) viewer.getChildren().get(0);
            assertEquals(viewer.getWidth(), inner.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), inner.getHeight(), 1.0);
        });

        robot.interact(() -> {
            stage.setWidth(600);
            stage.setHeight(450);
        });
        robot.interact(() -> {});  // allow layout pass
        robot.interact(() -> {});  // allow deferred child layout

        robot.interact(() -> {
            Region inner = (Region) viewer.getChildren().get(0);
            assertEquals(viewer.getWidth(), inner.getWidth(), 1.0);
            assertEquals(viewer.getHeight(), inner.getHeight(), 1.0);
        });
    }

    @Test
    void imageNodeFitSizeTracksResize(FxRobot robot) {
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        robot.interact(() -> viewer.loadMedia(url));
        robot.interact(() -> {});

        AtomicReference<Double> initialFitWidth = new AtomicReference<>();
        robot.interact(() -> {
            ImageViewer imageViewer = (ImageViewer) viewer.getChildren().get(0);
            ImageView imageView = findDescendant(imageViewer, ImageView.class);
            assertNotNull(imageView);
            assertEquals(imageViewer.getWidth(), imageView.getFitWidth(), 1.0);
            assertEquals(imageViewer.getHeight(), imageView.getFitHeight(), 1.0);
            initialFitWidth.set(imageView.getFitWidth());
        });

        robot.interact(() -> {
            stage.setWidth(720);
            stage.setHeight(520);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            ImageViewer imageViewer = (ImageViewer) viewer.getChildren().get(0);
            ImageView imageView = findDescendant(imageViewer, ImageView.class);
            assertNotNull(imageView);
            assertNotEquals(initialFitWidth.get(), imageView.getFitWidth(), 1.0);
            assertEquals(imageViewer.getWidth(), imageView.getFitWidth(), 1.0);
            assertEquals(imageViewer.getHeight(), imageView.getFitHeight(), 1.0);
        });
    }

    @Test
    void videoNodeFitSizeTracksResize(FxRobot robot) {
        String url = getClass().getResource("/sample-media/sample.mp4").toExternalForm();
        AtomicReference<RuntimeException> mediaLoadFailure = new AtomicReference<>();
        robot.interact(() -> {
            try {
                viewer.loadMedia(url);
            } catch (RuntimeException ex) {
                mediaLoadFailure.set(ex);
            }
        });
        RuntimeException failure = mediaLoadFailure.get();
        Assumptions.assumeTrue(failure == null, () ->
            "JavaFX media backend unavailable in this environment: " + failure);
        robot.interact(() -> {});

        AtomicReference<Double> initialFitWidth = new AtomicReference<>();
        robot.interact(() -> {
            VideoViewer videoViewer = (VideoViewer) viewer.getChildren().get(0);
            MediaView mediaView = findDescendant(videoViewer, MediaView.class);
            assertNotNull(mediaView);
            assertEquals(videoViewer.getWidth(), mediaView.getFitWidth(), 1.0);
            assertEquals(videoViewer.getHeight(), mediaView.getFitHeight(), 1.0);
            initialFitWidth.set(mediaView.getFitWidth());
        });

        robot.interact(() -> {
            stage.setWidth(760);
            stage.setHeight(560);
        });
        robot.interact(() -> {});
        robot.interact(() -> {});

        robot.interact(() -> {
            VideoViewer videoViewer = (VideoViewer) viewer.getChildren().get(0);
            MediaView mediaView = findDescendant(videoViewer, MediaView.class);
            assertNotNull(mediaView);
            assertNotEquals(initialFitWidth.get(), mediaView.getFitWidth(), 1.0);
            assertEquals(videoViewer.getWidth(), mediaView.getFitWidth(), 1.0);
            assertEquals(videoViewer.getHeight(), mediaView.getFitHeight(), 1.0);
        });
    }

    @Test
    void embedNodeTracksRepeatedResize(FxRobot robot) {
        robot.interact(() -> viewer.loadMedia("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        robot.interact(() -> {});

        int[][] sizes = {
            {300, 180},
            {290, 175},
            {280, 170},
            {270, 165},
            {260, 160}
        };

        for (int[] size : sizes) {
            robot.interact(() -> {
                stage.setWidth(size[0]);
                stage.setHeight(size[1]);
            });
            robot.interact(() -> {});
            robot.interact(() -> {});

            robot.interact(() -> {
                EmbedViewer embedViewer = (EmbedViewer) viewer.getChildren().get(0);
                WebView webView = findDescendant(embedViewer, WebView.class);
                assertNotNull(webView);
                assertEquals(embedViewer.getWidth(), webView.getWidth(), 1.0);
                assertEquals(embedViewer.getHeight(), webView.getHeight(), 1.0);
            });
        }
    }

    @Test
    void bindThemePropertyPropagatesToActiveViewer(FxRobot robot) {
        String url = getClass().getResource("/sample-media/sample.png").toExternalForm();
        ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
        AtomicReference<Color> darkBackground = new AtomicReference<>();

        robot.interact(() -> {
            viewer.loadMedia(url);
            viewer.bindThemeProperty(themeProperty);
        });
        robot.interact(() -> {});

        robot.interact(() -> {
            ImageViewer imageViewer = (ImageViewer) viewer.getChildren().get(0);
            darkBackground.set(backgroundColor(imageViewer));
            themeProperty.set(Theme.light());
        });
        robot.interact(() -> {});

        robot.interact(() -> {
            ImageViewer imageViewer = (ImageViewer) viewer.getChildren().get(0);
            Color lightBackground = backgroundColor(imageViewer);
            assertNotNull(darkBackground.get());
            assertNotNull(lightBackground);
            assertNotEquals(darkBackground.get(), lightBackground);
        });
    }

    @Test
    void bindThemePropertyPropagatesToEmbedViewer(FxRobot robot) {
        ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());
        AtomicReference<Color> darkBackground = new AtomicReference<>();

        robot.interact(() -> {
            viewer.loadMedia("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            viewer.bindThemeProperty(themeProperty);
        });
        robot.interact(() -> {});

        robot.interact(() -> {
            EmbedViewer embedViewer = (EmbedViewer) viewer.getChildren().get(0);
            darkBackground.set(backgroundColor(embedViewer));
            themeProperty.set(Theme.light());
        });
        robot.interact(() -> {});

        robot.interact(() -> {
            EmbedViewer embedViewer = (EmbedViewer) viewer.getChildren().get(0);
            Color lightBackground = backgroundColor(embedViewer);
            assertNotNull(darkBackground.get());
            assertNotNull(lightBackground);
            assertNotEquals(darkBackground.get(), lightBackground);
            assertEquals(Theme.light().background(), lightBackground);
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

    private static Color backgroundColor(Region region) {
        if (region.getBackground() == null || region.getBackground().getFills().isEmpty()) {
            return null;
        }
        if (region.getBackground().getFills().getFirst().getFill() instanceof Color color) {
            return color;
        }
        return null;
    }
}
