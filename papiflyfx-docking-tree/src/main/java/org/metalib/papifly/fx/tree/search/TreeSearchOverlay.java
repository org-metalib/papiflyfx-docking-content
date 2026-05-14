package org.metalib.papifly.fx.tree.search;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.searchui.SearchIconPaths;
import org.metalib.papifly.fx.searchui.SearchOverlayBase;
import org.metalib.papifly.fx.tree.theme.TreeViewTheme;
import org.metalib.papifly.fx.ui.UiCommonPalette;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TreeSearchOverlay extends SearchOverlayBase {

    private static final String STYLESHEET_NAME = "tree-search-overlay.css";
    private static final double COMPACT_WIDTH_THRESHOLD = UiMetrics.SPACE_6 * 11.0;
    private static final double OVERLAY_PREF_WIDTH = UiMetrics.SPACE_6 * 16.0;

    private final TextField queryField = new TextField();
    private final Label resultLabel = new Label();
    private final List<SVGPath> iconNodes = new ArrayList<>();

    private Runnable onNext = () -> {};
    private Runnable onPrevious = () -> {};
    private Consumer<String> onQueryChanged = value -> {};
    private Runnable onClose = () -> {};

    private boolean programmaticUpdate;
    private boolean compactLayout;
    private boolean hasResultText;

    public TreeSearchOverlay() {
        super();
        getStyleClass().addAll("pf-tree-search-overlay", "pf-ui-popup-surface");
        setSpacing(UiMetrics.SPACE_1);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        setPrefWidth(OVERLAY_PREF_WIDTH);
        setMaxWidth(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        ensureStylesheetLoaded();

        queryField.setPromptText("Find");
        queryField.getStyleClass().addAll("pf-tree-search-field", "pf-ui-compact-field");
        queryField.setMinWidth(UiMetrics.SPACE_6 * 2.0);
        queryField.setMinHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.setPrefHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.setMaxHeight(UiMetrics.CONTROL_HEIGHT_COMPACT);
        queryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!programmaticUpdate) {
                onQueryChanged.accept(newValue == null ? "" : newValue);
            }
        });
        queryField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    onPrevious.run();
                } else {
                    onNext.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            }
        });

        resultLabel.getStyleClass().addAll("pf-tree-search-result-label", "pf-ui-result-label");
        resultLabel.setMinWidth(UiMetrics.SPACE_6 * 2.0);
        resultLabel.setPrefWidth(UiMetrics.SPACE_5 * 4.0);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        resultLabel.setManaged(false);
        resultLabel.setVisible(false);

        Button prevButton = createIconButton(SearchIconPaths.ARROW_UP, UiMetrics.SPACE_3 - UiMetrics.SPACE_1, onPrevious);
        Button nextButton = createIconButton(SearchIconPaths.ARROW_DOWN, UiMetrics.SPACE_3 - UiMetrics.SPACE_1, onNext);
        Button closeButton = createIconButton(SearchIconPaths.CLOSE, UiMetrics.SPACE_3 - UiMetrics.SPACE_1, this::close);

        HBox row = new HBox(UiMetrics.SPACE_1, queryField, resultLabel, prevButton, nextButton, closeButton);
        row.getStyleClass().add("pf-tree-search-row");
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        getChildren().add(row);
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            compactLayout = newWidth.doubleValue() > 0.0 && newWidth.doubleValue() < COMPACT_WIDTH_THRESHOLD;
            updateResultLabelVisibility();
        });
    }

    public void setOnQueryChanged(Consumer<String> onQueryChanged) {
        this.onQueryChanged = onQueryChanged == null ? value -> {} : onQueryChanged;
    }

    public void setOnNext(Runnable onNext) {
        this.onNext = onNext == null ? () -> {} : onNext;
    }

    public void setOnPrevious(Runnable onPrevious) {
        this.onPrevious = onPrevious == null ? () -> {} : onPrevious;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose == null ? () -> {} : onClose;
    }

    @Override
    public void open(String initialQuery) {
        showOverlay();
        if (initialQuery != null) {
            withProgrammaticUpdate(() -> queryField.setText(initialQuery));
            onQueryChanged.accept(initialQuery);
        }
        queryField.requestFocus();
        queryField.selectAll();
    }

    public void appendTyped(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!isOpen()) {
            open(text);
            queryField.positionCaret(queryField.getText().length());
            return;
        }
        withProgrammaticUpdate(() -> queryField.appendText(text));
        onQueryChanged.accept(queryField.getText());
        queryField.positionCaret(queryField.getText().length());
    }

    @Override
    public void close() {
        hideOverlay();
        withProgrammaticUpdate(queryField::clear);
        hasResultText = false;
        resultLabel.setText("");
        updateResultLabelVisibility();
        onClose.run();
    }

    public boolean isOpen() {
        return isVisible();
    }

    public String getQuery() {
        return queryField.getText();
    }

    public void updateCount(int currentIndex, int matchCount) {
        String query = getQuery();
        if (query == null || query.isBlank()) {
            hasResultText = false;
            resultLabel.setText("");
            updateResultLabelVisibility();
            return;
        }
        hasResultText = true;
        if (matchCount <= 0) {
            resultLabel.setText("No results");
            updateResultLabelVisibility();
            return;
        }
        int displayIndex = Math.max(1, Math.min(currentIndex + 1, matchCount));
        resultLabel.setText(displayIndex + " of " + matchCount);
        updateResultLabelVisibility();
    }

    public void setTheme(TreeViewTheme theme) {
        TreeViewTheme safeTheme = theme == null ? TreeViewTheme.dark() : theme;
        boolean dark = UiCommonThemeSupport.isDark(safeTheme.background());
        UiCommonPalette palette = new UiCommonPalette(
            safeTheme.rowBackgroundAlternate(),
            safeTheme.connectingLineColor(),
            safeTheme.textColor(),
            withOpacity(safeTheme.textColor(), 0.66),
            withOpacity(safeTheme.textColor(), 0.5),
            safeTheme.rowBackground(),
            safeTheme.hoverBackground(),
            safeTheme.selectedBackground(),
            safeTheme.focusedBorder(),
            safeTheme.focusedBorder(),
            UiCommonThemeSupport.semanticColor(dark, UiCommonThemeSupport.SemanticTone.SUCCESS),
            UiCommonThemeSupport.semanticColor(dark, UiCommonThemeSupport.SemanticTone.WARNING),
            UiCommonThemeSupport.semanticColor(dark, UiCommonThemeSupport.SemanticTone.DANGER),
            safeTheme.focusedBorder(),
            UiCommonThemeSupport.shadowColor(dark ? Theme.dark() : Theme.light(), 0.25, 0.18)
        );
        setStyle(UiStyleSupport.metricVariables()
            + UiStyleSupport.fontVariables(null)
            + UiCommonThemeSupport.themeVariables(palette));
        Color iconColor = UiStyleSupport.asColor(
            safeTheme.textColor(),
            UiStyleSupport.asColor(safeTheme.textColorSelected(), Color.TRANSPARENT)
        );
        for (SVGPath icon : iconNodes) {
            icon.setFill(iconColor);
        }
    }

    private Button createIconButton(String path, double size, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("pf-tree-search-icon-button", "pf-ui-icon-button");
        button.setGraphic(createIcon(path, size));
        button.setFocusTraversable(false);
        button.setOnAction(event -> action.run());
        button.setMinSize(UiMetrics.ICON_BUTTON_SIZE_COMPACT, UiMetrics.ICON_BUTTON_SIZE_COMPACT);
        button.setPrefSize(UiMetrics.ICON_BUTTON_SIZE_COMPACT, UiMetrics.ICON_BUTTON_SIZE_COMPACT);
        button.setMaxSize(UiMetrics.ICON_BUTTON_SIZE_COMPACT, UiMetrics.ICON_BUTTON_SIZE_COMPACT);
        return button;
    }

    private SVGPath createIcon(String path, double size) {
        SVGPath icon = SearchIconPaths.createIcon(path, size);
        icon.getStyleClass().add("pf-ui-icon");
        iconNodes.add(icon);
        return icon;
    }

    private void withProgrammaticUpdate(Runnable action) {
        boolean previous = programmaticUpdate;
        programmaticUpdate = true;
        try {
            action.run();
        } finally {
            programmaticUpdate = previous;
        }
    }

    private void updateResultLabelVisibility() {
        boolean visible = hasResultText && !compactLayout;
        resultLabel.setManaged(visible);
        resultLabel.setVisible(visible);
    }

    private void ensureStylesheetLoaded() {
        URL stylesheetUrl = TreeSearchOverlay.class.getResource(STYLESHEET_NAME);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!getStylesheets().contains(stylesheet)) {
            getStylesheets().add(stylesheet);
        }
    }

    private static Color withOpacity(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    private static Color withOpacity(javafx.scene.paint.Paint paint, double opacity) {
        if (paint instanceof Color color) {
            return withOpacity(color, opacity);
        }
        return Color.TRANSPARENT;
    }
}
