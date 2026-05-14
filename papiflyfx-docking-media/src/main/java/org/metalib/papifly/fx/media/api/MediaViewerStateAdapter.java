package org.metalib.papifly.fx.media.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.media.model.MediaState;
import org.metalib.papifly.fx.media.model.MediaStateCodec;

import java.util.Map;

public class MediaViewerStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() { return MediaViewerFactory.FACTORY_ID; }

    @Override
    public int getVersion() { return VERSION; }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof MediaViewer mv)) return Map.of();
        return MediaStateCodec.toMap(mv.captureState());
    }

    @Override
    public Node restore(LeafContentData content) {
        MediaViewer mv = new MediaViewer();
        if (content != null && content.state() != null) {
            MediaState state = MediaStateCodec.fromMap(content.state());
            mv.applyState(state);
        }
        return mv;
    }
}
