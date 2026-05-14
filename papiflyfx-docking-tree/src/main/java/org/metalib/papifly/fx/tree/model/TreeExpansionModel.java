package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeExpansionModel<T> {

    public interface ExpansionListener<T> {
        void onExpansionChanged(TreeItem<T> item, boolean expanded);
    }

    private final Set<TreeItem<T>> expandedItems = new LinkedHashSet<>();
    private final List<ExpansionListener<T>> listeners = new CopyOnWriteArrayList<>();

    public boolean isExpanded(TreeItem<T> item) {
        return item != null && (expandedItems.contains(item) || item.isExpanded());
    }

    public void setExpanded(TreeItem<T> item, boolean expanded) {
        if (item == null) {
            return;
        }
        boolean changed;
        if (expanded) {
            changed = expandedItems.add(item);
        } else {
            changed = expandedItems.remove(item);
        }
        if (item.isExpanded() != expanded) {
            item.setExpanded(expanded);
            changed = true;
        }
        if (changed) {
            notifyExpansionChanged(item, expanded);
        }
    }

    public void toggle(TreeItem<T> item) {
        if (item == null) {
            return;
        }
        setExpanded(item, !isExpanded(item));
    }

    public void clear() {
        if (expandedItems.isEmpty()) {
            return;
        }
        Set<TreeItem<T>> previous = new LinkedHashSet<>(expandedItems);
        expandedItems.clear();
        for (TreeItem<T> item : previous) {
            item.setExpanded(false);
            notifyExpansionChanged(item, false);
        }
    }

    public Set<TreeItem<T>> getExpandedItems() {
        return Set.copyOf(expandedItems);
    }

    public void addListener(ExpansionListener<T> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ExpansionListener<T> listener) {
        listeners.remove(listener);
    }

    private void notifyExpansionChanged(TreeItem<T> item, boolean expanded) {
        for (ExpansionListener<T> listener : listeners) {
            listener.onExpansionChanged(item, expanded);
        }
    }
}
