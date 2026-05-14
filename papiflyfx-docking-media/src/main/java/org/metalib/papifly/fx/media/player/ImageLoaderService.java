package org.metalib.papifly.fx.media.player;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

public class ImageLoaderService {

    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(0.0);
    private final ObjectProperty<Exception> errorProperty = new SimpleObjectProperty<>();
    private Image current;

    public void load(String url) {
        dispose();
        current = new Image(url, 0, 0, true, true, true);
        progressProperty.bind(current.progressProperty());
        current.errorProperty().addListener((obs, o, n) -> {
            if (n) errorProperty.set(current.getException());
        });
        current.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !current.isError()) {
                imageProperty.set(current);
            }
        });
    }

    public ObjectProperty<Image> imageProperty()    { return imageProperty; }
    public ReadOnlyDoubleProperty progressProperty() { return progressProperty; }
    public ObjectProperty<Exception> errorProperty() { return errorProperty; }

    public void dispose() {
        progressProperty.unbind();
        current = null;
        imageProperty.set(null);
        errorProperty.set(null);
        progressProperty.set(0.0);
    }
}
