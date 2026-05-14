package org.metalib.papifly.fx.tree.api;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoFocusPolicy;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoMode;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoToggleMode;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.tree.search.TreeSearchOverlay;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class TreeViewFxTest {

    private TreeView<String> treeView;

    @Start
    void start(Stage stage) {
        treeView = new TreeView<>();
        treeView.setEditCommitHandler(TreeItem::setValue);
        treeView.setRoot(createLargeTree());
        Scene scene = new Scene(treeView, 480, 320);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void viewportDirtyFlagCyclesThroughLayout() {
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getViewport().isDirty()));

        assertTrue(callOnFx(() -> {
            treeView.getViewport().markDirty();
            return treeView.getViewport().isDirty();
        }));

        flushLayout();
        assertFalse(callOnFx(() -> treeView.getViewport().isDirty()));
    }

    @Test
    void viewportScrollOffsetUpdatesForLargeTree() {
        flushLayout();
        runOnFx(() -> treeView.getViewport().setScrollOffset(600.0));
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getViewport().getScrollOffset()) > 0.0);
    }

    @Test
    void keyboardNavigationMovesFocusToNextVisibleRow() {
        flushLayout();
        runOnFx(() -> {
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("node-0", focused.getValue());
    }

    @Test
    void keyboardNavigationSkipsRowsExcludedByNavigationPredicate() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample"));
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> !"heading".equals(item.getValue()));
        });
        flushLayout();
        runOnFx(() -> {
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("sample", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesBoundaryKeyWhenNoFurtherSelectableRow() {
        KeyEvent upEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> !"heading".equals(item.getValue()));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(upEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(upEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesLeftWhenNoSelectableAncestor() {
        KeyEvent leftEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> item != null && item.getValue() != null && item.getValue().startsWith("sample-"));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(leftEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(leftEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void keyboardNavigationConsumesRightOnLeaf() {
        KeyEvent rightEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false);
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("heading"));
            root.addChild(new TreeItem<>("sample-1"));
            root.addChild(new TreeItem<>("sample-2"));
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.setNavigationSelectablePredicate(item -> item != null && item.getValue() != null && item.getValue().startsWith("sample-"));
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.getOnKeyPressed().handle(rightEvent);
            treeView.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false));
        });
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertTrue(rightEvent.isConsumed());
        assertNotNull(focused);
        assertEquals("sample-2", focused.getValue());
    }

    @Test
    void hitTestIgnoresVerticalScrollbarArea() {
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getViewport().isVerticalScrollbarVisible()));
        TreeViewport.ScrollbarGeometry geometry = callOnFx(() -> treeView.getViewport().getVerticalScrollbarGeometry());
        assertNotNull(geometry);

        TreeViewport.HitInfo<String> hitInfo = callOnFx(() -> treeView.getViewport().hitTest(
            geometry.trackX() + (geometry.trackWidth() * 0.5),
            geometry.trackY() + Math.max(1.0, geometry.trackHeight() * 0.5)
        ));
        assertNull(hitInfo);
    }

    @Test
    void cmdOrCtrlFOpensSearchOverlay() {
        runOnFx(() -> {
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, true, false, false));
        });
        assertTrue(callOnFx(treeView::isSearchOpen));
    }

    @Test
    void searchOverlayRemainsCompactAndUsesSearchStyling() {
        runOnFx(() -> treeView.openSearch("node"));
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        double overlayHeight = callOnFx(overlay::getHeight);
        double treeHeight = callOnFx(treeView::getHeight);

        assertEquals(Region.USE_PREF_SIZE, callOnFx(overlay::getMaxHeight));
        assertTrue(overlayHeight < treeHeight * 0.25);
        assertTrue(callOnFx(() -> overlay.getStyleClass().contains("pf-tree-search-overlay")));
        assertTrue(callOnFx(() -> overlay.lookupAll(".pf-tree-search-field").size() == 1));
    }

    @Test
    void treeViewportAndSearchOverlayUseSharedMetrics() {
        flushLayout();
        assertEquals(UiMetrics.SPACE_6, callOnFx(() -> treeView.getViewport().rowHeight()), 0.01);
        assertEquals(UiMetrics.SPACE_5, callOnFx(() -> treeView.getViewport().getTheme().indentWidth()), 0.01);
        assertEquals(UiMetrics.SPACE_4, callOnFx(() -> treeView.getViewport().getTheme().iconSize()), 0.01);

        runOnFx(() -> treeView.openSearch("node"));
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        TextField searchField = callOnFx(() -> (TextField) overlay.lookup(".pf-tree-search-field"));
        ButtonWrapper closeButton = callOnFx(() -> new ButtonWrapper((Region) overlay.lookupAll(".pf-tree-search-icon-button").stream()
            .map(Region.class::cast)
            .reduce((first, second) -> second)
            .orElseThrow()));

        assertEquals(UiMetrics.SPACE_2, callOnFx(overlay::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_3, callOnFx(() -> overlay.getPadding().getTop()), 0.01);
        assertEquals(UiMetrics.SPACE_3, callOnFx(() -> overlay.getPadding().getRight()), 0.01);
        assertEquals(UiMetrics.CONTROL_HEIGHT_COMPACT, callOnFx(searchField::getHeight), 1.0);
        assertEquals(UiMetrics.ICON_BUTTON_SIZE_COMPACT, callOnFx(closeButton.region()::getWidth), 1.0);
    }

    @Test
    void searchOverlayAdaptsToNarrowTreeWidth() {
        runOnFx(() -> {
            treeView.getScene().getWindow().setWidth(210.0);
            treeView.openSearch("sample");
        });
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        TextField searchField = callOnFx(() -> (TextField) overlay.lookup(".pf-tree-search-field"));
        Label countLabel = callOnFx(() -> (Label) overlay.lookup(".pf-tree-search-result-label"));

        assertNotNull(searchField);
        assertNotNull(countLabel);
        assertTrue(callOnFx(overlay::getMaxWidth) <= callOnFx(treeView::getWidth));
        assertTrue(callOnFx(searchField::getWidth) >= 40.0);
        assertFalse(callOnFx(countLabel::isVisible));
    }

    @Test
    void typingPrintableCharOpensSearchOverlayWithSeedQuery() {
        runOnFx(() -> {
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "n", "n", KeyCode.UNDEFINED, false, false, false, false));
        });

        assertTrue(callOnFx(treeView::isSearchOpen));
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertNotNull(focused);
        assertEquals("node-0", focused.getValue());
    }

    @Test
    void searchSelectsAndExpandsAndRevealsMatch() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            for (int i = 0; i < 220; i++) {
                root.addChild(new TreeItem<>("noise-" + i));
            }
            TreeItem<String> branch = new TreeItem<>("branch");
            TreeItem<String> target = new TreeItem<>("target-node");
            branch.addChild(target);
            root.addChild(branch);
            treeView.setRoot(root);
            treeView.getExpansionModel().setExpanded(branch, false);
            treeView.getViewport().setScrollOffset(0.0);
        });
        flushLayout();

        TreeItem<String> match = callOnFx(() -> treeView.searchAndRevealFirst("target"));
        assertNotNull(match);
        assertEquals("target-node", match.getValue());
        TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
        assertSame(match, focused);
        assertTrue(callOnFx(() -> treeView.getExpansionModel().isExpanded(match.getParent())));
        assertTrue(callOnFx(() -> treeView.getViewport().getScrollOffset()) > 0.0);
    }

    @Test
    void searchNextAndPreviousCycleThroughMatches() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            root.addChild(new TreeItem<>("match-1"));
            root.addChild(new TreeItem<>("match-2"));
            root.addChild(new TreeItem<>("match-3"));
            treeView.setRoot(root);
            treeView.openSearch("match");
        });

        assertEquals("match-1", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-2", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-3", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchNext);
        assertEquals("match-1", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));

        runOnFx(treeView::searchPrevious);
        assertEquals("match-3", callOnFx(() -> treeView.getSelectionModel().getFocusedItem().getValue()));
    }

    @Test
    void escapeClosesSearchOverlayAndReturnsFocusToTree() {
        runOnFx(() -> {
            treeView.openSearch("node");
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
        });

        assertFalse(callOnFx(treeView::isSearchOpen));
        assertTrue(callOnFx(() -> treeView.getScene().getFocusOwner() == treeView));
    }

    @Test
    void pointerToggleExpandsAndCollapsesInlineInfo() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
        });
        flushLayout();

        Bounds firstRowBounds = callOnFx(() -> treeView.getViewport().rowBounds(0));
        double toggleX = callOnFx(() -> treeView.getViewport().getEffectiveTextWidth() - TreeViewport.INFO_TOGGLE_MARGIN - (TreeViewport.INFO_TOGGLE_SIZE * 0.5));
        double toggleY = firstRowBounds.getMinY() + (firstRowBounds.getHeight() * 0.5);

        runOnFx(() -> treeView.getViewport().fireEvent(mousePressed(toggleX, toggleY)));
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
        assertEquals(2, callOnFx(() -> treeView.getFlattenedTree().size()));
        assertTrue(callOnFx(() -> treeView.getFlattenedTree().isInfoRow(1)));

        runOnFx(() -> treeView.getViewport().fireEvent(mousePressed(toggleX, toggleY)));
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
        assertEquals(1, callOnFx(() -> treeView.getFlattenedTree().size()));
    }

    @Test
    void disabledModePreventsKeyboardAndMouseToggle() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.DISABLED);
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(toggleInfoShortcutEvent());
        });
        flushLayout();

        clickInfoToggleAtRow(0);
        flushLayout();

        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void keyboardOnlyModeAllowsKeyboardButBlocksMouseToggle() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.KEYBOARD_ONLY);
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(toggleInfoShortcutEvent());
        });
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        clickInfoToggleAtRow(0);
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        runOnFx(() -> treeView.fireEvent(toggleInfoShortcutEvent()));
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void mouseOnlyModeAllowsMouseButBlocksKeyboardToggle() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.MOUSE_ONLY);
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(toggleInfoShortcutEvent());
        });
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        clickInfoToggleAtRow(0);
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        runOnFx(() -> treeView.fireEvent(toggleInfoShortcutEvent()));
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        clickInfoToggleAtRow(0);
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void bothModeAllowsKeyboardAndMouseToggle() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE);
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(toggleInfoShortcutEvent());
        });
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        clickInfoToggleAtRow(0);
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void mouseToggleFocusPolicyKeepsCurrentFocus() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            TreeItem<String> second = new TreeItem<>("second");
            root.addChild(first);
            root.addChild(second);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.MOUSE_ONLY);
            treeView.setNodeInfoFocusPolicy(TreeNodeInfoFocusPolicy.KEEP_CURRENT_FOCUS);
            treeView.getSelectionModel().selectOnly(first);
            treeView.getSelectionModel().setFocusedItem(first);
            treeView.getSelectionModel().setAnchorItem(first);
        });
        flushLayout();

        clickInfoToggleAtRow(1);
        flushLayout();

        TreeItem<String> first = callOnFx(() -> treeView.getRoot().getChildren().getFirst());
        TreeItem<String> second = callOnFx(() -> treeView.getRoot().getChildren().get(1));
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(second)));
        assertSame(first, callOnFx(() -> treeView.getSelectionModel().getFocusedItem()));
        assertTrue(callOnFx(() -> treeView.getSelectionModel().isSelected(first)));
        assertFalse(callOnFx(() -> treeView.getSelectionModel().isSelected(second)));
    }

    @Test
    void mouseToggleFocusPolicyFocusesToggledItem() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            TreeItem<String> second = new TreeItem<>("second");
            root.addChild(first);
            root.addChild(second);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.MOUSE_ONLY);
            treeView.setNodeInfoFocusPolicy(TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM);
            treeView.getSelectionModel().selectOnly(first);
            treeView.getSelectionModel().setFocusedItem(first);
            treeView.getSelectionModel().setAnchorItem(first);
        });
        flushLayout();

        clickInfoToggleAtRow(1);
        flushLayout();

        TreeItem<String> first = callOnFx(() -> treeView.getRoot().getChildren().getFirst());
        TreeItem<String> second = callOnFx(() -> treeView.getRoot().getChildren().get(1));
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(second)));
        assertSame(second, callOnFx(() -> treeView.getSelectionModel().getFocusedItem()));
        assertSame(second, callOnFx(() -> treeView.getSelectionModel().getAnchorItem()));
        assertTrue(callOnFx(() -> treeView.getSelectionModel().isSelected(second)));
        assertFalse(callOnFx(() -> treeView.getSelectionModel().isSelected(first)));
    }

    @Test
    void mouseDisabledHidesInfoToggleAffordanceAndHitZone() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.setNodeInfoToggleMode(TreeNodeInfoToggleMode.KEYBOARD_ONLY);
        });
        flushLayout();

        double toggleX = infoToggleCenterX();
        double toggleY = rowCenterY(0);
        TreeViewport.HitInfo<String> hitInfo = callOnFx(() -> treeView.getViewport().hitTest(toggleX, toggleY));
        assertNotNull(hitInfo);
        assertFalse(hitInfo.infoToggleHit());
        assertFalse(callOnFx(() -> treeView.getViewport().getVisibleRows().getFirst().infoAvailable()));

        runOnFx(() -> treeView.getViewport().fireEvent(mousePressed(toggleX, toggleY)));
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void singleInfoModeKeepsOnlyLastToggledInlineInfoExpanded() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            TreeItem<String> second = new TreeItem<>("second");
            root.addChild(first);
            root.addChild(second);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.setNodeInfoMode(TreeNodeInfoMode.SINGLE);
            treeView.toggleNodeInfo(first);
            treeView.toggleNodeInfo(second);
        });
        flushLayout();

        TreeItem<String> first = callOnFx(() -> treeView.getRoot().getChildren().getFirst());
        TreeItem<String> second = callOnFx(() -> treeView.getRoot().getChildren().get(1));
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(first)));
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(second)));
        assertEquals(3, callOnFx(() -> treeView.getFlattenedTree().size()));
        assertTrue(callOnFx(() -> treeView.getFlattenedTree().isInfoRow(2)));
    }

    @Test
    void multipleInfoModeKeepsMultipleInlineInfoRowsExpanded() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            TreeItem<String> second = new TreeItem<>("second");
            root.addChild(first);
            root.addChild(second);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.setNodeInfoMode(TreeNodeInfoMode.MULTIPLE);
            treeView.toggleNodeInfo(first);
            treeView.toggleNodeInfo(second);
        });
        flushLayout();

        TreeItem<String> first = callOnFx(() -> treeView.getRoot().getChildren().getFirst());
        TreeItem<String> second = callOnFx(() -> treeView.getRoot().getChildren().get(1));
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(first)));
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(second)));
        assertEquals(4, callOnFx(() -> treeView.getFlattenedTree().size()));
        assertTrue(callOnFx(() -> treeView.getFlattenedTree().isInfoRow(1)));
        assertTrue(callOnFx(() -> treeView.getFlattenedTree().isInfoRow(3)));
    }

    @Test
    void expandedInlineInfoMountsVisibleContent() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.toggleNodeInfo(first);
        });
        flushLayout();

        assertTrue(callOnFx(() -> {
            if (treeView.getChildren().size() < 2 || !(treeView.getChildren().get(1) instanceof Pane inlineLayer)) {
                return false;
            }
            if (inlineLayer.getChildren().isEmpty() || !(inlineLayer.getChildren().getFirst() instanceof VBox box)) {
                return false;
            }
            if (box.getChildren().isEmpty() || !(box.getChildren().getFirst() instanceof Label label)) {
                return false;
            }
            return label.getBoundsInParent().getWidth() > 0.0 && label.getBoundsInParent().getHeight() > 0.0;
        }));
    }

    @Test
    void inlineInfoContentUsesTreeIndentation() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.toggleNodeInfo(first);
        });
        flushLayout();

        assertTrue(callOnFx(() -> {
            if (treeView.getChildren().size() < 2 || !(treeView.getChildren().get(1) instanceof Pane inlineLayer)) {
                return false;
            }
            if (inlineLayer.getChildren().isEmpty()) {
                return false;
            }
            Node infoNode = inlineLayer.getChildren().getFirst();
            double nodeX = infoNode.getLayoutX();
            double nodeWidth = infoNode.getBoundsInParent().getWidth();
            double viewportWidth = treeView.getViewport().getEffectiveTextWidth();
            return nodeX > 0.0 && nodeWidth > 0.0 && nodeWidth < viewportWidth;
        }));
    }

    @Test
    void lastChildConnectorStopsAtBranchCenter() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> branch = new TreeItem<>("branch");
            TreeItem<String> first = new TreeItem<>("first");
            TreeItem<String> last = new TreeItem<>("last");
            branch.addChild(first);
            branch.addChild(last);
            root.addChild(branch);
            treeView.setShowRoot(true);
            treeView.setRoot(root);
            treeView.getExpansionModel().setExpanded(root, true);
            treeView.getExpansionModel().setExpanded(branch, true);
        });
        flushLayout();

        WritableImage snapshot = callOnFx(() -> treeView.getViewport().snapshot(null, null));
        Bounds firstRowBounds = callOnFx(() -> treeView.getViewport().rowBounds(2));
        Bounds lastRowBounds = callOnFx(() -> treeView.getViewport().rowBounds(3));
        double viewportWidth = callOnFx(() -> treeView.getViewport().getWidth());
        double viewportHeight = callOnFx(() -> treeView.getViewport().getHeight());
        double xScale = snapshot.getWidth() / Math.max(1.0, viewportWidth);
        double yScale = snapshot.getHeight() / Math.max(1.0, viewportHeight);
        double indentWidth = callOnFx(() -> treeView.getViewport().getTheme().indentWidth());
        double connectorX = ((2.0 * indentWidth) - (indentWidth * 0.5)) * xScale;

        Color nonLastBottomColor = colorAt(
            snapshot,
            connectorX,
            (firstRowBounds.getMinY() + firstRowBounds.getHeight() - 2.0) * yScale
        );
        Color lastBottomColor = colorAt(
            snapshot,
            connectorX,
            (lastRowBounds.getMinY() + lastRowBounds.getHeight() - 2.0) * yScale
        );
        Color connectingLineColor = callOnFx(() -> (Color) treeView.getTreeViewTheme().connectingLineColor());
        Color lastRowBackground = callOnFx(() -> (Color) treeView.getTreeViewTheme().rowBackgroundAlternate());

        assertTrue(isColorClose(nonLastBottomColor, connectingLineColor, 0.08));
        assertTrue(isColorClose(lastBottomColor, lastRowBackground, 0.08));
    }

    @Test
    void selectedItemUsesBorderHighlightForInlineInfoRow() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info-" + item.getValue())));
            treeView.toggleNodeInfo(first);
            treeView.getSelectionModel().selectOnly(first);
            treeView.getSelectionModel().setFocusedItem(first);
            treeView.getViewport().setHoveredItem(null);
        });
        flushLayout();

        WritableImage snapshot = callOnFx(() -> treeView.getViewport().snapshot(null, null));
        Bounds itemRowBounds = callOnFx(() -> treeView.getViewport().rowBounds(0));
        Bounds infoRowBounds = callOnFx(() -> treeView.getViewport().rowBounds(1));
        double viewportWidth = callOnFx(() -> treeView.getViewport().getWidth());
        double viewportHeight = callOnFx(() -> treeView.getViewport().getHeight());
        double xScale = snapshot.getWidth() / Math.max(1.0, viewportWidth);
        double yScale = snapshot.getHeight() / Math.max(1.0, viewportHeight);
        Color itemRowColor = colorAt(snapshot, 2.0 * xScale, (itemRowBounds.getMinY() + (itemRowBounds.getHeight() * 0.5)) * yScale);
        Color infoRowColor = colorAt(snapshot, 2.0 * xScale, (infoRowBounds.getMinY() + (infoRowBounds.getHeight() * 0.5)) * yScale);
        Color infoRowEdgeColor = colorAt(snapshot, 0.0, (infoRowBounds.getMinY() + (infoRowBounds.getHeight() * 0.5)) * yScale);
        Color selectedBackground = callOnFx(() -> (Color) treeView.getTreeViewTheme().selectedBackground());
        Color alternateBackground = callOnFx(() -> (Color) treeView.getTreeViewTheme().rowBackgroundAlternate());

        assertTrue(isColorClose(itemRowColor, selectedBackground, 0.04));
        assertTrue(isColorClose(infoRowColor, alternateBackground, 0.04));
        assertFalse(isColorClose(infoRowEdgeColor, alternateBackground, 0.02));
    }

    @Test
    void hoveredItemUsesHoverBackgroundColor() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.getSelectionModel().clearSelection();
            treeView.getSelectionModel().setFocusedItem(null);
            treeView.getViewport().setHoveredItem(first);
        });
        flushLayout();

        WritableImage snapshot = callOnFx(() -> treeView.getViewport().snapshot(null, null));
        Bounds rowBounds = callOnFx(() -> treeView.getViewport().rowBounds(0));
        double viewportWidth = callOnFx(() -> treeView.getViewport().getWidth());
        double viewportHeight = callOnFx(() -> treeView.getViewport().getHeight());
        double xScale = snapshot.getWidth() / Math.max(1.0, viewportWidth);
        double yScale = snapshot.getHeight() / Math.max(1.0, viewportHeight);
        Color rowColor = colorAt(snapshot, 2.0 * xScale, (rowBounds.getMinY() + (rowBounds.getHeight() * 0.5)) * yScale);
        Color hoverBackground = callOnFx(() -> (Color) treeView.getTreeViewTheme().hoverBackground());

        assertTrue(isColorClose(rowColor, hoverBackground, 0.04));
    }

    @Test
    void boundThemePropertyUpdatesTreeAndSearchOverlayColors() {
        ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.dark());
        runOnFx(() -> {
            treeView.bindThemeProperty(theme);
            treeView.openSearch("node");
        });
        flushLayout();

        TreeSearchOverlay overlay = callOnFx(() -> treeView.getChildren().stream()
            .filter(TreeSearchOverlay.class::isInstance)
            .map(TreeSearchOverlay.class::cast)
            .findFirst()
            .orElseThrow());
        String darkStyle = callOnFx(overlay::getStyle);
        Color darkText = callOnFx(() -> (Color) treeView.getTreeViewTheme().textColor());

        runOnFx(() -> theme.set(Theme.light()));
        flushLayout();

        String lightStyle = callOnFx(overlay::getStyle);
        Color lightText = callOnFx(() -> (Color) treeView.getTreeViewTheme().textColor());

        assertFalse(darkStyle.equals(lightStyle));
        assertTrue(darkStyle.contains("-pf-ui-surface-overlay"));
        assertTrue(lightStyle.contains("-pf-ui-surface-overlay"));
        assertTrue(darkText.getBrightness() > lightText.getBrightness());
    }

    @Test
    void platformShortcutTogglesFocusedNodeInfo() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(toggleInfoShortcutEvent());
        });
        flushLayout();
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));

        runOnFx(() -> treeView.fireEvent(toggleInfoShortcutEvent()));
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void nonPlatformShortcutDoesNotToggleFocusedNodeInfo() {
        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.requestFocus();
            treeView.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.HOME, false, false, false, false));
            treeView.fireEvent(nonToggleInfoShortcutEvent());
        });
        flushLayout();
        assertFalse(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(treeView.getRoot().getChildren().getFirst())));
    }

    @Test
    void stateSaveRestoreKeepsExpandedInfoRows() {
        Map<String, Object> state = callOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.toggleNodeInfo(first);
            return treeView.captureState();
        });

        runOnFx(() -> {
            TreeItem<String> root = new TreeItem<>("root");
            TreeItem<String> first = new TreeItem<>("first");
            root.addChild(first);
            treeView.setShowRoot(false);
            treeView.setRoot(root);
            treeView.setNodeInfoProvider(item -> new VBox(new Label("info")));
            treeView.applyState(state);
        });
        flushLayout();

        TreeItem<String> first = callOnFx(() -> treeView.getRoot().getChildren().getFirst());
        assertTrue(callOnFx(() -> treeView.getNodeInfoModel().isExpanded(first)));
        assertEquals(2, callOnFx(() -> treeView.getFlattenedTree().size()));
    }

    private TreeItem<String> createLargeTree() {
        TreeItem<String> root = new TreeItem<>("root");
        for (int i = 0; i < 500; i++) {
            root.addChild(new TreeItem<>("node-" + i));
        }
        root.setExpanded(true);
        return root;
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

    private Color colorAt(WritableImage image, double x, double y) {
        int maxX = Math.max(0, (int) image.getWidth() - 1);
        int maxY = Math.max(0, (int) image.getHeight() - 1);
        int sampleX = Math.max(0, Math.min(maxX, (int) Math.round(x)));
        int sampleY = Math.max(0, Math.min(maxY, (int) Math.round(y)));
        return image.getPixelReader().getColor(sampleX, sampleY);
    }

    private boolean isColorClose(Color actual, Color expected, double tolerance) {
        return Math.abs(actual.getRed() - expected.getRed()) <= tolerance
            && Math.abs(actual.getGreen() - expected.getGreen()) <= tolerance
            && Math.abs(actual.getBlue() - expected.getBlue()) <= tolerance
            && Math.abs(actual.getOpacity() - expected.getOpacity()) <= tolerance;
    }

    private record ButtonWrapper(Region region) {
    }

    private void clickInfoToggleAtRow(int rowIndex) {
        double toggleX = infoToggleCenterX();
        double toggleY = rowCenterY(rowIndex);
        runOnFx(() -> treeView.getViewport().fireEvent(mousePressed(toggleX, toggleY)));
    }

    private double infoToggleCenterX() {
        return callOnFx(() ->
            treeView.getViewport().getEffectiveTextWidth() - TreeViewport.INFO_TOGGLE_MARGIN - (TreeViewport.INFO_TOGGLE_SIZE * 0.5)
        );
    }

    private double rowCenterY(int rowIndex) {
        Bounds rowBounds = callOnFx(() -> treeView.getViewport().rowBounds(rowIndex));
        return rowBounds.getMinY() + (rowBounds.getHeight() * 0.5);
    }

    private MouseEvent mousePressed(double x, double y) {
        return new MouseEvent(
            MouseEvent.MOUSE_PRESSED,
            x,
            y,
            x,
            y,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            true,
            false,
            false,
            null
        );
    }

    private KeyEvent toggleInfoShortcutEvent() {
        if (isMacPlatform()) {
            return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.I, false, false, false, true);
        }
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, true, false);
    }

    private KeyEvent nonToggleInfoShortcutEvent() {
        if (isMacPlatform()) {
            return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, true, false);
        }
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.I, false, false, false, true);
    }

    private boolean isMacPlatform() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
