package org.metalib.papifly.fx.hugo.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.nio.file.Path;
import java.util.Map;

public final class HugoPreviewStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return HugoPreviewFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof HugoPreviewPane pane)) {
            return Map.of();
        }
        return HugoPreviewStateCodec.toMap(pane.captureState());
    }

    @Override
    public Node restore(LeafContentData content) {
        if (content == null) {
            return null;
        }
        HugoPreviewState state = HugoPreviewStateCodec.fromMap(content.state());
        Path siteRoot = state.siteDir() == null || state.siteDir().isBlank()
            ? Path.of(".")
            : Path.of(state.siteDir());

        HugoPreviewPane pane = new HugoPreviewPane(new HugoPreviewConfig(
            siteRoot,
            content.contentId() == null || content.contentId().isBlank() ? "hugo:restored" : content.contentId(),
            "/",
            1313,
            true,
            false
        ));
        pane.applyState(state);
        return pane;
    }
}
