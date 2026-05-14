package org.metalib.papifly.fx.tree.controller;

import javafx.geometry.Bounds;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.model.FlattenedRow;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class TreeEditController<T> {

    private final TreeViewport<T> viewport;
    private final TextField editor = new TextField();
    private final Runnable redrawAction;

    private BiConsumer<TreeItem<T>, String> commitHandler = (item, text) -> {};
    private TreeItem<T> editingItem;
    private int editingRowIndex = -1;
    private int editingDepth = -1;

    public TreeEditController(TreeViewport<T> viewport, Runnable redrawAction) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.redrawAction = Objects.requireNonNull(redrawAction, "redrawAction");
        editor.setManaged(false);
        editor.setVisible(false);
        editor.setOnAction(event -> commitEdit());
        editor.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (!focused && isEditing()) {
                commitEdit();
            }
        });
        editor.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                event.consume();
            }
        });
    }

    public TextField editorNode() {
        return editor;
    }

    public void setCommitHandler(BiConsumer<TreeItem<T>, String> commitHandler) {
        this.commitHandler = commitHandler == null ? (item, text) -> {} : commitHandler;
    }

    public boolean isEditing() {
        return editingItem != null;
    }

    public TreeItem<T> getEditingItem() {
        return editingItem;
    }

    public void startEdit(TreeViewport.HitInfo<T> hitInfo) {
        if (hitInfo == null || hitInfo.item() == null || hitInfo.rowKind() != FlattenedRow.RowKind.ITEM) {
            return;
        }
        editingItem = hitInfo.item();
        editingRowIndex = hitInfo.rowIndex();
        editingDepth = hitInfo.depth();
        editor.setText(String.valueOf(editingItem.getValue()));
        editor.setVisible(true);
        relayout();
        editor.requestFocus();
        editor.selectAll();
    }

    public void relayout() {
        if (!isEditing()) {
            return;
        }
        Bounds rowBounds = viewport.rowBounds(editingRowIndex);
        if (rowBounds == null) {
            cancelEdit();
            return;
        }
        double x = (editingDepth * viewport.getTheme().indentWidth())
            + viewport.getTheme().indentWidth()
            + viewport.getTheme().iconSize()
            + UiMetrics.SPACE_2
            - viewport.getHorizontalScrollOffset();
        double y = rowBounds.getMinY() + 1.0;
        double width = Math.max(UiMetrics.SPACE_5 * 4.0, rowBounds.getWidth() - x - UiMetrics.SPACE_2);
        double height = Math.max(UiMetrics.SPACE_4, rowBounds.getHeight() - (UiMetrics.SPACE_1 * 0.5));
        editor.resizeRelocate(Math.max(0.0, x), y, width, height);
    }

    public void commitEdit() {
        if (!isEditing()) {
            return;
        }
        commitHandler.accept(editingItem, editor.getText());
        stopEditing();
        redrawAction.run();
    }

    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }
        stopEditing();
        redrawAction.run();
    }

    public void dispose() {
        stopEditing();
        editor.setOnAction(null);
        editor.setOnKeyPressed(null);
    }

    private void stopEditing() {
        editingItem = null;
        editingRowIndex = -1;
        editingDepth = -1;
        editor.setVisible(false);
    }
}
