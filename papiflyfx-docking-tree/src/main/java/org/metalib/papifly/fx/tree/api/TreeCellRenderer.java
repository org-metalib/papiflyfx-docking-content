package org.metalib.papifly.fx.tree.api;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.tree.render.TreeRenderContext;

@FunctionalInterface
public interface TreeCellRenderer<T> {
    void render(GraphicsContext graphicsContext, T item, TreeRenderContext<T> context, CellState state);
}
