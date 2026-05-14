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
    private WebView webView;

    public SvgViewer(String url) {
        setMinSize(0, 0);
        setAlignment(Pos.CENTER);
        load(url);
        wireTheme();
    }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    @Override
    protected void layoutChildren() {
        if (webView != null) {
            webView.setPrefWidth(getWidth());
            webView.setPrefHeight(getHeight());
        }
        super.layoutChildren();
    }

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
            webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.getEngine().load(url);
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
