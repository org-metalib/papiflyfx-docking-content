package org.metalib.papifly.fx.hugo.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

import java.nio.file.Path;

public final class HugoPreviewFactory implements ContentFactory {

    public static final String FACTORY_ID = "hugo-preview";

    private final Path defaultSiteRoot;

    public HugoPreviewFactory() {
        this(Path.of("."));
    }

    public HugoPreviewFactory(Path defaultSiteRoot) {
        this.defaultSiteRoot = defaultSiteRoot == null ? Path.of(".") : defaultSiteRoot;
    }

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) {
            return null;
        }
        return new HugoPreviewPane(new HugoPreviewConfig(
            defaultSiteRoot,
            "hugo:default",
            "/",
            1313,
            true,
            false
        ));
    }
}
