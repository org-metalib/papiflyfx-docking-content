package org.metalib.papifly.fx.tree.controller;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Bounds;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeExpansionModel;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class TreeDragDropController<T> {

    public static final DataFormat INTERNAL_TREE_ITEM_PATH = new DataFormat("application/x-papifly-tree-item-path");

    public enum DropPosition {
        BEFORE,
        INSIDE,
        AFTER
    }

    public record DropHint<T>(
        TreeItem<T> targetItem,
        DropPosition position,
        double rowY,
        double rowHeight
    ) {
    }

    private final TreeView<T> treeView;
    private final TreeViewport<T> viewport;
    private final FlattenedTree<T> flattenedTree;
    private final TreeSelectionModel<T> selectionModel;
    private final TreeExpansionModel<T> expansionModel;
    private final Canvas overlayCanvas = new Canvas();

    private TreeItem<T> draggedItem;
    private DropHint<T> currentDropHint;
    private BiConsumer<DropHint<T>, Dragboard> externalDropHandler = (hint, dragboard) -> {};

    public TreeDragDropController(
        TreeView<T> treeView,
        TreeViewport<T> viewport,
        FlattenedTree<T> flattenedTree,
        TreeSelectionModel<T> selectionModel,
        TreeExpansionModel<T> expansionModel
    ) {
        this.treeView = Objects.requireNonNull(treeView, "treeView");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.flattenedTree = Objects.requireNonNull(flattenedTree, "flattenedTree");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.expansionModel = Objects.requireNonNull(expansionModel, "expansionModel");
        overlayCanvas.setManaged(false);
        overlayCanvas.setMouseTransparent(true);
        overlayCanvas.widthProperty().bind(treeView.widthProperty());
        overlayCanvas.heightProperty().bind(treeView.heightProperty());
        installEventHandlers();
    }

    public Canvas overlayCanvas() {
        return overlayCanvas;
    }

    public void setExternalDropHandler(BiConsumer<DropHint<T>, Dragboard> externalDropHandler) {
        this.externalDropHandler = externalDropHandler == null ? (hint, dragboard) -> {} : externalDropHandler;
    }

    public DropHint<T> getCurrentDropHint() {
        return currentDropHint;
    }

    public void dispose() {
        clearDropHint();
        draggedItem = null;
    }

    private void installEventHandlers() {
        viewport.setOnDragDetected(this::handleDragDetected);
        treeView.setOnDragOver(this::handleDragOver);
        treeView.setOnDragDropped(this::handleDragDropped);
        treeView.setOnDragExited(event -> clearDropHint());
        treeView.setOnDragDone(event -> {
            draggedItem = null;
            clearDropHint();
        });
    }

    private void handleDragDetected(MouseEvent event) {
        TreeViewport.HitInfo<T> hitInfo = viewport.hitTest(event.getX(), event.getY());
        if (hitInfo == null || hitInfo.item() == null) {
            return;
        }
        TreeItem<T> sourceItem = hitInfo.item();
        draggedItem = selectionModel.isSelected(sourceItem)
            ? selectionModel.getFocusedItem()
            : sourceItem;
        if (draggedItem == null) {
            draggedItem = sourceItem;
        }
        Dragboard dragboard = treeView.startDragAndDrop(TransferMode.MOVE);
        ClipboardContent content = new ClipboardContent();
        String path = encodePath(pathOf(draggedItem));
        content.put(INTERNAL_TREE_ITEM_PATH, path);
        content.putString(path);
        dragboard.setContent(content);
        event.consume();
    }

    private void handleDragOver(DragEvent event) {
        DropHint<T> dropHint = resolveDropHint(event.getX(), event.getY());
        if (dropHint == null) {
            clearDropHint();
            return;
        }
        if (draggedItem != null) {
            event.acceptTransferModes(TransferMode.MOVE);
        } else {
            event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE);
        }
        currentDropHint = dropHint;
        paintDropHint(dropHint);
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        boolean completed = false;
        if (draggedItem != null && currentDropHint != null) {
            completed = performInternalDrop(draggedItem, currentDropHint);
        } else if (currentDropHint != null) {
            externalDropHandler.accept(currentDropHint, event.getDragboard());
            completed = true;
        }
        event.setDropCompleted(completed);
        draggedItem = null;
        clearDropHint();
        event.consume();
    }

    private DropHint<T> resolveDropHint(double x, double y) {
        TreeViewport.HitInfo<T> hitInfo = viewport.hitTest(x, y);
        if (hitInfo == null || hitInfo.item() == null) {
            return null;
        }
        TreeItem<T> targetItem = hitInfo.item();
        double rowY = hitInfo.y();
        double rowHeight = hitInfo.height();
        if (hitInfo.isInfoRow()) {
            int itemRowIndex = flattenedTree.itemRowIndexOf(targetItem);
            if (itemRowIndex < 0) {
                return null;
            }
            Bounds itemRowBounds = viewport.rowBounds(itemRowIndex);
            if (itemRowBounds == null) {
                return null;
            }
            rowY = itemRowBounds.getMinY();
            rowHeight = itemRowBounds.getHeight();
        }
        double relative = (y - rowY) / Math.max(1.0, rowHeight);
        DropPosition position;
        if (relative < 0.25) {
            position = DropPosition.BEFORE;
        } else if (relative > 0.75) {
            position = DropPosition.AFTER;
        } else {
            position = DropPosition.INSIDE;
        }
        return new DropHint<>(targetItem, position, rowY, rowHeight);
    }

    private void paintDropHint(DropHint<T> dropHint) {
        GraphicsContext graphics = overlayCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
        if (dropHint == null) {
            return;
        }
        graphics.setStroke(viewport.getTheme().focusedBorder());
        graphics.setLineWidth(UiMetrics.SPACE_1 * 0.5);
        switch (dropHint.position()) {
            case BEFORE -> {
                double y = dropHint.rowY() + 1.0;
                graphics.strokeLine(0.0, y, viewport.getEffectiveTextWidth(), y);
            }
            case AFTER -> {
                double y = dropHint.rowY() + dropHint.rowHeight() - 1.0;
                graphics.strokeLine(0.0, y, viewport.getEffectiveTextWidth(), y);
            }
            case INSIDE -> graphics.strokeRect(1.0, dropHint.rowY() + 1.0, Math.max(0.0, viewport.getEffectiveTextWidth() - 2.0), Math.max(0.0, dropHint.rowHeight() - 2.0));
        }
    }

    private void clearDropHint() {
        currentDropHint = null;
        GraphicsContext graphics = overlayCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
    }

    private boolean performInternalDrop(TreeItem<T> sourceItem, DropHint<T> dropHint) {
        TreeItem<T> targetItem = dropHint.targetItem();
        if (sourceItem == null || targetItem == null || sourceItem == targetItem) {
            return false;
        }
        TreeItem<T> sourceParent = sourceItem.getParent();
        if (sourceParent == null) {
            return false;
        }

        TreeItem<T> destinationParent;
        int destinationIndex;
        if (dropHint.position() == DropPosition.INSIDE) {
            destinationParent = targetItem;
            destinationIndex = destinationParent.getChildCount();
        } else {
            destinationParent = targetItem.getParent();
            if (destinationParent == null) {
                return false;
            }
            int targetIndex = destinationParent.indexOfChild(targetItem);
            if (targetIndex < 0) {
                return false;
            }
            destinationIndex = dropHint.position() == DropPosition.AFTER ? targetIndex + 1 : targetIndex;
        }

        if (isDescendant(destinationParent, sourceItem)) {
            return false;
        }

        int sourceIndex = sourceParent.indexOfChild(sourceItem);
        if (sourceIndex < 0) {
            return false;
        }
        sourceParent.removeChild(sourceItem);
        if (destinationParent == sourceParent && sourceIndex < destinationIndex) {
            destinationIndex--;
        }
        destinationIndex = Math.max(0, Math.min(destinationIndex, destinationParent.getChildCount()));
        destinationParent.addChild(destinationIndex, sourceItem);
        expansionModel.setExpanded(destinationParent, true);
        viewport.ensureItemVisible(sourceItem);
        viewport.markDirty();
        return true;
    }

    private boolean isDescendant(TreeItem<T> candidateParent, TreeItem<T> candidateChild) {
        if (candidateParent == null || candidateChild == null) {
            return false;
        }
        TreeItem<T> current = candidateParent;
        while (current != null) {
            if (current == candidateChild) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private List<Integer> pathOf(TreeItem<T> item) {
        List<Integer> path = new ArrayList<>();
        TreeItem<T> current = item;
        while (current != null && current.getParent() != null) {
            TreeItem<T> parent = current.getParent();
            path.addFirst(parent.indexOfChild(current));
            current = parent;
        }
        return path;
    }

    private String encodePath(List<Integer> path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(path.get(i));
        }
        return builder.toString();
    }
}
