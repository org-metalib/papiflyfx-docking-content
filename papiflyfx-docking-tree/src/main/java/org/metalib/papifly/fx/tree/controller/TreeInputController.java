package org.metalib.papifly.fx.tree.controller;

import javafx.geometry.Bounds;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeExpansionModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoToggleMode;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.render.TreeViewport;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TreeInputController<T> {

    private static final boolean IS_MAC_OS = detectMacOs();

    private final FlattenedTree<T> flattenedTree;
    private final TreeSelectionModel<T> selectionModel;
    private final TreeExpansionModel<T> expansionModel;
    private final TreeNodeInfoModel<T> nodeInfoModel;
    private final TreeViewport<T> viewport;
    private final TreeEditController<T> editController;
    private Predicate<TreeItem<T>> navigationSelectable = item -> true;
    private Supplier<TreeNodeInfoToggleMode> nodeInfoToggleModeSupplier = () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE;

    public TreeInputController(
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

    public boolean handleKeyPressed(KeyEvent event) {
        if (event == null || event.isConsumed() || visibleItems().isEmpty()) {
            return false;
        }
        if (isToggleFocusedInfoShortcut(event) && resolveNodeInfoToggleMode().allowsKeyboard()) {
            return consume(event, toggleFocusedInfo());
        }
        KeyCode code = event.getCode();
        boolean extendSelection = event.isShiftDown();
        return switch (code) {
            case UP -> consume(event, moveFocusBy(-1, extendSelection));
            case DOWN -> consume(event, moveFocusBy(1, extendSelection));
            case HOME -> consume(event, focusByIndex(0, 1, extendSelection));
            case END -> consume(event, focusByIndex(visibleItems().size() - 1, -1, extendSelection));
            case PAGE_UP -> consume(event, moveFocusBy(-pageJumpSize(), extendSelection));
            case PAGE_DOWN -> consume(event, moveFocusBy(pageJumpSize(), extendSelection));
            case LEFT -> consume(event, navigateLeft());
            case RIGHT -> consume(event, navigateRight());
            case ENTER -> consume(event, toggleFocusedExpansion());
            case SPACE -> consume(event, handleSpace());
            case F2 -> consume(event, beginEditFocusedItem());
            default -> false;
        };
    }

    public void setNavigationSelectablePredicate(Predicate<TreeItem<T>> navigationSelectable) {
        this.navigationSelectable = navigationSelectable == null ? item -> true : navigationSelectable;
    }

    public void setNodeInfoToggleModeSupplier(Supplier<TreeNodeInfoToggleMode> supplier) {
        this.nodeInfoToggleModeSupplier = supplier == null ? () -> TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : supplier;
    }

    private boolean moveFocusBy(int delta, boolean extendSelection) {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        List<TreeItem<T>> visibleItems = visibleItems();
        int currentIndex = visibleItems.indexOf(focused);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int unclampedTarget = currentIndex + delta;
        int targetIndex = clamp(unclampedTarget, 0, visibleItems.size() - 1);
        int direction = delta < 0 ? -1 : 1;
        targetIndex = findSelectableIndex(visibleItems, targetIndex, direction);
        if (targetIndex < 0) {
            return true;
        }
        return focusByVisibleIndex(visibleItems, targetIndex, extendSelection);
    }

    private boolean focusByIndex(int index, int searchDirection, boolean extendSelection) {
        List<TreeItem<T>> visibleItems = visibleItems();
        int targetIndex = findSelectableIndex(visibleItems, index, searchDirection);
        if (targetIndex < 0) {
            return selectionModel.getFocusedItem() != null;
        }
        return focusByVisibleIndex(visibleItems, targetIndex, extendSelection);
    }

    private boolean focusByVisibleIndex(List<TreeItem<T>> visibleItems, int index, boolean extendSelection) {
        if (index < 0 || index >= visibleItems.size()) {
            return false;
        }
        TreeItem<T> target = visibleItems.get(index);
        if (!isNavigationSelectable(target)) {
            return false;
        }
        focusItem(target, visibleItems, extendSelection);
        return true;
    }

    private void focusItem(TreeItem<T> item, List<TreeItem<T>> visibleItems, boolean extendSelection) {
        if (item == null) {
            return;
        }
        if (extendSelection && selectionModel.getSelectionMode() == TreeSelectionModel.SelectionMode.MULTIPLE) {
            TreeItem<T> anchor = selectionModel.getAnchorItem();
            if (anchor == null) {
                anchor = selectionModel.getFocusedItem();
            }
            if (anchor == null) {
                anchor = item;
            }
            selectionModel.selectRange(visibleItems, anchor, item);
        } else {
            selectionModel.selectOnly(item);
        }
        selectionModel.setFocusedItem(item);
        if (!extendSelection) {
            selectionModel.setAnchorItem(item);
        }
        viewport.ensureItemVisible(item);
        viewport.markDirty();
    }

    private boolean navigateLeft() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        if (!focused.isLeaf() && expansionModel.isExpanded(focused)) {
            expansionModel.setExpanded(focused, false);
            viewport.markDirty();
            return true;
        }
        TreeItem<T> target = findSelectableAncestor(focused.getParent());
        if (target == null) {
            return true;
        }
        focusItem(target, visibleItems(), false);
        return true;
    }

    private boolean navigateRight() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        if (focused.isLeaf()) {
            return true;
        }
        if (!expansionModel.isExpanded(focused)) {
            expansionModel.setExpanded(focused, true);
            viewport.markDirty();
            return true;
        }
        TreeItem<T> target = findFirstSelectableDescendant(focused);
        if (target != null) {
            focusItem(target, visibleItems(), false);
        }
        return true;
    }

    private boolean toggleFocusedExpansion() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null || focused.isLeaf()) {
            return false;
        }
        expansionModel.toggle(focused);
        viewport.markDirty();
        return true;
    }

    private boolean toggleFocusedInfo() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        nodeInfoModel.toggle(focused);
        viewport.ensureItemVisible(focused);
        viewport.markDirty();
        return true;
    }

    private boolean handleSpace() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        if (selectionModel.getSelectionMode() == TreeSelectionModel.SelectionMode.MULTIPLE) {
            selectionModel.toggleSelection(focused);
            selectionModel.setFocusedItem(focused);
            viewport.markDirty();
            return true;
        }
        if (!focused.isLeaf()) {
            expansionModel.toggle(focused);
            viewport.markDirty();
            return true;
        }
        return false;
    }

    private boolean beginEditFocusedItem() {
        TreeItem<T> focused = ensureFocusedItem();
        if (focused == null) {
            return false;
        }
        int rowIndex = flattenedTree.itemRowIndexOf(focused);
        if (rowIndex < 0) {
            return false;
        }
        Bounds rowBounds = viewport.rowBounds(rowIndex);
        if (rowBounds == null) {
            return false;
        }
        double sampleY = rowBounds.getMinY() + Math.min(1.0, Math.max(0.0, rowBounds.getHeight() * 0.5));
        TreeViewport.HitInfo<T> hitInfo = viewport.hitTest(1.0, sampleY);
        if (hitInfo == null) {
            return false;
        }
        editController.startEdit(hitInfo);
        return true;
    }

    private TreeItem<T> ensureFocusedItem() {
        TreeItem<T> focused = selectionModel.getFocusedItem();
        if (focused != null && flattenedTree.itemRowIndexOf(focused) >= 0) {
            return focused;
        }
        List<TreeItem<T>> visibleItems = visibleItems();
        if (visibleItems.isEmpty()) {
            return null;
        }
        int firstSelectableIndex = findSelectableIndex(visibleItems, 0, 1);
        if (firstSelectableIndex < 0) {
            return null;
        }
        TreeItem<T> first = visibleItems.get(firstSelectableIndex);
        selectionModel.selectOnly(first);
        selectionModel.setFocusedItem(first);
        selectionModel.setAnchorItem(first);
        return first;
    }

    private int pageJumpSize() {
        double rowHeight = Math.max(1.0, viewport.rowHeight());
        return Math.max(1, (int) Math.floor(viewport.getEffectiveTextHeight() / rowHeight));
    }

    private boolean consume(KeyEvent event, boolean handled) {
        if (handled) {
            event.consume();
        }
        return handled;
    }

    private boolean isToggleFocusedInfoShortcut(KeyEvent event) {
        if (event == null) {
            return false;
        }
        if (IS_MAC_OS) {
            return event.getCode() == KeyCode.I
                && event.isMetaDown()
                && !event.isAltDown()
                && !event.isControlDown();
        }
        return event.getCode() == KeyCode.ENTER && event.isAltDown();
    }

    private TreeNodeInfoToggleMode resolveNodeInfoToggleMode() {
        TreeNodeInfoToggleMode mode = nodeInfoToggleModeSupplier.get();
        return mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode;
    }

    private int findSelectableIndex(List<TreeItem<T>> visibleItems, int startIndex, int direction) {
        if (visibleItems.isEmpty()) {
            return -1;
        }
        int step = direction < 0 ? -1 : 1;
        int index = clamp(startIndex, 0, visibleItems.size() - 1);
        while (index >= 0 && index < visibleItems.size()) {
            TreeItem<T> item = visibleItems.get(index);
            if (isNavigationSelectable(item)) {
                return index;
            }
            index += step;
        }
        return -1;
    }

    private boolean isNavigationSelectable(TreeItem<T> item) {
        return item != null && navigationSelectable.test(item);
    }

    private TreeItem<T> findSelectableAncestor(TreeItem<T> start) {
        TreeItem<T> current = start;
        while (current != null) {
            if (isNavigationSelectable(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private TreeItem<T> findFirstSelectableDescendant(TreeItem<T> parent) {
        if (parent == null) {
            return null;
        }
        for (TreeItem<T> child : parent.getChildren()) {
            if (isNavigationSelectable(child)) {
                return child;
            }
            TreeItem<T> nested = findFirstSelectableDescendant(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private List<TreeItem<T>> visibleItems() {
        return flattenedTree.visibleItems();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static boolean detectMacOs() {
        return System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("mac");
    }
}
