package org.metalib.papifly.fx.tree.render;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.tree.api.TreeItem;
import org.metalib.papifly.fx.tree.api.TreeNodeInfoProvider;
import org.metalib.papifly.fx.tree.api.TreeView;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class TreeViewportTest {

    private TreeView<String> treeView;
    private TreeItem<String> first;
    private TreeItem<String> second;

    @Start
    void start(Stage stage) {
        treeView = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("root");
        first = new TreeItem<>("first");
        second = new TreeItem<>("second");
        root.addChild(first);
        root.addChild(second);
        treeView.setShowRoot(false);
        treeView.setRoot(root);
        treeView.setNodeInfoProvider(new TreeNodeInfoProvider<>() {
            @Override
            public javafx.scene.Node createContent(TreeItem<String> item) {
                return new VBox(new Label("info-" + item.getValue()));
            }

            @Override
            public double preferredHeight(TreeItem<String> item, double availableWidth) {
                return 140.0;
            }
        });
        treeView.getNodeInfoModel().setExpanded(first, true);
        Scene scene = new Scene(treeView, 320, 180);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void hitTestResolvesItemAndInfoRowsWithVariableHeights() {
        flushLayout();
        Bounds firstItemRow = callOnFx(() -> treeView.getViewport().rowBounds(0));
        Bounds firstInfoRow = callOnFx(() -> treeView.getViewport().rowBounds(1));

        TreeViewport.HitInfo<String> itemHit = callOnFx(() -> treeView.getViewport().hitTest(
            8.0,
            firstItemRow.getMinY() + (firstItemRow.getHeight() * 0.5)
        ));
        TreeViewport.HitInfo<String> infoHit = callOnFx(() -> treeView.getViewport().hitTest(
            8.0,
            firstInfoRow.getMinY() + (firstInfoRow.getHeight() * 0.5)
        ));

        assertTrue(itemHit.isItemRow());
        assertTrue(infoHit.isInfoRow());
        assertSame(first, infoHit.item());
    }

    @Test
    void ensureItemVisibleAccountsForInfoRowHeight() {
        flushLayout();
        runOnFx(() -> {
            treeView.getViewport().setScrollOffset(0.0);
            treeView.getViewport().ensureItemVisible(second);
        });

        assertTrue(callOnFx(() -> treeView.getViewport().getScrollOffset()) > 0.0);
    }

    @Test
    void visibleRowsIncludeInfoRowInViewportWindow() {
        flushLayout();
        List<TreeRenderRow<String>> rows = callOnFx(() -> treeView.getViewport().getVisibleRows());

        assertEquals(3, callOnFx(() -> treeView.getFlattenedTree().size()));
        assertTrue(rows.stream().anyMatch(TreeRenderRow::isInfoRow));
    }

    private void flushLayout() {
        runOnFx(() -> {
            treeView.applyCss();
            treeView.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private <R> R callOnFx(Callable<R> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        final Object[] result = new Object[1];
        final RuntimeException[] error = new RuntimeException[1];
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result[0] = action.call();
            } catch (Exception e) {
                error[0] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        R typed = (R) result[0];
        return typed;
    }
}
