package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TreeNodeInfoModel<T> {

    public interface Listener<T> {
        void onInfoExpandedChanged(TreeItem<T> item, boolean expanded);
    }

    private final Set<TreeItem<T>> expandedItems = new LinkedHashSet<>();
    private final List<Listener<T>> listeners = new CopyOnWriteArrayList<>();
    private TreeNodeInfoMode mode = TreeNodeInfoMode.SINGLE;

    public boolean isExpanded(TreeItem<T> item) {
        return item != null && expandedItems.contains(item);
    }

    public Set<TreeItem<T>> getExpandedItems() {
        return Set.copyOf(expandedItems);
    }

    public TreeNodeInfoMode getMode() {
        return mode;
    }

    public void setMode(TreeNodeInfoMode mode) {
        TreeNodeInfoMode nextMode = mode == null ? TreeNodeInfoMode.SINGLE : mode;
        if (this.mode == nextMode) {
            return;
        }
        this.mode = nextMode;
        if (nextMode == TreeNodeInfoMode.SINGLE && expandedItems.size() > 1) {
            List<TreeItem<T>> snapshot = new ArrayList<>(expandedItems);
            TreeItem<T> keep = snapshot.getFirst();
            for (int i = 1; i < snapshot.size(); i++) {
                TreeItem<T> item = snapshot.get(i);
                expandedItems.remove(item);
                notifyChanged(item, false);
            }
            expandedItems.add(keep);
        }
    }

    public void toggle(TreeItem<T> item) {
        if (item == null) {
            return;
        }
        setExpanded(item, !expandedItems.contains(item));
    }

    public void setExpanded(TreeItem<T> item, boolean expanded) {
        if (item == null) {
            return;
        }
        if (expanded) {
            expand(item);
        } else {
            collapse(item);
        }
    }

    public void clear() {
        if (expandedItems.isEmpty()) {
            return;
        }
        List<TreeItem<T>> snapshot = new ArrayList<>(expandedItems);
        expandedItems.clear();
        for (TreeItem<T> item : snapshot) {
            notifyChanged(item, false);
        }
    }

    public void addListener(Listener<T> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }

    private void expand(TreeItem<T> item) {
        if (expandedItems.contains(item)) {
            return;
        }
        if (mode == TreeNodeInfoMode.SINGLE && !expandedItems.isEmpty()) {
            List<TreeItem<T>> snapshot = new ArrayList<>(expandedItems);
            expandedItems.clear();
            for (TreeItem<T> existing : snapshot) {
                notifyChanged(existing, false);
            }
        }
        expandedItems.add(item);
        notifyChanged(item, true);
    }

    private void collapse(TreeItem<T> item) {
        if (!expandedItems.remove(item)) {
            return;
        }
        notifyChanged(item, false);
    }

    private void notifyChanged(TreeItem<T> item, boolean expanded) {
        for (Listener<T> listener : listeners) {
            listener.onInfoExpandedChanged(item, expanded);
        }
    }
}
