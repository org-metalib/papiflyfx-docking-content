package org.metalib.papifly.fx.github.ui.popup;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.metalib.papifly.fx.github.model.GitRefKind;
import org.metalib.papifly.fx.github.model.RefPopupEntry;
import org.metalib.papifly.fx.github.model.RefPopupSection;
import org.metalib.papifly.fx.github.ui.theme.GitHubThemeSupport;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class GitRefPopup {

    private final Popup popup;
    private final VBox root;
    private final TextField searchField;
    private final ListView<RowItem> listView;
    private final ObservableList<RowItem> rows;
    private final Popup submenuPopup;
    private final ListView<RefPopupEntry.Action> submenuList;

    private Consumer<RefPopupEntry.Action> actionHandler;
    private Consumer<RefPopupEntry.Ref> refHandler;
    private Runnable onHidden;

    public GitRefPopup() {
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);

        actionHandler = action -> {
        };
        refHandler = ref -> {
        };
        onHidden = () -> {
        };

        rows = FXCollections.observableArrayList();
        searchField = new TextField();
        searchField.setId("github-ref-popup-search");
        searchField.getStyleClass().addAll("pf-github-ref-popup-search", "pf-ui-compact-field");
        searchField.setPromptText("Search branches and tags");
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSearchKeyPressed);

        listView = new ListView<>(rows);
        listView.setId("github-ref-popup-list");
        listView.getStyleClass().addAll("pf-github-ref-popup-list", "pf-ui-popup-list");
        listView.setFocusTraversable(true);
        listView.setCellFactory(ignored -> new RowCell());
        listView.addEventFilter(KeyEvent.KEY_PRESSED, this::handleListKeyPressed);
        listView.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleListMouseReleased);

        root = new VBox(searchField, listView);
        root.setId("github-ref-popup");
        root.getStyleClass().addAll("pf-github-ref-popup", "pf-ui-popup-surface");
        root.setPrefWidth(392);
        root.setMaxHeight(520);
        UiStyleSupport.ensureCommonStylesheetLoaded(root);
        GitHubThemeSupport.ensureStylesheetLoaded(root, GitHubThemeSupport.TOOLBAR_STYLESHEET);

        popup.getContent().add(root);
        popup.setOnHidden(event -> {
            hideSubmenu();
            onHidden.run();
        });

        submenuPopup = new Popup();
        submenuPopup.setAutoHide(true);
        submenuPopup.setAutoFix(true);
        submenuPopup.setHideOnEscape(true);

        submenuList = new ListView<>();
        submenuList.setId("github-ref-submenu-list");
        submenuList.getStyleClass().addAll("pf-github-ref-submenu-list", "pf-ui-popup-list", "pf-ui-popup-surface");
        submenuList.setFocusTraversable(true);
        submenuList.setCellFactory(ignored -> new SubmenuCell());
        submenuList.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSubmenuKeyPressed);
        submenuList.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleSubmenuMouseReleased);
        UiStyleSupport.ensureCommonStylesheetLoaded(submenuList);
        GitHubThemeSupport.ensureStylesheetLoaded(submenuList, GitHubThemeSupport.TOOLBAR_STYLESHEET);
        submenuPopup.getContent().add(submenuList);
    }

    public void setSections(List<RefPopupSection> sections) {
        List<RowItem> flattened = new ArrayList<>();
        for (RefPopupSection section : sections) {
            if (!section.title().isBlank()) {
                flattened.add(new HeaderRow(section.title()));
            }
            for (RefPopupEntry entry : section.entries()) {
                flattened.add(new EntryRow(entry));
            }
        }
        rows.setAll(flattened);
        selectFirstEntry();
    }

    public void show(Node anchor) {
        Objects.requireNonNull(anchor, "anchor");
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            return;
        }
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + UiMetrics.SPACE_1 + UiMetrics.SPACE_1 * 0.5);
        searchField.requestFocus();
        selectFirstEntry();
    }

    public void hide() {
        hideSubmenu();
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public TextField searchField() {
        return searchField;
    }

    public void setStyle(String style) {
        root.setStyle(style);
        submenuList.setStyle(style);
    }

    public void setOnAction(Consumer<RefPopupEntry.Action> actionHandler) {
        this.actionHandler = Objects.requireNonNull(actionHandler, "actionHandler");
    }

    public void setOnRefActivated(Consumer<RefPopupEntry.Ref> refHandler) {
        this.refHandler = Objects.requireNonNull(refHandler, "refHandler");
    }

    public void setOnHidden(Runnable onHidden) {
        this.onHidden = Objects.requireNonNull(onHidden, "onHidden");
    }

    private void handleSearchKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            listView.requestFocus();
            selectRelative(1);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.UP) {
            listView.requestFocus();
            selectRelative(-1);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ENTER) {
            activateSelected();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            hide();
            event.consume();
        }
    }

    private void handleListKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> {
                selectRelative(-1);
                event.consume();
            }
            case DOWN -> {
                selectRelative(1);
                event.consume();
            }
            case ENTER -> {
                activateSelected();
                event.consume();
            }
            case RIGHT -> {
                openSelectedSubmenu();
                event.consume();
            }
            case LEFT -> {
                hideSubmenu();
                event.consume();
            }
            case ESCAPE -> {
                if (submenuPopup.isShowing()) {
                    hideSubmenu();
                } else {
                    hide();
                }
                event.consume();
            }
            default -> {
                if (isPrintableKey(event)) {
                    searchField.requestFocus();
                    searchField.appendText(event.getText());
                    event.consume();
                }
            }
        }
    }

    private void handleSubmenuKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER -> {
                activateSelectedSubmenuAction();
                event.consume();
            }
            case LEFT, ESCAPE -> {
                hideSubmenu();
                listView.requestFocus();
                event.consume();
            }
            default -> {
                if (isPrintableKey(event)) {
                    hideSubmenu();
                    searchField.requestFocus();
                    searchField.appendText(event.getText());
                    event.consume();
                }
            }
        }
    }

    private void handleListMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        ListCell<RowItem> cell = findParentCell(event.getPickResult().getIntersectedNode());
        if (cell == null || !(cell.getItem() instanceof EntryRow entryRow)) {
            return;
        }
        listView.getSelectionModel().select(cell.getIndex());
        if (entryRow.entry() instanceof RefPopupEntry.Ref ref && ref.hasSubmenu() && hasStyleClassInChain(
            event.getPickResult().getIntersectedNode(),
            cell,
            "pf-github-ref-popup-chevron"
        )) {
            openSubmenu(cell, ref);
            event.consume();
            return;
        }
        activateEntry(entryRow.entry());
        event.consume();
    }

    private void handleSubmenuMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        ListCell<RefPopupEntry.Action> cell = findParentCell(event.getPickResult().getIntersectedNode());
        if (cell == null || cell.getItem() == null || !cell.getItem().enabled()) {
            return;
        }
        submenuList.getSelectionModel().select(cell.getIndex());
        activateSelectedSubmenuAction();
        event.consume();
    }

    private void activateSelected() {
        RowItem item = listView.getSelectionModel().getSelectedItem();
        if (item instanceof EntryRow entryRow) {
            activateEntry(entryRow.entry());
        }
    }

    private void activateEntry(RefPopupEntry entry) {
        if (!entry.enabled()) {
            return;
        }
        if (entry instanceof RefPopupEntry.Action action) {
            actionHandler.accept(action);
            return;
        }
        if (entry instanceof RefPopupEntry.Ref ref) {
            refHandler.accept(ref);
        }
    }

    private void activateSelectedSubmenuAction() {
        RefPopupEntry.Action action = submenuList.getSelectionModel().getSelectedItem();
        if (action == null || !action.enabled()) {
            return;
        }
        hide();
        actionHandler.accept(action);
    }

    private void openSelectedSubmenu() {
        RowItem item = listView.getSelectionModel().getSelectedItem();
        if (!(item instanceof EntryRow entryRow)) {
            return;
        }
        if (entryRow.entry() instanceof RefPopupEntry.Ref ref && ref.hasSubmenu()) {
            int index = listView.getSelectionModel().getSelectedIndex();
            @SuppressWarnings("unchecked")
            ListCell<RowItem> cell = (ListCell<RowItem>) listView.lookup(".list-cell:selected");
            if (cell == null) {
                cell = findCell(index);
            }
            if (cell != null) {
                openSubmenu(cell, ref);
            }
        }
    }

    private ListCell<RowItem> findCell(int index) {
        for (Node node : listView.lookupAll(".list-cell")) {
            if (node instanceof ListCell<?> rawCell) {
                @SuppressWarnings("unchecked")
                ListCell<RowItem> cell = (ListCell<RowItem>) rawCell;
                if (cell.getIndex() == index) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static <T> ListCell<T> findParentCell(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof ListCell<?> rawCell) {
                @SuppressWarnings("unchecked")
                ListCell<T> cell = (ListCell<T>) rawCell;
                return cell;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean hasStyleClassInChain(Node node, Node boundary, String styleClass) {
        Node current = node;
        while (current != null && current != boundary) {
            if (current.getStyleClass().contains(styleClass)) {
                return true;
            }
            current = current.getParent();
        }
        return current != null && current.getStyleClass().contains(styleClass);
    }

    private void openSubmenu(Node owner, RefPopupEntry.Ref ref) {
        if (!ref.hasSubmenu()) {
            return;
        }
        submenuList.getItems().setAll(ref.submenuEntries());
        submenuList.getSelectionModel().selectFirst();
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        submenuPopup.show(owner, bounds.getMaxX() + UiMetrics.SPACE_1, bounds.getMinY());
        Platform.runLater(submenuList::requestFocus);
    }

    private void hideSubmenu() {
        submenuPopup.hide();
    }

    private void selectFirstEntry() {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index) instanceof EntryRow) {
                listView.getSelectionModel().select(index);
                listView.scrollTo(index);
                return;
            }
        }
        listView.getSelectionModel().clearSelection();
    }

    private void selectRelative(int direction) {
        if (rows.isEmpty()) {
            return;
        }
        int current = listView.getSelectionModel().getSelectedIndex();
        int index = current;
        if (index < 0) {
            index = direction > 0 ? -1 : rows.size();
        }
        while (true) {
            index += direction;
            if (index < 0 || index >= rows.size()) {
                return;
            }
            if (rows.get(index) instanceof EntryRow) {
                listView.getSelectionModel().select(index);
                listView.scrollTo(index);
                return;
            }
        }
    }

    private static boolean isPrintableKey(KeyEvent event) {
        String text = event.getText();
        return text != null && !text.isBlank() && !event.isMetaDown() && !event.isControlDown();
    }

    private sealed interface RowItem permits HeaderRow, EntryRow {
    }

    private record HeaderRow(String title) implements RowItem {
    }

    private record EntryRow(RefPopupEntry entry) implements RowItem {
    }

    private final class RowCell extends ListCell<RowItem> {

        @Override
        protected void updateItem(RowItem item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("pf-github-ref-popup-header-cell");
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setMouseTransparent(false);
                return;
            }
            if (item instanceof HeaderRow headerRow) {
                Label label = new Label(headerRow.title());
                label.getStyleClass().add("pf-github-ref-popup-header");
                setGraphic(label);
                setText(null);
                getStyleClass().add("pf-github-ref-popup-header-cell");
                setMouseTransparent(true);
                return;
            }
            EntryRow entryRow = (EntryRow) item;
            setGraphic(createEntryGraphic(entryRow.entry()));
            setText(null);
            setMouseTransparent(false);
        }

        private Node createEntryGraphic(RefPopupEntry entry) {
            Label icon = new Label(entryKindLabel(entry));
            icon.getStyleClass().add("pf-github-ref-popup-kind");
            if (entry instanceof RefPopupEntry.Ref ref) {
                icon.getStyleClass().add(kindStyle(ref.refKind()));
            } else {
                icon.getStyleClass().add("pf-github-ref-popup-kind-action");
            }

            Label primary = new Label(entry.primaryText());
            primary.getStyleClass().add("pf-github-ref-popup-primary");
            primary.setDisable(!entry.enabled());

            Label secondary = new Label(entry.secondaryText());
            secondary.getStyleClass().add("pf-github-ref-popup-secondary");
            secondary.setVisible(!entry.secondaryText().isBlank());
            secondary.setManaged(!entry.secondaryText().isBlank());
            secondary.setDisable(!entry.enabled());

            VBox textBox = new VBox(primary, secondary);
            textBox.getStyleClass().add("pf-github-ref-popup-text");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label chevron = new Label(entry instanceof RefPopupEntry.Ref ref && ref.hasSubmenu() ? ">" : "");
            chevron.getStyleClass().add("pf-github-ref-popup-chevron");
            chevron.setVisible(!chevron.getText().isBlank());
            chevron.setManaged(!chevron.getText().isBlank());

            BorderPane currentMarker = new BorderPane();
            currentMarker.getStyleClass().add("pf-github-ref-popup-current");
            boolean current = entry instanceof RefPopupEntry.Ref ref && ref.current();
            currentMarker.setVisible(current);
            currentMarker.setManaged(current);

            HBox box = new HBox(icon, textBox, spacer, currentMarker, chevron);
            box.getStyleClass().add("pf-github-ref-popup-entry");
            if (!entry.enabled()) {
                box.getStyleClass().add("pf-github-ref-popup-entry-disabled");
            }
            return box;
        }

        private static String entryKindLabel(RefPopupEntry entry) {
            if (entry instanceof RefPopupEntry.Ref ref) {
                return switch (ref.refKind()) {
                    case LOCAL_BRANCH -> "B";
                    case REMOTE_BRANCH -> "R";
                    case TAG -> "T";
                    case DETACHED_COMMIT -> "C";
                };
            }
            return "A";
        }

        private static String kindStyle(GitRefKind kind) {
            return switch (kind) {
                case LOCAL_BRANCH -> "pf-github-ref-popup-kind-local";
                case REMOTE_BRANCH -> "pf-github-ref-popup-kind-remote";
                case TAG -> "pf-github-ref-popup-kind-tag";
                case DETACHED_COMMIT -> "pf-github-ref-popup-kind-detached";
            };
        }
    }

    private final class SubmenuCell extends ListCell<RefPopupEntry.Action> {

        @Override
        protected void updateItem(RefPopupEntry.Action item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label primary = new Label(item.primaryText());
            primary.getStyleClass().add("pf-github-ref-popup-primary");
            primary.setDisable(!item.enabled());

            Label secondary = new Label(item.secondaryText());
            secondary.getStyleClass().add("pf-github-ref-popup-secondary");
            secondary.setVisible(!item.secondaryText().isBlank());
            secondary.setManaged(!item.secondaryText().isBlank());
            secondary.setDisable(!item.enabled());

            VBox box = new VBox(primary, secondary);
            box.getStyleClass().add("pf-github-ref-popup-entry");
            if (!item.enabled()) {
                box.getStyleClass().add("pf-github-ref-popup-entry-disabled");
            }
            setGraphic(box);
            setText(null);
        }
    }
}
