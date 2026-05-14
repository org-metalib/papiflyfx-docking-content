package org.metalib.papifly.fx.tree.api;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.tree.controller.TreeDragDropController;
import org.metalib.papifly.fx.tree.controller.TreeEditController;
import org.metalib.papifly.fx.tree.controller.TreeInputController;
import org.metalib.papifly.fx.tree.controller.TreePointerController;
import org.metalib.papifly.fx.tree.model.FlattenedTree;
import org.metalib.papifly.fx.tree.model.TreeExpansionModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoFocusPolicy;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoMode;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoModel;
import org.metalib.papifly.fx.tree.model.TreeNodeInfoToggleMode;
import org.metalib.papifly.fx.tree.model.TreeSelectionModel;
import org.metalib.papifly.fx.tree.render.TreeInlineInfoHost;
import org.metalib.papifly.fx.tree.render.TreeViewport;
import org.metalib.papifly.fx.tree.search.TreeSearchEngine;
import org.metalib.papifly.fx.tree.search.TreeSearchOverlay;
import org.metalib.papifly.fx.tree.search.TreeSearchSession;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.tree.theme.TreeViewThemeMapper;
import org.metalib.papifly.fx.tree.util.TreeStateCodec;
import org.metalib.papifly.fx.tree.util.TreeViewStateData;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeView<T> extends StackPane implements DisposableContent {

    private final ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<>(this, "root");
    private final ObjectProperty<Function<T, String>> searchTextExtractor = new SimpleObjectProperty<>(
        this,
        "searchTextExtractor",
        value -> value == null ? "" : String.valueOf(value)
    );
    private final ObjectProperty<TreeNodeInfoProvider<T>> nodeInfoProvider = new SimpleObjectProperty<>(this, "nodeInfoProvider");
    private final ObjectProperty<TreeNodeInfoToggleMode> nodeInfoToggleMode = new SimpleObjectProperty<>(
        this,
        "nodeInfoToggleMode",
        TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE
    );
    private final ObjectProperty<TreeNodeInfoFocusPolicy> nodeInfoFocusPolicy = new SimpleObjectProperty<>(
        this,
        "nodeInfoFocusPolicy",
        TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM
    );
    private final TreeSelectionModel<T> selectionModel = new TreeSelectionModel<>();
    private final TreeExpansionModel<T> expansionModel = new TreeExpansionModel<>();
    private final TreeNodeInfoModel<T> nodeInfoModel = new TreeNodeInfoModel<>();
    private final FlattenedTree<T> flattenedTree = new FlattenedTree<>(expansionModel, nodeInfoModel);
    private final TreeViewport<T> viewport = new TreeViewport<>(flattenedTree, selectionModel);
    private final TreeInlineInfoHost<T> inlineInfoHost = new TreeInlineInfoHost<>();
    private final Rectangle inlineInfoClip = new Rectangle();
    private final TreeEditController<T> editController = new TreeEditController<>(viewport, viewport::markDirty);
    private final TreeSearchEngine<T> searchEngine = new TreeSearchEngine<>();
    private final TreeSearchSession<T> searchSession = new TreeSearchSession<>(
        searchEngine,
        this::getRoot,
        this::getSearchTextExtractor,
        this::isShowRoot
    );
    private final TreeSearchOverlay searchOverlay = new TreeSearchOverlay();

    private final TreeInputController<T> inputController = new TreeInputController<>(
        flattenedTree,
        selectionModel,
        expansionModel,
        nodeInfoModel,
        viewport,
        editController
    );
    private final TreePointerController<T> pointerController = new TreePointerController<>(
        flattenedTree,
        selectionModel,
        expansionModel,
        nodeInfoModel,
        viewport,
        editController
    );
    private final TreeDragDropController<T> dragDropController = new TreeDragDropController<>(
        this,
        viewport,
        flattenedTree,
        selectionModel,
        expansionModel
    );

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;
    private BiConsumer<TreeItem<T>, String> editCommitHandler = (item, text) -> {};
    private Predicate<TreeItem<T>> navigationSelectablePredicate = item -> true;
    private TreeViewStateData pendingState;
    private boolean disposed;

    public TreeView() {
        setMinSize(0, 0);
        setPrefSize(480, 360);
        setFocusTraversable(true);

        editController.setCommitHandler(this::onEditCommit);
        inlineInfoHost.layer().setClip(inlineInfoClip);
        StackPane.setAlignment(searchOverlay, Pos.TOP_RIGHT);
        StackPane.setMargin(searchOverlay, new Insets(UiMetrics.SPACE_2));
        searchOverlay.maxWidthProperty().bind(Bindings.max(0.0, widthProperty().subtract(UiMetrics.SPACE_4)));
        getChildren().addAll(viewport, inlineInfoHost.layer(), dragDropController.overlayCanvas(), editController.editorNode(), searchOverlay);

        configureSearchOverlay();
        installHandlers();
        inputController.setNavigationSelectablePredicate(navigationSelectablePredicate);
        inputController.setNodeInfoToggleModeSupplier(this::getNodeInfoToggleMode);
        pointerController.setNodeInfoToggleModeSupplier(this::getNodeInfoToggleMode);
        pointerController.setNodeInfoFocusPolicySupplier(this::getNodeInfoFocusPolicy);
        applyNodeInfoToggleModeToViewport(getNodeInfoToggleMode());
        root.addListener((obs, oldRoot, newRoot) -> onRootChanged(newRoot));
        selectionModel.addListener(model -> viewport.markDirty());
        expansionModel.addListener((item, expanded) -> viewport.markDirty());
        nodeInfoModel.addListener((item, expanded) -> viewport.markDirty());
        nodeInfoProvider.addListener((obs, oldProvider, newProvider) -> onNodeInfoProviderChanged(oldProvider, newProvider));
        nodeInfoToggleMode.addListener((obs, oldMode, newMode) -> {
            applyNodeInfoToggleModeToViewport(newMode);
            viewport.markDirty();
        });
        setTreeViewTheme(TreeViewTheme.dark());
    }

    public TreeItem<T> getRoot() {
        return root.get();
    }

    public void setRoot(TreeItem<T> root) {
        this.root.set(root);
    }

    public ObjectProperty<TreeItem<T>> rootProperty() {
        return root;
    }

    public TreeSelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    public TreeExpansionModel<T> getExpansionModel() {
        return expansionModel;
    }

    public TreeNodeInfoModel<T> getNodeInfoModel() {
        return nodeInfoModel;
    }

    public TreeNodeInfoProvider<T> getNodeInfoProvider() {
        return nodeInfoProvider.get();
    }

    public void setNodeInfoProvider(TreeNodeInfoProvider<T> nodeInfoProvider) {
        this.nodeInfoProvider.set(nodeInfoProvider);
    }

    public ObjectProperty<TreeNodeInfoProvider<T>> nodeInfoProviderProperty() {
        return nodeInfoProvider;
    }

    public TreeNodeInfoToggleMode getNodeInfoToggleMode() {
        TreeNodeInfoToggleMode mode = nodeInfoToggleMode.get();
        return mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode;
    }

    public void setNodeInfoToggleMode(TreeNodeInfoToggleMode mode) {
        nodeInfoToggleMode.set(mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode);
    }

    public ObjectProperty<TreeNodeInfoToggleMode> nodeInfoToggleModeProperty() {
        return nodeInfoToggleMode;
    }

    public TreeNodeInfoFocusPolicy getNodeInfoFocusPolicy() {
        TreeNodeInfoFocusPolicy policy = nodeInfoFocusPolicy.get();
        return policy == null ? TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM : policy;
    }

    public void setNodeInfoFocusPolicy(TreeNodeInfoFocusPolicy policy) {
        nodeInfoFocusPolicy.set(policy == null ? TreeNodeInfoFocusPolicy.FOCUS_TOGGLED_ITEM : policy);
    }

    public ObjectProperty<TreeNodeInfoFocusPolicy> nodeInfoFocusPolicyProperty() {
        return nodeInfoFocusPolicy;
    }

    public void toggleNodeInfo(TreeItem<T> item) {
        nodeInfoModel.toggle(item);
    }

    public void collapseNodeInfo(TreeItem<T> item) {
        nodeInfoModel.setExpanded(item, false);
    }

    public void collapseAllNodeInfo() {
        nodeInfoModel.clear();
    }

    public void setNodeInfoMode(TreeNodeInfoMode mode) {
        nodeInfoModel.setMode(mode);
    }

    public TreeNodeInfoMode getNodeInfoMode() {
        return nodeInfoModel.getMode();
    }

    public FlattenedTree<T> getFlattenedTree() {
        return flattenedTree;
    }

    public boolean isShowRoot() {
        return flattenedTree.isShowRoot();
    }

    public void setShowRoot(boolean showRoot) {
        flattenedTree.setShowRoot(showRoot);
        viewport.markDirty();
    }

    public TreeViewport<T> getViewport() {
        return viewport;
    }

    public TreeEditController<T> getEditController() {
        return editController;
    }

    public TreeDragDropController<T> getDragDropController() {
        return dragDropController;
    }

    public void setCellRenderer(TreeCellRenderer<T> cellRenderer) {
        viewport.setCellRenderer(cellRenderer);
    }

    public void setIconResolver(Function<T, Image> iconResolver) {
        viewport.setIconResolver(iconResolver);
    }

    public void setEditCommitHandler(BiConsumer<TreeItem<T>, String> editCommitHandler) {
        this.editCommitHandler = editCommitHandler == null ? (item, text) -> {} : editCommitHandler;
    }

    public void setNavigationSelectablePredicate(Predicate<TreeItem<T>> navigationSelectablePredicate) {
        this.navigationSelectablePredicate = navigationSelectablePredicate == null ? item -> true : navigationSelectablePredicate;
        inputController.setNavigationSelectablePredicate(this.navigationSelectablePredicate);
    }

    public Predicate<TreeItem<T>> getNavigationSelectablePredicate() {
        return navigationSelectablePredicate;
    }

    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        if (themeProperty == null) {
            return;
        }
        this.boundThemeProperty = themeProperty;
        this.themeChangeListener = (obs, oldTheme, newTheme) -> applyDockingTheme(newTheme);
        themeProperty.addListener(themeChangeListener);
        applyDockingTheme(themeProperty.get());
    }

    public void unbindThemeProperty() {
        if (boundThemeProperty != null && themeChangeListener != null) {
            boundThemeProperty.removeListener(themeChangeListener);
        }
        boundThemeProperty = null;
        themeChangeListener = null;
    }

    public void setTreeViewTheme(TreeViewTheme theme) {
        TreeViewTheme resolvedTheme = theme == null ? TreeViewTheme.dark() : theme;
        viewport.setTheme(resolvedTheme);
        searchOverlay.setTheme(resolvedTheme);
    }

    public TreeViewTheme getTreeViewTheme() {
        return viewport.getTheme();
    }

    public Function<T, String> getSearchTextExtractor() {
        return searchTextExtractor.get();
    }

    public void setSearchTextExtractor(Function<T, String> searchTextExtractor) {
        this.searchTextExtractor.set(searchTextExtractor == null ? value -> value == null ? "" : String.valueOf(value) : searchTextExtractor);
        if (searchOverlay.isOpen()) {
            searchSession.refresh();
            updateSearchOverlayCount();
            revealMatch(searchSession.getCurrentMatch());
        }
    }

    public ObjectProperty<Function<T, String>> searchTextExtractorProperty() {
        return searchTextExtractor;
    }

    public Map<String, Object> captureState() {
        return TreeStateCodec.toMap(captureStateData());
    }

    public TreeViewStateData captureStateData() {
        List<List<Integer>> expandedPaths = expansionModel.getExpandedItems().stream()
            .map(this::pathOf)
            .filter(path -> !path.isEmpty() || getRoot() != null)
            .sorted(Comparator.comparingInt(List::size))
            .toList();
        List<List<Integer>> expandedInfoPaths = nodeInfoModel.getExpandedItems().stream()
            .map(this::pathOf)
            .filter(path -> !path.isEmpty() || getRoot() != null)
            .toList();
        List<List<Integer>> selectedPaths = selectionModel.getSelectedItems().stream()
            .map(this::pathOf)
            .toList();
        List<Integer> focusedPath = pathOf(selectionModel.getFocusedItem());
        return new TreeViewStateData(
            expandedPaths,
            expandedInfoPaths,
            selectedPaths,
            focusedPath,
            viewport.getScrollOffset(),
            viewport.getHorizontalScrollOffset()
        );
    }

    public void applyState(Map<String, Object> state) {
        applyState(TreeStateCodec.fromMap(state));
    }

    public void applyState(TreeViewStateData state) {
        TreeViewStateData safeState = state == null ? TreeViewStateData.empty() : state;
        if (getRoot() == null) {
            pendingState = safeState;
            return;
        }
        pendingState = null;
        expansionModel.clear();
        nodeInfoModel.clear();
        for (List<Integer> path : safeState.expandedPaths()) {
            TreeItem<T> item = resolvePath(path);
            if (item != null) {
                expansionModel.setExpanded(item, true);
            }
        }
        if (safeState.expandedPaths().isEmpty() && getRoot() != null) {
            expansionModel.setExpanded(getRoot(), true);
        }
        for (List<Integer> path : safeState.expandedInfoPaths()) {
            TreeItem<T> item = resolvePath(path);
            if (item != null) {
                nodeInfoModel.setExpanded(item, true);
            }
        }

        selectionModel.clearSelection();
        for (List<Integer> path : safeState.selectedPaths()) {
            TreeItem<T> item = resolvePath(path);
            if (item != null) {
                selectionModel.addSelection(item);
            }
        }

        TreeItem<T> focused = resolvePath(safeState.focusedPath());
        if (focused != null) {
            selectionModel.setFocusedItem(focused);
            if (!selectionModel.isSelected(focused)) {
                selectionModel.addSelection(focused);
            }
        }

        viewport.setScrollOffset(safeState.scrollOffset());
        viewport.setHorizontalScrollOffset(safeState.horizontalScrollOffset());
        viewport.markDirty();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        viewport.layout();
        editController.relayout();
        syncInlineInfoHost();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        inlineInfoHost.clear(getNodeInfoProvider());
        unbindThemeProperty();
        closeSearch();
        editController.dispose();
        pointerController.dispose();
        dragDropController.dispose();
        setOnKeyPressed(null);
        setOnKeyTyped(null);
        viewport.setOnMousePressed(null);
        viewport.setOnMouseDragged(null);
        viewport.setOnMouseReleased(null);
        viewport.setOnMouseMoved(null);
        viewport.setOnMouseExited(null);
        viewport.setOnScroll(null);
        viewport.setOnDragDetected(null);
        setOnDragOver(null);
        setOnDragDropped(null);
        setOnDragExited(null);
        setOnDragDone(null);
    }

    public TreeItem<T> resolvePath(List<Integer> path) {
        TreeItem<T> current = getRoot();
        if (current == null) {
            return null;
        }
        if (path == null || path.isEmpty()) {
            return current;
        }
        for (Integer segment : path) {
            if (segment == null) {
                return null;
            }
            int index = segment;
            if (index < 0 || index >= current.getChildCount()) {
                return null;
            }
            current = current.getChildren().get(index);
        }
        return current;
    }

    public List<Integer> pathOf(TreeItem<T> item) {
        if (item == null || getRoot() == null) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        TreeItem<T> current = item;
        while (current != null && current.getParent() != null) {
            TreeItem<T> parent = current.getParent();
            int index = parent.indexOfChild(current);
            if (index < 0) {
                return List.of();
            }
            path.add(index);
            current = parent;
        }
        if (current != getRoot()) {
            return List.of();
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private void installHandlers() {
        setOnKeyPressed(event -> {
            if (handleSearchKeyPressed(event)) {
                return;
            }
            if (searchOverlay.isFocusWithin()) {
                return;
            }
            inputController.handleKeyPressed(event);
        });
        setOnKeyTyped(this::handleSearchKeyTyped);
        viewport.setOnMousePressed(event -> {
            requestFocus();
            pointerController.handleMousePressed(event);
        });
        viewport.setOnMouseDragged(pointerController::handleMouseDragged);
        viewport.setOnMouseReleased(pointerController::handleMouseReleased);
        viewport.setOnMouseMoved(pointerController::handleMouseMoved);
        viewport.setOnMouseExited(pointerController::handleMouseExited);
        viewport.setOnScroll(pointerController::handleScroll);
    }

    private void onRootChanged(TreeItem<T> newRoot) {
        inlineInfoHost.clear(getNodeInfoProvider());
        nodeInfoModel.clear();
        flattenedTree.setRoot(newRoot);
        if (newRoot != null && !expansionModel.isExpanded(newRoot)) {
            expansionModel.setExpanded(newRoot, true);
        }
        if (pendingState != null) {
            applyState(pendingState);
        } else {
            viewport.markDirty();
        }
        if (searchOverlay.isOpen()) {
            searchSession.refresh();
            updateSearchOverlayCount();
            revealMatch(searchSession.getCurrentMatch());
        }
    }

    private void onNodeInfoProviderChanged(TreeNodeInfoProvider<T> oldProvider, TreeNodeInfoProvider<T> newProvider) {
        inlineInfoHost.clear(oldProvider);
        flattenedTree.setNodeInfoProvider(newProvider);
        if (newProvider == null) {
            nodeInfoModel.clear();
        }
        viewport.markDirty();
    }

    private void onEditCommit(TreeItem<T> item, String text) {
        if (item == null) {
            return;
        }
        editCommitHandler.accept(item, text);
        viewport.markDirty();
    }

    private void applyDockingTheme(Theme theme) {
        setTreeViewTheme(TreeViewThemeMapper.map(theme));
    }

    public void openSearch() {
        openSearch(seedFromFocusedItem());
    }

    public void openSearch(String initialQuery) {
        searchOverlay.open(initialQuery == null ? "" : initialQuery);
        searchSession.setQuery(searchOverlay.getQuery());
        updateSearchOverlayCount();
        revealMatch(searchSession.getCurrentMatch());
    }

    public void closeSearch() {
        if (searchOverlay.isOpen()) {
            searchOverlay.close();
        }
    }

    public boolean isSearchOpen() {
        return searchOverlay.isOpen();
    }

    public TreeItem<T> searchAndRevealFirst(String query) {
        return searchAndRevealFirst(query, getSearchTextExtractor());
    }

    public TreeItem<T> searchAndRevealFirst(String query, Function<T, String> textExtractor) {
        List<TreeItem<T>> matches = searchEngine.findAll(getRoot(), query, textExtractor, isShowRoot());
        if (matches.isEmpty()) {
            return null;
        }
        TreeItem<T> match = matches.getFirst();
        revealMatch(match);
        return match;
    }

    public TreeItem<T> searchNext() {
        TreeItem<T> next = searchSession.next();
        revealMatch(next);
        updateSearchOverlayCount();
        return next;
    }

    public TreeItem<T> searchPrevious() {
        TreeItem<T> previous = searchSession.previous();
        revealMatch(previous);
        updateSearchOverlayCount();
        return previous;
    }

    private void configureSearchOverlay() {
        searchOverlay.setTheme(getTreeViewTheme());
        searchOverlay.setOnQueryChanged(query -> {
            searchSession.setQuery(query);
            updateSearchOverlayCount();
            revealMatch(searchSession.getCurrentMatch());
        });
        searchOverlay.setOnNext(this::searchNext);
        searchOverlay.setOnPrevious(this::searchPrevious);
        searchOverlay.setOnClose(() -> {
            searchSession.clear();
            requestFocus();
            viewport.markDirty();
        });
    }

    private boolean handleSearchKeyPressed(KeyEvent event) {
        if (event == null || event.isConsumed()) {
            return false;
        }
        if (isOpenSearchShortcut(event)) {
            openSearch();
            event.consume();
            return true;
        }
        if (event.getCode() == KeyCode.ESCAPE && searchOverlay.isOpen() && !searchOverlay.isFocusWithin()) {
            closeSearch();
            event.consume();
            return true;
        }
        return false;
    }

    private void handleSearchKeyTyped(KeyEvent event) {
        if (!shouldOpenTypeToSearch(event)) {
            return;
        }
        searchOverlay.appendTyped(event.getCharacter());
        event.consume();
    }

    private boolean shouldOpenTypeToSearch(KeyEvent event) {
        if (event == null || event.isConsumed() || editController.isEditing() || searchOverlay.isFocusWithin()) {
            return false;
        }
        String character = event.getCharacter();
        if (character == null || character.isEmpty()) {
            return false;
        }
        char ch = character.charAt(0);
        if (ch < 32 || ch == 127) {
            return false;
        }
        if (event.isControlDown() || event.isMetaDown() || event.isAltDown()) {
            return false;
        }
        return true;
    }

    private boolean isOpenSearchShortcut(KeyEvent event) {
        return event.getCode() == KeyCode.F
            && (event.isControlDown() || event.isMetaDown())
            && !event.isAltDown();
    }

    private String seedFromFocusedItem() {
        TreeItem<T> focused = selectionModel.getFocusedItem();
        if (focused == null) {
            return "";
        }
        String text = getSearchTextExtractor().apply(focused.getValue());
        return text == null ? "" : text;
    }

    private void updateSearchOverlayCount() {
        searchOverlay.updateCount(searchSession.getCurrentMatchIndex(), searchSession.getMatchCount());
    }

    private void revealMatch(TreeItem<T> match) {
        if (match == null) {
            return;
        }
        TreeItem<T> parent = match.getParent();
        while (parent != null) {
            expansionModel.setExpanded(parent, true);
            parent = parent.getParent();
        }
        selectionModel.selectOnly(match);
        selectionModel.setFocusedItem(match);
        viewport.ensureItemVisible(match);
        viewport.markDirty();
    }

    private void syncInlineInfoHost() {
        inlineInfoHost.layer().resizeRelocate(0.0, 0.0, getWidth(), getHeight());
        inlineInfoClip.setX(0.0);
        inlineInfoClip.setY(0.0);
        inlineInfoClip.setWidth(Math.max(0.0, viewport.getEffectiveTextWidth()));
        inlineInfoClip.setHeight(Math.max(0.0, viewport.getEffectiveTextHeight()));
        inlineInfoHost.sync(
            viewport.getVisibleRows(),
            getNodeInfoProvider(),
            viewport.getEffectiveTextWidth(),
            viewport.getTheme().indentWidth(),
            viewport.getTheme().iconSize(),
            viewport.getHorizontalScrollOffset()
        );
    }

    private void applyNodeInfoToggleModeToViewport(TreeNodeInfoToggleMode mode) {
        TreeNodeInfoToggleMode safeMode = mode == null ? TreeNodeInfoToggleMode.KEYBOARD_AND_MOUSE : mode;
        viewport.setNodeInfoMouseToggleEnabled(safeMode.allowsMouse());
    }
}
