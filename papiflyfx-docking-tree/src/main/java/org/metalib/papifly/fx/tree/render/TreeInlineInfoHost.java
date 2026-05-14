package org.metalib.papifly.fx.tree.render;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeNodeInfoProvider;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TreeInlineInfoHost<T> {

    private final Pane layer = new Pane();
    private final Map<TreeItem<T>, Node> nodeByItem = new IdentityHashMap<>();
    private final Map<Node, TreeItem<T>> itemByNode = new IdentityHashMap<>();
    private final Set<TreeItem<T>> mountedItems = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public TreeInlineInfoHost() {
        layer.setManaged(false);
        layer.setPickOnBounds(false);
    }

    public Pane layer() {
        return layer;
    }

    public void sync(
        List<TreeRenderRow<T>> visibleRows,
        TreeNodeInfoProvider<T> provider,
        double width,
        double indentWidth,
        double iconSize,
        double horizontalScrollOffset
    ) {
        if (provider == null) {
            layer.getChildren().clear();
            mountedItems.clear();
            return;
        }
        mountedItems.clear();
        for (TreeRenderRow<T> row : visibleRows) {
            if (!row.isInfoRow()) {
                continue;
            }
            TreeItem<T> item = row.item();
            Node node = nodeByItem.computeIfAbsent(item, key -> createNode(provider, key));
            if (node == null) {
                continue;
            }
            if (node.getParent() != layer) {
                layer.getChildren().add(node);
            }
            node.setManaged(false);
            double x = Math.max(
                0.0,
                (row.depth() * indentWidth) + indentWidth + iconSize + UiMetrics.SPACE_2 - horizontalScrollOffset
            );
            double nodeWidth = Math.max(0.0, width - x);
            node.resizeRelocate(x, row.y(), nodeWidth, Math.max(0.0, row.height()));
            if (node instanceof Parent parent) {
                parent.layout();
            }
            mountedItems.add(item);
        }
        List<TreeItem<T>> toUnmount = new ArrayList<>();
        for (TreeItem<T> item : nodeByItem.keySet()) {
            if (!mountedItems.contains(item)) {
                toUnmount.add(item);
            }
        }
        for (TreeItem<T> item : toUnmount) {
            Node node = nodeByItem.get(item);
            if (node != null) {
                layer.getChildren().remove(node);
            }
        }
    }

    public void clear(TreeNodeInfoProvider<T> provider) {
        for (Map.Entry<TreeItem<T>, Node> entry : nodeByItem.entrySet()) {
            if (provider != null && entry.getValue() != null) {
                provider.disposeContent(entry.getKey(), entry.getValue());
            }
        }
        layer.getChildren().clear();
        mountedItems.clear();
        itemByNode.clear();
        nodeByItem.clear();
    }

    private Node createNode(TreeNodeInfoProvider<T> provider, TreeItem<T> item) {
        Node node = provider.createContent(item);
        if (node != null) {
            itemByNode.put(node, item);
        }
        return node;
    }
}
