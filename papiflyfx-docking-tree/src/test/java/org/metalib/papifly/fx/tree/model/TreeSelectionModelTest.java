package org.metalib.papifly.fx.tree.model;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSelectionModelTest {

    @Test
    void singleSelectionKeepsSingleItem() {
        TreeSelectionModel<String> selectionModel = new TreeSelectionModel<>();
        TreeItem<String> a = new TreeItem<>("a");
        TreeItem<String> b = new TreeItem<>("b");

        selectionModel.select(a);
        selectionModel.select(b);

        assertEquals(1, selectionModel.getSelectedItems().size());
        assertSame(b, selectionModel.getSelectedItems().getFirst());
        assertSame(b, selectionModel.getFocusedItem());
    }

    @Test
    void multiSelectionSupportsRangeSelection() {
        TreeSelectionModel<String> selectionModel = new TreeSelectionModel<>();
        selectionModel.setSelectionMode(TreeSelectionModel.SelectionMode.MULTIPLE);
        TreeItem<String> a = new TreeItem<>("a");
        TreeItem<String> b = new TreeItem<>("b");
        TreeItem<String> c = new TreeItem<>("c");
        List<TreeItem<String>> visible = List.of(a, b, c);

        selectionModel.selectOnly(a);
        selectionModel.selectRange(visible, a, c);

        assertEquals(3, selectionModel.getSelectedItems().size());
        assertTrue(selectionModel.isSelected(a));
        assertTrue(selectionModel.isSelected(b));
        assertTrue(selectionModel.isSelected(c));
    }

    @Test
    void toggleSelectionAddsAndRemovesInMultiMode() {
        TreeSelectionModel<String> selectionModel = new TreeSelectionModel<>();
        selectionModel.setSelectionMode(TreeSelectionModel.SelectionMode.MULTIPLE);
        TreeItem<String> item = new TreeItem<>("item");

        selectionModel.toggleSelection(item);
        assertTrue(selectionModel.isSelected(item));

        selectionModel.toggleSelection(item);
        assertFalse(selectionModel.isSelected(item));
    }
}
