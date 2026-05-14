package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.tree.api.TreeItem;

final class TreeConnectingLinesPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        graphics.setStroke(context.theme().connectingLineColor());
        for (TreeRenderRow<T> row : context.rows()) {
            if (!row.isItemRow() || row.depth() <= 0) {
                continue;
            }
            double centerY = row.y() + (row.height() * 0.5);
            double indent = row.depth() * context.indentWidth() - context.horizontalScrollOffset();
            double parentIndent = indent - (context.indentWidth() * 0.5);
            graphics.strokeLine(parentIndent, centerY, indent, centerY);
            double verticalEndY = isLastSibling(row) ? centerY : row.y() + row.height();
            graphics.strokeLine(parentIndent, row.y(), parentIndent, verticalEndY);
        }
    }

    private boolean isLastSibling(TreeRenderRow<T> row) {
        if (row == null || row.item() == null) {
            return true;
        }
        TreeItem<T> parent = row.item().getParent();
        if (parent == null) {
            return true;
        }
        int childCount = parent.getChildCount();
        if (childCount <= 0) {
            return true;
        }
        return parent.getChildren().get(childCount - 1) == row.item();
    }
}
