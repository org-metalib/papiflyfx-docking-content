package org.metalib.papifly.fx.tree.model;

import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.api.TreeItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlattenedTreeTest {

    @Test
    void flattenedTreeTracksVisibilityFromExpansion() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> branchA = new TreeItem<>("a");
        TreeItem<String> branchB = new TreeItem<>("b");
        TreeItem<String> leafA1 = new TreeItem<>("a1");
        TreeItem<String> leafA2 = new TreeItem<>("a2");

        root.addChild(branchA);
        root.addChild(branchB);
        branchA.addChild(leafA1);
        branchA.addChild(leafA2);

        TreeExpansionModel<String> expansionModel = new TreeExpansionModel<>();
        FlattenedTree<String> flattenedTree = new FlattenedTree<>(expansionModel);
        flattenedTree.setRoot(root);

        assertEquals(1, flattenedTree.size());
        assertSame(root, flattenedTree.getItem(0));

        expansionModel.setExpanded(root, true);
        assertEquals(3, flattenedTree.size());

        expansionModel.setExpanded(branchA, true);
        assertEquals(5, flattenedTree.size());
        assertEquals(1, flattenedTree.indexOf(branchA));
        assertEquals(2, flattenedTree.indexOf(leafA1));
        assertEquals(2, flattenedTree.depthOf(leafA1));
    }

    @Test
    void flattenedTreeRebuildsOnTreeMutation() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeExpansionModel<String> expansionModel = new TreeExpansionModel<>();
        FlattenedTree<String> flattenedTree = new FlattenedTree<>(expansionModel);
        flattenedTree.setRoot(root);
        expansionModel.setExpanded(root, true);

        TreeItem<String> child = new TreeItem<>("child");
        root.addChild(child);
        assertEquals(2, flattenedTree.size());

        root.removeChild(child);
        assertEquals(1, flattenedTree.size());
    }

    @Test
    void flattenedTreeCanHideRootRow() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> branch = new TreeItem<>("branch");
        TreeItem<String> leaf = new TreeItem<>("leaf");
        root.addChild(branch);
        branch.addChild(leaf);

        TreeExpansionModel<String> expansionModel = new TreeExpansionModel<>();
        FlattenedTree<String> flattenedTree = new FlattenedTree<>(expansionModel);
        flattenedTree.setShowRoot(false);
        flattenedTree.setRoot(root);

        expansionModel.setExpanded(root, true);
        expansionModel.setExpanded(branch, true);

        assertEquals(2, flattenedTree.size());
        assertSame(branch, flattenedTree.getItem(0));
        assertSame(leaf, flattenedTree.getItem(1));
        assertEquals(0, flattenedTree.depthOf(branch));
        assertEquals(1, flattenedTree.depthOf(leaf));
        assertEquals(-1, flattenedTree.indexOf(root));
    }

    @Test
    void flattenedTreeIncludesInfoRowsWhenProviderAndInfoExpansionAreSet() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> branch = new TreeItem<>("branch");
        root.addChild(branch);

        TreeExpansionModel<String> expansionModel = new TreeExpansionModel<>();
        TreeNodeInfoModel<String> nodeInfoModel = new TreeNodeInfoModel<>();
        FlattenedTree<String> flattenedTree = new FlattenedTree<>(expansionModel, nodeInfoModel);
        flattenedTree.setShowRoot(false);
        flattenedTree.setNodeInfoProvider(item -> new Pane());
        flattenedTree.setRoot(root);

        expansionModel.setExpanded(root, true);
        nodeInfoModel.setExpanded(branch, true);

        assertEquals(2, flattenedTree.size());
        assertTrue(flattenedTree.isItemRow(0));
        assertTrue(flattenedTree.isInfoRow(1));
        assertSame(branch, flattenedTree.getOwnerItem(1));
        assertEquals(0, flattenedTree.itemRowIndexOf(branch));
        assertEquals(1, flattenedTree.infoRowIndexOf(branch));
        assertEquals(1, flattenedTree.visibleItems().size());
        assertSame(branch, flattenedTree.visibleItems().getFirst());
    }
}
