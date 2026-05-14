package org.metalib.papifly.fx.tree.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.util.Map;

public class TreeViewStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 2;

    @Override
    public String getTypeKey() {
        return TreeViewFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof TreeView<?> treeView)) {
            return Map.of();
        }
        return treeView.captureState();
    }

    @Override
    public Node restore(LeafContentData content) {
        TreeView<String> treeView = TreeViewFactory.createDefaultTreeView();
        if (content != null && content.state() != null) {
            treeView.applyState(content.state());
        }
        return treeView;
    }
}
