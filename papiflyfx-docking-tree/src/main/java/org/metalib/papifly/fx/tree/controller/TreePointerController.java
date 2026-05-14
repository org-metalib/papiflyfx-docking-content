package org.metalib.papifly.fx.tree.controller;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeExpansionModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoFocusPolicy;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoToggleMode;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TreePointerController<T> {

    private enum ScrollbarDragTarget {
        NONE,
        VERTICAL,
        HORIZONTAL
    }

    private final FlattenedTree<T> flattenedTree;
    private final TreeSelectionModel<T> selectionModel;
    private final TreeExpansionModel<T> expansionModel;
    private final TreeNodeInfoModel<T> nodeInfoModel;
    private final TreeViewport<T> viewport;
    private final TreeEditController<T> editController;
    private Supplier<TreeNodeInfoToggleMode> nodeInfoToggleModeSupplier = () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE;
    private Supplier<TreeNodeInfoFocusPolicy> nodeInfoFocusPolicySupplier = () -> TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM;

    private ScrollbarDragTarget scrollbarDragTarget = ScrollbarDragTarget.NONE;
    private double scrollbarDragPointerOffset;

    public TreePointerController(
        FlattenedTree<T> flattenedTree,
        TreeSelectionModel<T> selectionModel,
        TreeExpansionModel<T> expansionModel,
        TreeNodeInfoModel<T> nodeInfoModel,
        TreeViewport<T> viewport,
        TreeEditController<T> editController
    ) {
        this.flattenedTree = Objects.requireNonNull(flattenedTree, "flattenedTree");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.expansionModel = Objects.requireNonNull(expansionModel, "expansionModel");
        this.nodeInfoModel = Objects.requireNonNull(nodeInfoModel, "nodeInfoModel");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.editController = Objects.requireNonNull(editController, "editController");
    }

    public boolean handleMousePressed(MouseEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        if (handleScrollbarPressed(event)) {
            event.consume();
            return true;
        }

        TreeViewport.HitInfo<T> hitInfo = viewport.hitTest(event.getX(), event.getY());
        if (hitInfo == null || hitInfo.item() == null) {
            return false;
        }

        TreeItem<T> item = hitInfo.item();
        if (hitInfo.isInfoRow()) {
            selectionModel.selectOnly(item);
            selectionModel.setFocusedItem(item);
            selectionModel.setAnchorItem(item);
            viewport.ensureItemVisible(item);
            viewport.markDirty();
            event.consume();
            return true;
        }
        if (hitInfo.infoToggleHit() && resolveNodeInfoToggleMode().allowsMouse()) {
            if (resolveNodeInfoFocusPolicy() == TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM) {
                selectionModel.selectOnly(item);
                selectionModel.setFocusedItem(item);
                selectionModel.setAnchorItem(item);
            }
            nodeInfoModel.toggle(item);
            viewport.ensureItemVisible(item);
            viewport.markDirty();
            event.consume();
            return true;
        }
        if (hitInfo.disclosureHit()) {
            expansionModel.toggle(item);
            viewport.markDirty();
            event.consume();
            return true;
        }

        if (event.isShiftDown() && selectionModel.getSelectionMode() == TreeSelectionModel.SelectionMode.MULTIPLE) {
            TreeItem<T> anchor = selectionModel.getAnchorItem();
            if (anchor == null) {
                anchor = selectionModel.getFocusedItem();
            }
            if (anchor == null) {
                anchor = item;
            }
            List<TreeItem<T>> visibleItems = flattenedTree.visibleItems();
            selectionModel.selectRange(visibleItems, anchor, item);
            selectionModel.setFocusedItem(item);
        } else if (event.isShortcutDown() && selectionModel.getSelectionMode() == TreeSelectionModel.SelectionMode.MULTIPLE) {
            selectionModel.toggleSelection(item);
            selectionModel.setFocusedItem(item);
            selectionModel.setAnchorItem(item);
        } else {
            selectionModel.selectOnly(item);
            selectionModel.setFocusedItem(item);
            selectionModel.setAnchorItem(item);
        }

        if (event.getClickCount() >= 2) {
            editController.startEdit(hitInfo);
        }

        viewport.ensureItemVisible(item);
        viewport.markDirty();
        event.consume();
        return true;
    }

    public void setNodeInfoToggleModeSupplier(Supplier<TreeNodeInfoToggleMode> supplier) {
        this.nodeInfoToggleModeSupplier = supplier == null ? () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : supplier;
    }

    public void setNodeInfoFocusPolicySupplier(Supplier<TreeNodeInfoFocusPolicy> supplier) {
        this.nodeInfoFocusPolicySupplier = supplier == null ? () -> TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM : supplier;
    }

    public boolean handleMouseDragged(MouseEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        if (scrollbarDragTarget == ScrollbarDragTarget.VERTICAL) {
            double targetTop = event.getY() - scrollbarDragPointerOffset;
            viewport.setScrollOffset(viewport.verticalOffsetForThumbTop(targetTop));
            event.consume();
            return true;
        }
        if (scrollbarDragTarget == ScrollbarDragTarget.HORIZONTAL) {
            double targetLeft = event.getX() - scrollbarDragPointerOffset;
            viewport.setHorizontalScrollOffset(viewport.horizontalOffsetForThumbLeft(targetLeft));
            event.consume();
            return true;
        }
        return false;
    }

    public boolean handleMouseReleased(MouseEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        if (scrollbarDragTarget != ScrollbarDragTarget.NONE) {
            scrollbarDragTarget = ScrollbarDragTarget.NONE;
            viewport.setScrollbarActivePart(TreeViewport.ScrollbarPart.NONE);
            updateHover(event.getX(), event.getY());
            event.consume();
            return true;
        }
        return false;
    }

    public void handleMouseMoved(MouseEvent event) {
        if (event == null || event.isConsumed()) {
            return;
        }
        updateHover(event.getX(), event.getY());
    }

    public void handleMouseExited(MouseEvent event) {
        viewport.setHoveredItem(null);
        viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.NONE);
    }

    public boolean handleScroll(ScrollEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        double lineFactor = Math.max(UiMetrics.SPACE_2, viewport.rowHeight() * 0.6);
        double verticalDelta = -event.getDeltaY() * lineFactor / 40.0;
        double horizontalDelta = -event.getDeltaX() * lineFactor / 40.0;
        if (event.isShiftDown() && Math.abs(event.getDeltaY()) > Math.abs(event.getDeltaX())) {
            horizontalDelta += -event.getDeltaY() * lineFactor / 40.0;
            verticalDelta = 0.0;
        }
        if (Math.abs(horizontalDelta) > 0.0) {
            viewport.setHorizontalScrollOffset(viewport.getHorizontalScrollOffset() + horizontalDelta);
        }
        if (Math.abs(verticalDelta) > 0.0) {
            viewport.setScrollOffset(viewport.getScrollOffset() + verticalDelta);
        }
        event.consume();
        return true;
    }

    public void dispose() {
        scrollbarDragTarget = ScrollbarDragTarget.NONE;
        viewport.setScrollbarActivePart(TreeViewport.ScrollbarPart.NONE);
        viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.NONE);
        viewport.setHoveredItem(null);
    }

    private boolean handleScrollbarPressed(MouseEvent event) {
        TreeViewport.ScrollbarGeometry verticalGeometry = viewport.getVerticalScrollbarGeometry();
        if (viewport.isVerticalScrollbarVisible()
            && verticalGeometry != null
            && verticalGeometry.containsTrack(event.getX(), event.getY())) {
            viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.VERTICAL_THUMB);
            viewport.setScrollbarActivePart(TreeViewport.ScrollbarPart.VERTICAL_THUMB);
            if (verticalGeometry.containsThumb(event.getX(), event.getY())) {
                scrollbarDragTarget = ScrollbarDragTarget.VERTICAL;
                scrollbarDragPointerOffset = event.getY() - verticalGeometry.thumbY();
            } else {
                viewport.setScrollOffset(viewport.verticalOffsetForTrackClick(event.getY()));
            }
            return true;
        }

        TreeViewport.ScrollbarGeometry horizontalGeometry = viewport.getHorizontalScrollbarGeometry();
        if (viewport.isHorizontalScrollbarVisible()
            && horizontalGeometry != null
            && horizontalGeometry.containsTrack(event.getX(), event.getY())) {
            viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.HORIZONTAL_THUMB);
            viewport.setScrollbarActivePart(TreeViewport.ScrollbarPart.HORIZONTAL_THUMB);
            if (horizontalGeometry.containsThumb(event.getX(), event.getY())) {
                scrollbarDragTarget = ScrollbarDragTarget.HORIZONTAL;
                scrollbarDragPointerOffset = event.getX() - horizontalGeometry.thumbX();
            } else {
                viewport.setHorizontalScrollOffset(viewport.horizontalOffsetForTrackClick(event.getX()));
            }
            return true;
        }
        scrollbarDragTarget = ScrollbarDragTarget.NONE;
        viewport.setScrollbarActivePart(TreeViewport.ScrollbarPart.NONE);
        return false;
    }

    private void updateHover(double x, double y) {
        if (scrollbarDragTarget != ScrollbarDragTarget.NONE) {
            return;
        }
        TreeViewport.ScrollbarGeometry verticalGeometry = viewport.getVerticalScrollbarGeometry();
        if (viewport.isVerticalScrollbarVisible()
            && verticalGeometry != null
            && verticalGeometry.containsTrack(x, y)) {
            viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.VERTICAL_THUMB);
            viewport.setHoveredItem(null);
            return;
        }
        TreeViewport.ScrollbarGeometry horizontalGeometry = viewport.getHorizontalScrollbarGeometry();
        if (viewport.isHorizontalScrollbarVisible()
            && horizontalGeometry != null
            && horizontalGeometry.containsTrack(x, y)) {
            viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.HORIZONTAL_THUMB);
            viewport.setHoveredItem(null);
            return;
        }
        viewport.setScrollbarHoverPart(TreeViewport.ScrollbarPart.NONE);
        TreeViewport.HitInfo<T> hitInfo = viewport.hitTest(x, y);
        viewport.setHoveredItem(hitInfo == null ? null : hitInfo.item());
    }

    private TreeNodeInfoToggleMode resolveNodeInfoToggleMode() {
        TreeNodeInfoToggleMode mode = nodeInfoToggleModeSupplier.get();
        return mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode;
    }

    private TreeNodeInfoFocusPolicy resolveNodeInfoFocusPolicy() {
        TreeNodeInfoFocusPolicy policy = nodeInfoFocusPolicySupplier.get();
        return policy == null ? TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM : policy;
    }
}
