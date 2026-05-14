package org.metalib.papifly.fx.media.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

public class MediaViewerFactory implements ContentFactory {

    public static final String FACTORY_ID = "media-viewer";

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) return null;
        return new MediaViewer();
    }
}
