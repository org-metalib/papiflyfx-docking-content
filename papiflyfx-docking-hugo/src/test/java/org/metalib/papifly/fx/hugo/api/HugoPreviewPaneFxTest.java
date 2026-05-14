package org.metalib.papifly.fx.hugo.api;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.FxTestUtil;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class HugoPreviewPaneFxTest {

    private StackPane root;

    @Start
    void start(Stage stage) {
        root = new StackPane();
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @Test
    void paneRendersToolbarAndWebView(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20110, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-toolbar")));
        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-webview")));
        assertNotNull(FxTestUtil.callFx(() -> pane.lookup("#hugo-preview-status")));

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void startAndStopUpdateStatusAndUrl(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20120, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        Button startButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-start"));
        Button stopButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-stop"));
        assertNotNull(startButton);
        assertNotNull(stopButton);
        assertFalse(FxTestUtil.callFx(startButton::isDisabled));
        assertTrue(FxTestUtil.callFx(stopButton::isDisabled));

        FxTestUtil.runFx(() -> pane.startServerAndLoad("/docs/"));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> {
                Label state = (Label) pane.lookup("#hugo-preview-status-state");
                return state != null && state.getText().startsWith("Running");
            }));
        assertTrue(FxTestUtil.callFx(startButton::isDisabled));
        assertFalse(FxTestUtil.callFx(stopButton::isDisabled));

        Hyperlink address = FxTestUtil.callFx(() -> (Hyperlink) pane.lookup("#hugo-preview-address"));
        assertNotNull(address);
        assertTrue(address.getText().contains("http://127.0.0.1:"));

        FxTestUtil.runFx(pane::stopServer);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> {
                Label state = (Label) pane.lookup("#hugo-preview-status-state");
                return state != null && "Stopped".equals(state.getText());
            }));
        assertFalse(FxTestUtil.callFx(startButton::isDisabled));
        assertTrue(FxTestUtil.callFx(stopButton::isDisabled));

        WebView webView = FxTestUtil.callFx(() -> (WebView) pane.lookup("#hugo-preview-webview"));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> "about:blank".equals(webView.getEngine().getLocation())));

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void themeBindingDoesNotApplyDockingStylesToHugoContent(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20130, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.dark());
        FxTestUtil.runFx(() -> pane.bindThemeProperty(theme));

        String initialPaneStyle = FxTestUtil.callFx(pane::getStyle);
        String initialToolbarStyle = FxTestUtil.callFx(() -> ((Region) pane.lookup("#hugo-preview-toolbar")).getStyle());
        String initialStartStyle = FxTestUtil.callFx(() -> ((Button) pane.lookup("#hugo-preview-start")).getStyle());
        String initialStatusStyle = FxTestUtil.callFx(() -> ((Label) pane.lookup("#hugo-preview-status-state")).getStyle());
        String initialUserStyleSheet = FxTestUtil.callFx(() -> ((WebView) pane.lookup("#hugo-preview-webview"))
            .getEngine().getUserStyleSheetLocation());
        FxTestUtil.runFx(() -> theme.set(Theme.light()));
        FxTestUtil.waitForFx();

        String updatedPaneStyle = FxTestUtil.callFx(pane::getStyle);
        String updatedToolbarStyle = FxTestUtil.callFx(() -> ((Region) pane.lookup("#hugo-preview-toolbar")).getStyle());
        String updatedStartStyle = FxTestUtil.callFx(() -> ((Button) pane.lookup("#hugo-preview-start")).getStyle());
        String updatedStatusStyle = FxTestUtil.callFx(() -> ((Label) pane.lookup("#hugo-preview-status-state")).getStyle());
        String updatedUserStyleSheet = FxTestUtil.callFx(() -> ((WebView) pane.lookup("#hugo-preview-webview"))
            .getEngine().getUserStyleSheetLocation());

        assertFalse(initialPaneStyle.equals(updatedPaneStyle));
        assertFalse(initialToolbarStyle.equals(updatedToolbarStyle));
        assertFalse(initialStartStyle.equals(updatedStartStyle));
        assertFalse(initialStatusStyle.equals(updatedStatusStyle));
        assertEquals(initialUserStyleSheet, updatedUserStyleSheet);
        assertEquals(null, updatedUserStyleSheet);

        FxTestUtil.runFx(() -> {
            pane.unbindThemeProperty();
            pane.dispose();
        });
    }

    @Test
    void toolbarAndStatusUseSharedDensityMetrics(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20135, false);
        FxTestUtil.runFx(() -> {
            root.getChildren().setAll(pane);
            root.applyCss();
            root.layout();
        });

        HBox toolbar = FxTestUtil.callFx(() -> (HBox) pane.lookup("#hugo-preview-toolbar"));
        HBox statusBar = FxTestUtil.callFx(() -> (HBox) pane.lookup("#hugo-preview-status"));

        assertEquals(UiMetrics.SPACE_2, FxTestUtil.callFx(toolbar::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_2, FxTestUtil.callFx(() -> toolbar.getPadding().getTop()), 0.01);
        assertEquals(UiMetrics.SPACE_3, FxTestUtil.callFx(() -> toolbar.getPadding().getRight()), 0.01);
        assertEquals(UiMetrics.TOOLBAR_HEIGHT, FxTestUtil.callFx(toolbar::getMinHeight), 0.01);
        assertEquals(UiMetrics.SPACE_3, FxTestUtil.callFx(statusBar::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_1, FxTestUtil.callFx(() -> statusBar.getPadding().getTop()), 0.01);
        assertEquals(UiMetrics.SPACE_3, FxTestUtil.callFx(() -> statusBar.getPadding().getRight()), 0.01);

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void toolbarNavigationButtonsProvideAccessibleMetadata(@TempDir Path tempDir) throws Exception {
        HugoPreviewPane pane = createPane(tempDir, 20140, false);
        FxTestUtil.runFx(() -> root.getChildren().setAll(pane));

        Button backButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-back"));
        Button forwardButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-forward"));
        Button reloadButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-reload"));
        Button openInBrowserButton = FxTestUtil.callFx(() -> (Button) pane.lookup("#hugo-preview-open-browser"));

        assertNotNull(backButton);
        assertNotNull(forwardButton);
        assertNotNull(reloadButton);
        assertNotNull(openInBrowserButton);

        assertEquals("Previous page", FxTestUtil.callFx(backButton::getAccessibleText));
        assertEquals("Next page", FxTestUtil.callFx(forwardButton::getAccessibleText));
        assertEquals("Reload page", FxTestUtil.callFx(reloadButton::getAccessibleText));
        assertEquals("Open in browser", FxTestUtil.callFx(openInBrowserButton::getAccessibleText));

        assertTrue(FxTestUtil.callFx(backButton::isFocusTraversable));
        assertTrue(FxTestUtil.callFx(forwardButton::isFocusTraversable));
        assertTrue(FxTestUtil.callFx(reloadButton::isFocusTraversable));
        assertTrue(FxTestUtil.callFx(openInBrowserButton::isFocusTraversable));

        assertEquals("Go to previous page", FxTestUtil.callFx(() -> backButton.getTooltip().getText()));
        assertEquals("Go to next page", FxTestUtil.callFx(() -> forwardButton.getTooltip().getText()));
        assertEquals("Reload current page", FxTestUtil.callFx(() -> reloadButton.getTooltip().getText()));
        assertEquals("Open current page in system browser", FxTestUtil.callFx(() -> openInBrowserButton.getTooltip().getText()));

        FxTestUtil.runFx(pane::dispose);
    }

    private HugoPreviewPane createPane(Path siteRoot, int preferredPort, boolean autoStart) throws Exception {
        Path script = writeReadyScript(siteRoot.resolve("hugo-ready.sh"));
        HugoCliProbe probe = new HugoCliProbe(List.of("sh", "-c", "echo hugo"));
        HugoServerProcessManager manager = new HugoServerProcessManager(
            (rootPath, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofSeconds(2)
        );

        return FxTestUtil.callFx(() -> new HugoPreviewPane(
            new HugoPreviewConfig(siteRoot, "hugo:test", "/", preferredPort, autoStart, false),
            probe,
            manager
        ));
    }

    private Path writeReadyScript(Path path) throws Exception {
        Files.writeString(path, """
            #!/bin/sh
            PORT="$1"
            echo "Web Server is available at http://127.0.0.1:${PORT}/"
            while true; do
              sleep 1
            done
            """);
        assertTrue(path.toFile().setExecutable(true));
        return path;
    }

}
