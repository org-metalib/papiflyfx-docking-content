package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeNodeInfoProvider;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlattenedTree<T> {

    public interface FlattenedTreeListener {
        void onFlattenedTreeChanged();
    }

    private final List<FlattenedRow<T>> rows = new ArrayList<>();
    private final Map<TreeItem<T>, Integer> itemRowIndexByItem = new IdentityHashMap<>();
    private final Map<TreeItem<T>, Integer> infoRowIndexByItem = new IdentityHashMap<>();
    private final Map<TreeItem<T>, TreeItem.TreeItemListener<T>> treeListeners = new IdentityHashMap<>();
    private final List<FlattenedTreeListener> listeners = new CopyOnWriteArrayList<>();
    private final TreeExpansionModel.ExpansionListener<T> expansionListener = (item, expanded) -> rebuild();
    private final TreeNodeInfoModel.Listener<T> infoListener = (item, expanded) -> rebuild();

    private TreeItem<T> root;
    private TreeExpansionModel<T> expansionModel;
    private TreeNodeInfoModel<T> nodeInfoModel;
    private TreeNodeInfoProvider<T> nodeInfoProvider;
    private boolean showRoot = true;

    public FlattenedTree() {
        this(new TreeExpansionModel<>(), new TreeNodeInfoModel<>());
    }

    public FlattenedTree(TreeExpansionModel<T> expansionModel) {
        this(expansionModel, new TreeNodeInfoModel<>());
    }

    public FlattenedTree(TreeExpansionModel<T> expansionModel, TreeNodeInfoModel<T> nodeInfoModel) {
        this.expansionModel = expansionModel == null ? new TreeExpansionModel<>() : expansionModel;
        this.nodeInfoModel = nodeInfoModel == null ? new TreeNodeInfoModel<>() : nodeInfoModel;
        this.expansionModel.addListener(expansionListener);
        this.nodeInfoModel.addListener(infoListener);
    }

    public TreeItem<T> getRoot() {
        return root;
    }

    public void setRoot(TreeItem<T> root) {
        if (this.root == root) {
            return;
        }
        detachListeners();
        this.root = root;
        if (root != null) {
            attachListeners(root);
        }
        rebuild();
    }

    public TreeExpansionModel<T> getExpansionModel() {
        return expansionModel;
    }

    public void setExpansionModel(TreeExpansionModel<T> expansionModel) {
        TreeExpansionModel<T> resolved = expansionModel == null ? new TreeExpansionModel<>() : expansionModel;
        if (this.expansionModel == resolved) {
            return;
        }
        this.expansionModel.removeListener(expansionListener);
        this.expansionModel = resolved;
        this.expansionModel.addListener(expansionListener);
        rebuild();
    }

    public TreeNodeInfoModel<T> getNodeInfoModel() {
        return nodeInfoModel;
    }

    public void setNodeInfoModel(TreeNodeInfoModel<T> nodeInfoModel) {
        TreeNodeInfoModel<T> resolved = nodeInfoModel == null ? new TreeNodeInfoModel<>() : nodeInfoModel;
        if (this.nodeInfoModel == resolved) {
            return;
        }
        this.nodeInfoModel.removeListener(infoListener);
        this.nodeInfoModel = resolved;
        this.nodeInfoModel.addListener(infoListener);
        rebuild();
    }

    public TreeNodeInfoProvider<T> getNodeInfoProvider() {
        return nodeInfoProvider;
    }

    public void setNodeInfoProvider(TreeNodeInfoProvider<T> nodeInfoProvider) {
        if (this.nodeInfoProvider == nodeInfoProvider) {
            return;
        }
        this.nodeInfoProvider = nodeInfoProvider;
        rebuild();
    }

    public boolean isShowRoot() {
        return showRoot;
    }

    public void setShowRoot(boolean showRoot) {
        if (this.showRoot == showRoot) {
            return;
        }
        this.showRoot = showRoot;
        rebuild();
    }

    public List<FlattenedRow<T>> rows() {
        return List.copyOf(rows);
    }

    public List<TreeItem<T>> visibleItems() {
        List<TreeItem<T>> items = new ArrayList<>(itemRowIndexByItem.size());
        for (FlattenedRow<T> row : rows) {
            if (row.isItemRow()) {
                items.add(row.item());
            }
        }
        return items;
    }

    public int size() {
        return rows.size();
    }

    public int itemRowCount() {
        return itemRowIndexByItem.size();
    }

    public FlattenedRow<T> getRow(int index) {
        return rows.get(index);
    }

    public TreeItem<T> getItem(int index) {
        return rows.get(index).item();
    }

    public boolean isItemRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }
        return rows.get(rowIndex).isItemRow();
    }

    public boolean isInfoRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }
        return rows.get(rowIndex).isInfoRow();
    }

    public TreeItem<T> getOwnerItem(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex).item();
    }

    public int itemRowIndexOf(TreeItem<T> item) {
        Integer index = itemRowIndexByItem.get(item);
        return index == null ? -1 : index;
    }

    public int infoRowIndexOf(TreeItem<T> item) {
        Integer index = infoRowIndexByItem.get(item);
        return index == null ? -1 : index;
    }

    public int depthOf(TreeItem<T> item) {
        Integer index = itemRowIndexByItem.get(item);
        if (index == null) {
            return -1;
        }
        return rows.get(index).depth();
    }

    public int indexOf(TreeItem<T> item) {
        Integer index = itemRowIndexByItem.get(item);
        return index == null ? -1 : index;
    }

    public void addListener(FlattenedTreeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(FlattenedTreeListener listener) {
        listeners.remove(listener);
    }

    public void rebuild() {
        rows.clear();
        itemRowIndexByItem.clear();
        infoRowIndexByItem.clear();
        if (root != null) {
            if (showRoot) {
                flatten(root, 0);
            } else {
                for (TreeItem<T> child : root.getChildren()) {
                    flatten(child, 0);
                }
            }
        }
        notifyChanged();
    }

    private void flatten(TreeItem<T> item, int depth) {
        int itemRowIndex = rows.size();
        rows.add(new FlattenedRow<>(FlattenedRow.RowKind.ITEM, item, depth));
        itemRowIndexByItem.put(item, itemRowIndex);
        if (nodeInfoProvider != null && nodeInfoModel.isExpanded(item)) {
            int infoRowIndex = rows.size();
            rows.add(new FlattenedRow<>(FlattenedRow.RowKind.INFO, item, depth));
            infoRowIndexByItem.put(item, infoRowIndex);
        }
        if (item.isLeaf() || !expansionModel.isExpanded(item)) {
            return;
        }
        for (TreeItem<T> child : item.getChildren()) {
            flatten(child, depth + 1);
        }
    }

    private void attachListeners(TreeItem<T> node) {
        if (node == null || treeListeners.containsKey(node)) {
            return;
        }
        TreeItem.TreeItemListener<T> listener = new TreeItem.TreeItemListener<>() {
            @Override
            public void onChildAdded(TreeItem<T> parent, TreeItem<T> child, int index) {
                attachListeners(child);
                rebuild();
            }

            @Override
            public void onChildRemoved(TreeItem<T> parent, TreeItem<T> child, int index) {
                detachListener(child);
                rebuild();
            }

            @Override
            public void onExpandedChanged(TreeItem<T> item, boolean expanded) {
                expansionModel.setExpanded(item, expanded);
                rebuild();
            }
        };
        node.addListener(listener);
        treeListeners.put(node, listener);
        for (TreeItem<T> child : node.getChildren()) {
            attachListeners(child);
        }
    }

    private void detachListeners() {
        List<TreeItem<T>> nodes = new ArrayList<>(treeListeners.keySet());
        for (TreeItem<T> node : nodes) {
            detachListener(node);
        }
        treeListeners.clear();
    }

    private void detachListener(TreeItem<T> node) {
        TreeItem.TreeItemListener<T> listener = treeListeners.remove(node);
        if (listener != null) {
            node.removeListener(listener);
        }
        for (TreeItem<T> child : node.getChildren()) {
            detachListener(child);
        }
    }

    private void notifyChanged() {
        for (FlattenedTreeListener listener : listeners) {
            listener.onFlattenedTreeChanged();
        }
    }
}
