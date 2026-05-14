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
        if (themeProperty.isBound()) {
            themeProperty.unbind();
        }
        if (external == null) {
            return;
        }
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
