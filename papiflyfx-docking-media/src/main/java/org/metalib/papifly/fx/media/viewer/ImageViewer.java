package org.metalib.papifly.fx.media.viewer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.media.controls.ZoomControls;
import org.metalib.papifly.fx.media.player.ImageLoaderService;

public class ImageViewer extends StackPane {

    private static final double MIN_ZOOM = 0.05;
    private static final double MAX_ZOOM = 16.0;

    private final ImageLoaderService loaderService = new ImageLoaderService();
    private final StackPane imageViewport = new StackPane();
    private final ImageView imageView = new ImageView();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Rectangle viewportClip = new Rectangle();
    private final Scale scaleXform = new Scale(1, 1, 0, 0);
    private final Translate panXform = new Translate(0, 0);

    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1.0);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();

    private double dragStartX, dragStartY;
    private final ZoomControls zoomControls;

    public ImageViewer() {
        setMinSize(0, 0);
        setAlignment(Pos.CENTER);

        imageViewport.setMinSize(0, 0);
        imageViewport.setAlignment(Pos.CENTER);
        imageViewport.setClip(viewportClip);
        viewportClip.widthProperty().bind(imageViewport.widthProperty());
        viewportClip.heightProperty().bind(imageViewport.heightProperty());

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getTransforms().addAll(scaleXform, panXform);
        imageViewport.getChildren().add(imageView);

        zoomControls = new ZoomControls(this::adjustZoom, this::resetZoom);
        StackPane.setAlignment(zoomControls, Pos.TOP_RIGHT);

        getChildren().addAll(imageViewport, progress, zoomControls);

        wireLoader();
        wireZoom();
        wirePan();
        wireTheme();
    }

    @Override
    protected void layoutChildren() {
        imageView.setFitWidth(getWidth());
        imageView.setFitHeight(getHeight());
        super.layoutChildren();
        clampPanToViewport();
    }

    public void load(String url) { loaderService.load(url); }

    public ObjectProperty<Theme> themeProperty() { return themeProperty; }

    public double getZoomLevel() { return zoomLevel.get(); }
    public void setZoomLevel(double z) { zoomLevel.set(clamp(z, MIN_ZOOM, MAX_ZOOM)); }

    public double getPanX() { return panXform.getX(); }
    public double getPanY() { return panXform.getY(); }

    public void dispose() { loaderService.dispose(); }

    private void wireLoader() {
        loaderService.imageProperty().addListener((obs, o, img) -> {
            imageView.setImage(img);
            progress.setVisible(false);
            clampPanToViewport();
        });
        loaderService.progressProperty().addListener((obs, o, n) -> {
            progress.setVisible(n.doubleValue() < 1.0);
            progress.setProgress(n.doubleValue());
        });
        loaderService.errorProperty().addListener((obs, o, ex) -> {
            if (ex != null) progress.setVisible(false);
        });
    }

    private void wireZoom() {
        zoomLevel.addListener((obs, o, n) -> {
            double z = n.doubleValue();
            scaleXform.setX(z);
            scaleXform.setY(z);
            zoomControls.setZoomLevel(z);
            clampPanToViewport();
        });

        setOnScroll((ScrollEvent e) -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
            adjustZoom(factor - 1.0);
            e.consume();
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                resetZoom();
            }
        });
    }

    private void wirePan() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getSceneX() - panXform.getX();
                dragStartY = e.getSceneY() - panXform.getY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                setPanClamped(e.getSceneX() - dragStartX, e.getSceneY() - dragStartY);
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
                    javafx.geometry.Insets.EMPTY)));
            zoomControls.applyTheme(t);
        });
    }

    private void adjustZoom(double delta) {
        setZoomLevel(zoomLevel.get() + delta * zoomLevel.get());
    }

    private void resetZoom() {
        zoomLevel.set(1.0);
        panXform.setX(0);
        panXform.setY(0);
    }

    private void clampPanToViewport() {
        setPanClamped(panXform.getX(), panXform.getY());
    }

    private void setPanClamped(double panX, double panY) {
        double maxPanX = maxPanX();
        double maxPanY = maxPanY();
        panXform.setX(clamp(panX, -maxPanX, maxPanX));
        panXform.setY(clamp(panY, -maxPanY, maxPanY));
    }

    private double maxPanX() {
        double contentWidth = fittedImageWidth() * zoomLevel.get();
        return Math.max(0.0, (contentWidth - getWidth()) / 2.0);
    }

    private double maxPanY() {
        double contentHeight = fittedImageHeight() * zoomLevel.get();
        return Math.max(0.0, (contentHeight - getHeight()) / 2.0);
    }

    private double fittedImageWidth() {
        Image image = imageView.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return getWidth();
        }
        double viewportWidth = getWidth();
        double viewportHeight = getHeight();
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return 0.0;
        }
        double scale = Math.min(viewportWidth / image.getWidth(), viewportHeight / image.getHeight());
        if (!Double.isFinite(scale) || scale <= 0) return 0.0;
        return image.getWidth() * scale;
    }

    private double fittedImageHeight() {
        Image image = imageView.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return getHeight();
        }
        double viewportWidth = getWidth();
        double viewportHeight = getHeight();
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return 0.0;
        }
        double scale = Math.min(viewportWidth / image.getWidth(), viewportHeight / image.getHeight());
        if (!Double.isFinite(scale) || scale <= 0) return 0.0;
        return image.getHeight() * scale;
    }

    void setPanForTesting(double panX, double panY) {
        setPanClamped(panX, panY);
    }

    double maxPanXForTesting() {
        return maxPanX();
    }

    double maxPanYForTesting() {
        return maxPanY();
    }

    boolean hasImageForTesting() {
        return imageView.getImage() != null;
    }

    boolean hasViewportClipForTesting() {
        return imageViewport.getClip() == viewportClip;
    }

    double viewportClipWidthForTesting() {
        return viewportClip.getWidth();
    }

    double viewportClipHeightForTesting() {
        return viewportClip.getHeight();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
