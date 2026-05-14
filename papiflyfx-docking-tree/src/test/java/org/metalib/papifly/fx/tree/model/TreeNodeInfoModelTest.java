package org.metalib.papifly.fx.tree.model;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.api.TreeItem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeNodeInfoModelTest {

    @Test
    void singleModeCollapsesPreviouslyExpandedItem() {
        TreeNodeInfoModel<String> model = new TreeNodeInfoModel<>();
        TreeItem<String> first = new TreeItem<>("first");
        TreeItem<String> second = new TreeItem<>("second");

        model.setExpanded(first, true);
        model.setExpanded(second, true);

        assertFalse(model.isExpanded(first));
        assertTrue(model.isExpanded(second));
    }

    @Test
    void multipleModeKeepsMultipleItemsExpanded() {
        TreeNodeInfoModel<String> model = new TreeNodeInfoModel<>();
        TreeItem<String> first = new TreeItem<>("first");
        TreeItem<String> second = new TreeItem<>("second");
        model.setMode(TreeNodeInfoMode.MULTIPLE);

        model.setExpanded(first, true);
        model.setExpanded(second, true);

        assertTrue(model.isExpanded(first));
        assertTrue(model.isExpanded(second));
    }

    @Test
    void clearAndToggleUpdateExpansionState() {
        TreeNodeInfoModel<String> model = new TreeNodeInfoModel<>();
        TreeItem<String> first = new TreeItem<>("first");

        model.toggle(first);
        assertTrue(model.isExpanded(first));

        model.toggle(first);
        assertFalse(model.isExpanded(first));

        model.setExpanded(first, true);
        model.clear();
        assertFalse(model.isExpanded(first));
    }
}
