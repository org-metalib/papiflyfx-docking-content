package org.metalib.papifly.fx.tree.api;

import javafx.scene.Node;

public interface TreeNodeInfoProvider<T> {

    Node createContent(TreeItem<T> item);

    default double preferredHeight(TreeItem<T> item, double availableWidth) {
        return 160.0;
    }

    default void disposeContent(TreeItem<T> item, Node content) {
    }
}
