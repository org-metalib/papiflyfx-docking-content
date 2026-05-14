package org.metalib.papifly.fx.tree.render;

import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.FlattenedRow;

public record TreeRenderRow<T>(
    FlattenedRow.RowKind rowKind,
    TreeItem<T> item,
    int rowIndex,
    int depth,
    double y,
    double height,
    boolean leaf,
    boolean expanded,
    boolean infoAvailable,
    boolean infoExpanded
) {
    public boolean isItemRow() {
        return rowKind == FlattenedRow.RowKind.ITEM;
    }

    public boolean isInfoRow() {
        return rowKind == FlattenedRow.RowKind.INFO;
    }
}
