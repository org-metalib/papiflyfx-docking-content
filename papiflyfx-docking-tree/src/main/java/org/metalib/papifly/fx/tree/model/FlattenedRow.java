package org.metalib.papifly.fx.tree.model;

import org.metalib.papifly.fx.tree.api.TreeItem;

public record FlattenedRow<T>(
    RowKind rowKind,
    TreeItem<T> item,
    int depth
) {
    public enum RowKind {
        ITEM,
        INFO
    }

    public boolean isItemRow() {
        return rowKind == RowKind.ITEM;
    }

    public boolean isInfoRow() {
        return rowKind == RowKind.INFO;
    }
}
