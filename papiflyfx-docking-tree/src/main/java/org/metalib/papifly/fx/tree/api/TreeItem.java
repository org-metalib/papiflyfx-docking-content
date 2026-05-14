package org.metalib.papifly.fx.tree.api;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class TreeItem<T> {

    public interface TreeItemListener<T> {
        default void onChildAdded(TreeItem<T> parent, TreeItem<T> child, int index) {}

        default void onChildRemoved(TreeItem<T> parent, TreeItem<T> child, int index) {}

        default void onValueChanged(TreeItem<T> item, T oldValue, T newValue) {}

        default void onExpandedChanged(TreeItem<T> item, boolean expanded) {}
    }

    private final ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");
    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded", false);
    private final List<TreeItem<T>> children = new ArrayList<>();
    private final List<TreeItemListener<T>> listeners = new CopyOnWriteArrayList<>();

    private TreeItem<T> parent;

    public TreeItem() {
        this(null);
    }

    public TreeItem(T value) {
        this.value.set(value);
        this.value.addListener((obs, oldValue, newValue) -> notifyValueChanged(oldValue, newValue));
        this.expanded.addListener((obs, oldValue, newValue) -> notifyExpandedChanged(Boolean.TRUE.equals(newValue)));
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T value) {
        this.value.set(value);
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public TreeItem<T> getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public List<TreeItem<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getChildCount() {
        return children.size();
    }

    public int indexOfChild(TreeItem<T> child) {
        return children.indexOf(child);
    }

    public void addChild(TreeItem<T> child) {
        addChild(children.size(), child);
    }

    public void addChild(int index, TreeItem<T> child) {
        Objects.requireNonNull(child, "child");
        if (child == this) {
            throw new IllegalArgumentException("Cannot add item as its own child.");
        }
        if (isAncestorOf(child)) {
            throw new IllegalArgumentException("Cannot create cyclic tree relationship.");
        }
        if (index < 0 || index > children.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", size: " + children.size());
        }
        TreeItem<T> oldParent = child.parent;
        if (oldParent != null) {
            oldParent.removeChild(child);
        }
        children.add(index, child);
        child.parent = this;
        notifyChildAdded(child, index);
    }

    public boolean removeChild(TreeItem<T> child) {
        int index = children.indexOf(child);
        if (index < 0) {
            return false;
        }
        TreeItem<T> removed = children.remove(index);
        removed.parent = null;
        notifyChildRemoved(removed, index);
        return true;
    }

    public TreeItem<T> removeChildAt(int index) {
        TreeItem<T> removed = children.remove(index);
        removed.parent = null;
        notifyChildRemoved(removed, index);
        return removed;
    }

    public void clearChildren() {
        for (int i = children.size() - 1; i >= 0; i--) {
            removeChildAt(i);
        }
    }

    public void addListener(TreeItemListener<T> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(TreeItemListener<T> listener) {
        listeners.remove(listener);
    }

    private boolean isAncestorOf(TreeItem<T> candidateAncestor) {
        TreeItem<T> current = this.parent;
        while (current != null) {
            if (current == candidateAncestor) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    private void notifyChildAdded(TreeItem<T> child, int index) {
        for (TreeItemListener<T> listener : listeners) {
            listener.onChildAdded(this, child, index);
        }
    }

    private void notifyChildRemoved(TreeItem<T> child, int index) {
        for (TreeItemListener<T> listener : listeners) {
            listener.onChildRemoved(this, child, index);
        }
    }

    private void notifyValueChanged(T oldValue, T newValue) {
        for (TreeItemListener<T> listener : listeners) {
            listener.onValueChanged(this, oldValue, newValue);
        }
    }

    private void notifyExpandedChanged(boolean expanded) {
        for (TreeItemListener<T> listener : listeners) {
            listener.onExpandedChanged(this, expanded);
        }
    }
}
