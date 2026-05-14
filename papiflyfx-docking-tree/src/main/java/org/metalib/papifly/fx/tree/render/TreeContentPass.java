package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.metalib.papifly.fx.tree.api.CellState;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.ui.UiMetrics;

final class TreeContentPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        graphics.setFont(context.theme().font());
        for (TreeRenderRow<T> row : context.rows()) {
            paintRow(context, row);
        }
    }

    @Override
    public void renderRow(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        paintRow(context, row);
    }

    private void paintRow(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        if (!row.isItemRow()) {
            return;
        }
        GraphicsContext graphics = context.graphics();
        TreeItem<T> item = row.item();
        drawDisclosure(context, row);
        drawInfoToggle(context, row);

        double iconX = context.textOriginX(row) - context.iconSize() - UiMetrics.SPACE_2;
        double iconY = row.y() + ((row.height() - context.iconSize()) * 0.5);
        drawIcon(context, item, iconX, iconY);

        double textX = context.textOriginX(row);
        double textY = row.y() + ((row.height() - context.glyphCache().getLineHeight()) * 0.5) + context.baseline();
        double textWidth = Math.max(0.0, context.effectiveTextWidth() - textX);
        double textHeight = row.height();
        CellState state = new CellState(
            context.selectionModel().isSelected(item),
            context.selectionModel().getFocusedItem() == item,
            context.hoveredItem() == item,
            row.expanded(),
            row.leaf(),
            row.depth(),
            textX,
            row.y(),
            textWidth,
            textHeight
        );

        if (context.cellRenderer() != null) {
            context.cellRenderer().render(graphics, item.getValue(), context, state);
            return;
        }
        Paint textPaint = context.selectionModel().isSelected(item)
            ? context.theme().textColorSelected()
            : context.theme().textColor();
        graphics.setFill(textPaint);
        graphics.fillText(String.valueOf(item.getValue()), textX, textY);
    }

    private void drawInfoToggle(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        if (!row.infoAvailable()) {
            return;
        }
        GraphicsContext graphics = context.graphics();
        double size = context.infoToggleSize();
        double x = context.infoToggleX();
        double y = row.y() + ((row.height() - size) * 0.5);
        graphics.setStroke(context.theme().disclosureColor());
        graphics.setLineWidth(1.25);
        graphics.strokeRoundRect(x, y, size, size, 3.0, 3.0);
        double cx = x + (size * 0.5);
        double cy = y + (size * 0.5);
        double arm = size * 0.3;
        graphics.strokeLine(cx - arm, cy, cx + arm, cy);
        if (!row.infoExpanded()) {
            graphics.strokeLine(cx, cy - arm, cx, cy + arm);
        }
    }

    private void drawIcon(TreeRenderContext<T> context, TreeItem<T> item, double x, double y) {
        if (context.iconResolver() == null) {
            return;
        }
        T value = item.getValue();
        Image image = context.iconResolver().apply(value);
        if (image == null) {
            return;
        }
        context.graphics().drawImage(image, x, y, context.iconSize(), context.iconSize());
    }

    private void drawDisclosure(TreeRenderContext<T> context, TreeRenderRow<T> row) {
        if (row.leaf()) {
            return;
        }
        GraphicsContext graphics = context.graphics();
        double centerX = context.disclosureOriginX(row);
        double centerY = row.y() + (row.height() * 0.5);
        double size = Math.min(9.0, row.height() * 0.5);
        graphics.setStroke(context.theme().disclosureColor());
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.setLineJoin(StrokeLineJoin.ROUND);
        graphics.setLineWidth(Math.max(1.5, size * 0.22));
        if (row.expanded()) {
            graphics.strokePolyline(
                new double[] {centerX - size * 0.45, centerX, centerX + size * 0.45},
                new double[] {centerY - size * 0.2, centerY + size * 0.35, centerY - size * 0.2},
                3
            );
        } else {
            graphics.strokePolyline(
                new double[] {centerX - size * 0.2, centerX + size * 0.3, centerX - size * 0.2},
                new double[] {centerY - size * 0.45, centerY, centerY + size * 0.45},
                3
            );
        }
    }
}
