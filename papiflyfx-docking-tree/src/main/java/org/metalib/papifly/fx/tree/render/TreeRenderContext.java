package org.metalib.papifly.fx.tree.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import org.metalib.papifly.fx.tree.api.TreeCellRenderer;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.tree.util.GlyphCache;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.List;
import java.util.function.Function;

public record TreeRenderContext<T>(
    GraphicsContext graphics,
    TreeViewTheme theme,
    GlyphCache glyphCache,
    List<TreeRenderRow<T>> rows,
    TreeSelectionModel<T> selectionModel,
    TreeItem<T> hoveredItem,
    double viewportWidth,
    double viewportHeight,
    double effectiveTextWidth,
    double effectiveTextHeight,
    double rowHeight,
    double indentWidth,
    double iconSize,
    double baseline,
    double scrollOffset,
    double horizontalScrollOffset,
    boolean verticalScrollbarVisible,
    boolean horizontalScrollbarVisible,
    TreeViewport.ScrollbarGeometry verticalScrollbarGeometry,
    TreeViewport.ScrollbarGeometry horizontalScrollbarGeometry,
    TreeViewport.ScrollbarPart scrollbarHoverPart,
    TreeViewport.ScrollbarPart scrollbarActivePart,
    Function<T, Image> iconResolver,
    TreeCellRenderer<T> cellRenderer
) {
    private static final double INFO_TOGGLE_MARGIN = UiMetrics.SPACE_1;
    private static final double INFO_TOGGLE_SIZE = UiMetrics.SPACE_3;

    public double textOriginX(TreeRenderRow<T> row) {
        return row.depth() * indentWidth + indentWidth + iconSize + UiMetrics.SPACE_2 - horizontalScrollOffset;
    }

    public double disclosureOriginX(TreeRenderRow<T> row) {
        return row.depth() * indentWidth + (indentWidth * 0.5) - horizontalScrollOffset;
    }

    public double infoToggleX() {
        return effectiveTextWidth - INFO_TOGGLE_MARGIN - INFO_TOGGLE_SIZE;
    }

    public double infoToggleSize() {
        return INFO_TOGGLE_SIZE;
    }
}
