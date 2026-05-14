package org.metalib.papifly.fx.tree.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeSelectionModel<T> {

    public enum SelectionMode {
        SINGLE,
        MULTIPLE
    }

    public interface SelectionListener<T> {
        void onSelectionChanged(TreeSelectionModel<T> selectionModel);
    }

    private final ObjectProperty<TreeItem<T>> focusedItem = new SimpleObjectProperty<>(this, "focusedItem");
    private final ObjectProperty<TreeItem<T>> anchorItem = new SimpleObjectProperty<>(this, "anchorItem");
    private final Set<TreeItem<T>> selectedItems = new LinkedHashSet<>();
    private final List<SelectionListener<T>> listeners = new CopyOnWriteArrayList<>();

    private SelectionMode selectionMode = SelectionMode.SINGLE;

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode == null ? SelectionMode.SINGLE : selectionMode;
        if (this.selectionMode == SelectionMode.SINGLE && selectedItems.size() > 1) {
            TreeItem<T> keep = focusedItem.get();
            clearSelectionInternal();
            if (keep != null) {
                selectedItems.add(keep);
            }
            notifySelectionChanged();
        }
    }

    public TreeItem<T> getFocusedItem() {
        return focusedItem.get();
    }

    public void setFocusedItem(TreeItem<T> focusedItem) {
        this.focusedItem.set(focusedItem);
        if (focusedItem != null && anchorItem.get() == null) {
            anchorItem.set(focusedItem);
        }
    }

    public ObjectProperty<TreeItem<T>> focusedItemProperty() {
        return focusedItem;
    }

    public TreeItem<T> getAnchorItem() {
        return anchorItem.get();
    }

    public void setAnchorItem(TreeItem<T> anchorItem) {
        this.anchorItem.set(anchorItem);
    }

    public ObjectProperty<TreeItem<T>> anchorItemProperty() {
        return anchorItem;
    }

    public List<TreeItem<T>> getSelectedItems() {
        return List.copyOf(selectedItems);
    }

    public boolean hasSelection() {
        return !selectedItems.isEmpty();
    }

    public boolean isSelected(TreeItem<T> item) {
        return selectedItems.contains(item);
    }

    public void select(TreeItem<T> item) {
        if (item == null) {
            clearSelection();
            return;
        }
        if (selectionMode == SelectionMode.SINGLE) {
            if (selectedItems.size() == 1 && selectedItems.contains(item) && focusedItem.get() == item) {
                return;
            }
            clearSelectionInternal();
            selectedItems.add(item);
        } else {
            selectedItems.add(item);
        }
        focusedItem.set(item);
        anchorItem.set(item);
        notifySelectionChanged();
    }

    public void selectOnly(TreeItem<T> item) {
        clearSelectionInternal();
        if (item != null) {
            selectedItems.add(item);
            focusedItem.set(item);
            anchorItem.set(item);
        } else {
            focusedItem.set(null);
            anchorItem.set(null);
        }
        notifySelectionChanged();
    }

    public void addSelection(TreeItem<T> item) {
        if (item == null) {
            return;
        }
        if (selectionMode == SelectionMode.SINGLE) {
            selectOnly(item);
            return;
        }
        boolean changed = selectedItems.add(item);
        focusedItem.set(item);
        anchorItem.set(item);
        if (changed) {
            notifySelectionChanged();
        }
    }

    public void toggleSelection(TreeItem<T> item) {
        if (item == null) {
            return;
        }
        if (selectionMode == SelectionMode.SINGLE) {
            selectOnly(item);
            return;
        }
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
            focusedItem.set(item);
            anchorItem.set(item);
        }
        notifySelectionChanged();
    }

    public void clearSelection() {
        if (selectedItems.isEmpty()) {
            return;
        }
        clearSelectionInternal();
        notifySelectionChanged();
    }

    public void selectRange(List<TreeItem<T>> visibleItems, TreeItem<T> rangeStart, TreeItem<T> rangeEnd) {
        if (visibleItems == null || visibleItems.isEmpty() || rangeStart == null || rangeEnd == null) {
            return;
        }
        int startIndex = visibleItems.indexOf(rangeStart);
        int endIndex = visibleItems.indexOf(rangeEnd);
        if (startIndex < 0 || endIndex < 0) {
            return;
        }
        int from = Math.min(startIndex, endIndex);
        int to = Math.max(startIndex, endIndex);
        if (selectionMode == SelectionMode.SINGLE) {
            selectOnly(visibleItems.get(endIndex));
            return;
        }
        clearSelectionInternal();
        for (int i = from; i <= to; i++) {
            selectedItems.add(visibleItems.get(i));
        }
        TreeItem<T> focused = visibleItems.get(endIndex);
        focusedItem.set(focused);
        notifySelectionChanged();
    }

    public void addListener(SelectionListener<T> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(SelectionListener<T> listener) {
        listeners.remove(listener);
    }

    public List<TreeItem<T>> selectedItemsAsList() {
        return new ArrayList<>(selectedItems);
    }

    private void clearSelectionInternal() {
        selectedItems.clear();
    }

    private void notifySelectionChanged() {
        for (SelectionListener<T> listener : listeners) {
            listener.onSelectionChanged(this);
        }
    }
}
