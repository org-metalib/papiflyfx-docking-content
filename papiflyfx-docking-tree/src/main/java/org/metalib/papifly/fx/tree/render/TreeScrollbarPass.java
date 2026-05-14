package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

final class TreeScrollbarPass<T> implements TreeRenderPass<T> {

    @Override
    public void renderFull(TreeRenderContext<T> context) {
        GraphicsContext graphics = context.graphics();
        if (context.verticalScrollbarVisible() && context.verticalScrollbarGeometry() != null) {
            paintScrollbar(graphics, context, context.verticalScrollbarGeometry(), TreeViewport.ScrollbarPart.VERTICAL_THUMB);
        }
        if (context.horizontalScrollbarVisible() && context.horizontalScrollbarGeometry() != null) {
            paintScrollbar(graphics, context, context.horizontalScrollbarGeometry(), TreeViewport.ScrollbarPart.HORIZONTAL_THUMB);
        }
    }

    private void paintScrollbar(
        GraphicsContext graphics,
        TreeRenderContext<T> context,
        TreeViewport.ScrollbarGeometry geometry,
        TreeViewport.ScrollbarPart part
    ) {
        graphics.setFill(context.theme().scrollbarTrackColor());
        graphics.fillRoundRect(
            geometry.trackX(),
            geometry.trackY(),
            geometry.trackWidth(),
            geometry.trackHeight(),
            TreeViewport.SCROLLBAR_RADIUS,
            TreeViewport.SCROLLBAR_RADIUS
        );

        Paint thumbPaint = context.theme().scrollbarThumbColor();
        if (context.scrollbarActivePart() == part) {
            thumbPaint = context.theme().scrollbarThumbActiveColor();
        } else if (context.scrollbarHoverPart() == part) {
            thumbPaint = context.theme().scrollbarThumbHoverColor();
        }
        graphics.setFill(thumbPaint);
        graphics.fillRoundRect(
            geometry.thumbX(),
            geometry.thumbY(),
            geometry.thumbWidth(),
            geometry.thumbHeight(),
            TreeViewport.SCROLLBAR_RADIUS,
            TreeViewport.SCROLLBAR_RADIUS
        );
    }
}
