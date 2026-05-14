package org.metalib.papifly.fx.github.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.util.Map;

public class GitHubToolbarStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return GitHubToolbar.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (content instanceof GitHubToolbar toolbar) {
            return toolbar.captureState();
        }
        return Map.of();
    }

    @Override
    public Node restore(LeafContentData content) {
        if (content == null || content.state() == null || content.state().isEmpty()) {
            return null;
        }
        return GitHubToolbar.fromState(content.state());
    }
}
