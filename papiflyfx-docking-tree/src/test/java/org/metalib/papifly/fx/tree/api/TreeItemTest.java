package org.metalib.papifly.fx.tree.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeItemTest {

    @Test
    void addChildSetsParentAndMaintainsOrder() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> childA = new TreeItem<>("a");
        TreeItem<String> childB = new TreeItem<>("b");

        root.addChild(childA);
        root.addChild(0, childB);

        assertEquals(2, root.getChildCount());
        assertSame(childB, root.getChildren().getFirst());
        assertSame(root, childA.getParent());
        assertSame(root, childB.getParent());
    }

    @Test
    void removeChildDetachesParent() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> child = new TreeItem<>("child");
        root.addChild(child);

        assertTrue(root.removeChild(child));
        assertNull(child.getParent());
        assertTrue(root.getChildren().isEmpty());
        assertFalse(root.removeChild(child));
    }

    @Test
    void listenerReceivesMutations() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> child = new TreeItem<>("child");

        AtomicInteger addEvents = new AtomicInteger();
        AtomicInteger removeEvents = new AtomicInteger();
        AtomicInteger valueEvents = new AtomicInteger();
        AtomicInteger expandedEvents = new AtomicInteger();

        root.addListener(new TreeItem.TreeItemListener<>() {
            @Override
            public void onChildAdded(TreeItem<String> parent, TreeItem<String> addedChild, int index) {
                addEvents.incrementAndGet();
            }

            @Override
            public void onChildRemoved(TreeItem<String> parent, TreeItem<String> removedChild, int index) {
                removeEvents.incrementAndGet();
            }

            @Override
            public void onValueChanged(TreeItem<String> item, String oldValue, String newValue) {
                valueEvents.incrementAndGet();
            }

            @Override
            public void onExpandedChanged(TreeItem<String> item, boolean expanded) {
                expandedEvents.incrementAndGet();
            }
        });

        root.addChild(child);
        root.setValue("new-root");
        root.setExpanded(true);
        root.removeChild(child);

        assertEquals(1, addEvents.get());
        assertEquals(1, removeEvents.get());
        assertEquals(1, valueEvents.get());
        assertEquals(1, expandedEvents.get());
    }
}
