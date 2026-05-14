package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiPillButton;
import org.metalib.papifly.fx.ui.UiStyleSupport;

public final class HugoPreviewToolbar extends HBox {
    private final Button startButton = new UiPillButton("Start");
    private final Button stopButton = new UiPillButton("Stop");
    private final Button backButton = new Button("<");
    private final Button forwardButton = new Button(">");
    private final Button reloadButton = new Button("Reload");
    private final Button openInBrowserButton = new UiPillButton("Open Browser");
    private final Hyperlink addressLink = new Hyperlink("-");
    private final Tooltip addressTooltip = new Tooltip("-");
    private final Tooltip backTooltip = new Tooltip("Go to previous page");
    private final Tooltip forwardTooltip = new Tooltip("Go to next page");
    private final Tooltip reloadTooltip = new Tooltip("Reload current page");
    private final Tooltip openInBrowserTooltip = new Tooltip("Open current page in system browser");
    private final HBox serverControls;
    private final HBox navigationControls;
    private final HBox addressContainer;
    private Theme currentTheme = Theme.dark();

    public HugoPreviewToolbar(
        Runnable onStart,
        Runnable onStop,
        Runnable onBack,
        Runnable onForward,
        Runnable onReload,
        Runnable onOpenExternal
    ) {
        setId("hugo-preview-toolbar");
        setSpacing(UiMetrics.SPACE_2);
        setPadding(new Insets(UiMetrics.SPACE_2, UiMetrics.SPACE_3, UiMetrics.SPACE_2, UiMetrics.SPACE_3));
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(UiMetrics.TOOLBAR_HEIGHT);

        startButton.setId("hugo-preview-start");
        stopButton.setId("hugo-preview-stop");
        backButton.setId("hugo-preview-back");
        forwardButton.setId("hugo-preview-forward");
        reloadButton.setId("hugo-preview-reload");
        openInBrowserButton.setId("hugo-preview-open-browser");
        addressLink.setId("hugo-preview-address");

        configureAccessibleButton(
            backButton,
            backTooltip,
            "Previous page",
            "Navigate to previous page in preview history"
        );
        configureAccessibleButton(
            forwardButton,
            forwardTooltip,
            "Next page",
            "Navigate to next page in preview history"
        );
        configureAccessibleButton(
            reloadButton,
            reloadTooltip,
            "Reload page",
            "Reload the current preview page"
        );
        configureAccessibleButton(
            openInBrowserButton,
            openInBrowserTooltip,
            "Open in browser",
            "Open the current preview page in the default system browser"
        );

        startButton.setOnAction(event -> onStart.run());
        stopButton.setOnAction(event -> onStop.run());
        backButton.setOnAction(event -> onBack.run());
        forwardButton.setOnAction(event -> onForward.run());
        reloadButton.setOnAction(event -> onReload.run());
        openInBrowserButton.setOnAction(event -> onOpenExternal.run());
        addressLink.setOnAction(event -> onOpenExternal.run());

        styleButton(startButton, ButtonRole.EMPHASIS, UiMetrics.SPACE_3);
        styleButton(stopButton, ButtonRole.SECONDARY, UiMetrics.SPACE_3);
        styleButton(backButton, ButtonRole.NEUTRAL, UiMetrics.SPACE_2);
        styleButton(forwardButton, ButtonRole.NEUTRAL, UiMetrics.SPACE_2);
        styleButton(reloadButton, ButtonRole.NEUTRAL, UiMetrics.SPACE_2);
        styleButton(openInBrowserButton, ButtonRole.EMPHASIS, UiMetrics.SPACE_3);

        backButton.setPrefWidth(UiMetrics.SPACE_5 * 2.0);
        forwardButton.setPrefWidth(UiMetrics.SPACE_5 * 2.0);
        reloadButton.setPrefWidth(UiMetrics.SPACE_4 * 4.0);

        addressLink.setTooltip(addressTooltip);
        addressLink.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        addressLink.setMaxWidth(Double.MAX_VALUE);
        addressLink.setUnderline(false);
        addressLink.setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));

        serverControls = createGroup(startButton, stopButton);
        navigationControls = createGroup(backButton, forwardButton, reloadButton);
        addressContainer = new HBox(addressLink);
        addressContainer.setAlignment(Pos.CENTER_LEFT);
        addressContainer.setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));

        HBox.setHgrow(addressContainer, Priority.ALWAYS);
        addressLink.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressLink, Priority.ALWAYS);

        getChildren().setAll(
            serverControls,
            navigationControls,
            addressContainer,
            openInBrowserButton
        );
        applyVisualStyle();
        setServerState(HugoServerProcessManager.State.STOPPED);
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            addressLink.setText("-");
            addressTooltip.setText("-");
            return;
        }
        addressLink.setText(address);
        addressTooltip.setText(address);
    }

    public String getAddress() {
        return addressLink.getText();
    }

    public Hyperlink getAddressLink() {
        return addressLink;
    }

    public void applyVisualStyle() {
        Color toolbarBackground = UiCommonThemeSupport.headerBackground(currentTheme);
        Color toolbarBorder = UiCommonThemeSupport.border(currentTheme);
        setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            """.formatted(
            UiStyleSupport.paintToCss(toolbarBackground, "transparent"),
            UiStyleSupport.paintToCss(toolbarBackground.darker(), "transparent")
        )));
        setBorder(new Border(new BorderStroke(
            toolbarBorder,
            BorderStrokeStyle.SOLID,
            javafx.scene.layout.CornerRadii.EMPTY,
            new BorderWidths(0, 0, 1, 0)
        )));
        applyGroupStyle(serverControls);
        applyGroupStyle(navigationControls);
        applyAddressStyle();
        for (Button button : new Button[] {startButton, stopButton, backButton, forwardButton, reloadButton, openInBrowserButton}) {
            applyButtonStyle(button);
        }
    }

    public void applyTheme(Theme theme) {
        currentTheme = UiCommonThemeSupport.resolvedTheme(theme);
        applyVisualStyle();
        javafx.scene.text.Font font = currentTheme.contentFont();
        for (Button button : new Button[] {startButton, stopButton, backButton, forwardButton, reloadButton, openInBrowserButton}) {
            if (font != null) {
                button.setFont(font);
            }
        }
        if (font != null) {
            addressLink.setFont(font);
        }
    }

    public void setServerState(HugoServerProcessManager.State state) {
        if (state == HugoServerProcessManager.State.RUNNING || state == HugoServerProcessManager.State.STARTING) {
            startButton.setDisable(true);
            stopButton.setDisable(false);
            return;
        }
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    private HBox createGroup(javafx.scene.Node... children) {
        HBox group = new HBox(UiMetrics.SPACE_2, children);
        group.setAlignment(Pos.CENTER_LEFT);
        group.setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        return group;
    }

    private void styleButton(Button button, ButtonRole role, double horizontalPadding) {
        button.setUserData(new ButtonStyles(role, horizontalPadding));
        button.setFocusTraversable(true);
        button.hoverProperty().addListener((obs, oldHover, hover) -> applyButtonStyle(button));
        button.disableProperty().addListener((obs, oldDisabled, disabled) -> applyButtonStyle(button));
        applyButtonStyle(button);
    }

    private void configureAccessibleButton(Button button, Tooltip tooltip, String accessibleText, String accessibleHelp) {
        button.setTooltip(tooltip);
        button.setAccessibleText(accessibleText);
        button.setAccessibleHelp(accessibleHelp);
    }

    private void applyButtonStyle(Button button) {
        Object userData = button.getUserData();
        if (!(userData instanceof ButtonStyles styles)) {
            return;
        }
        Color canvas = UiCommonThemeSupport.background(currentTheme);
        Color panel = UiCommonThemeSupport.headerBackground(currentTheme);
        Color accent = UiCommonThemeSupport.accent(currentTheme);
        Color border = UiCommonThemeSupport.border(currentTheme);
        Color hover = UiCommonThemeSupport.hover(currentTheme);
        Color pressed = UiCommonThemeSupport.pressed(currentTheme);
        Color textPrimary = UiCommonThemeSupport.textPrimary(currentTheme);
        Color textActive = UiCommonThemeSupport.textActive(currentTheme);

        Color baseBackground = switch (styles.role()) {
            case EMPHASIS -> accent;
            case SECONDARY -> pressed;
            case NEUTRAL -> blend(panel, canvas, 0.22);
        };
        Color baseBorder = styles.role() == ButtonRole.EMPHASIS ? accent : alpha(border, 0.90);
        Color baseText = styles.role() == ButtonRole.EMPHASIS ? textActive : textPrimary;

        if (button.isDisable()) {
            Color disabledBackground = blend(canvas, panel, 0.28);
            Color disabledBorder = alpha(border, 0.55);
            Color disabledText = alpha(textPrimary, 0.58);
            button.setStyle(buttonStyle(disabledBackground, disabledBorder, disabledText, styles.horizontalPadding()));
            return;
        }

        Color resolvedBackground = button.isHover()
            ? blend(baseBackground, hover, 0.50)
            : baseBackground;
        Color resolvedBorder = button.isHover()
            ? blend(baseBorder, accent, 0.32)
            : baseBorder;
        button.setStyle(buttonStyle(resolvedBackground, resolvedBorder, baseText, styles.horizontalPadding()));
    }

    private void applyGroupStyle(HBox group) {
        Color canvas = UiCommonThemeSupport.background(currentTheme);
        Color panel = UiCommonThemeSupport.headerBackground(currentTheme);
        Color border = UiCommonThemeSupport.border(currentTheme);
        Color background = blend(panel, canvas, 0.22);
        group.setBorder(new Border(new BorderStroke(
            alpha(border, 0.85),
            BorderStrokeStyle.SOLID,
            new javafx.scene.layout.CornerRadii(UiMetrics.RADIUS_MD),
            new BorderWidths(1)
        )));
        group.setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            -fx-background-radius: %.1f;
            """.formatted(
            UiStyleSupport.paintToCss(background, "transparent"),
            UiStyleSupport.paintToCss(background.darker(), "transparent"),
            UiMetrics.RADIUS_MD
        )));
    }

    private void applyAddressStyle() {
        Color canvas = UiCommonThemeSupport.background(currentTheme);
        Color panel = UiCommonThemeSupport.headerBackgroundActive(currentTheme);
        Color border = UiCommonThemeSupport.border(currentTheme);
        Color accent = UiCommonThemeSupport.accent(currentTheme);
        Color background = blend(panel, canvas, 0.18);

        addressContainer.setBorder(new Border(new BorderStroke(
            alpha(border, 0.82),
            BorderStrokeStyle.SOLID,
            new javafx.scene.layout.CornerRadii(UiMetrics.RADIUS_MD),
            new BorderWidths(1)
        )));
        addressContainer.setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            -fx-background-radius: %.1f;
            """.formatted(
            UiStyleSupport.paintToCss(background, "transparent"),
            UiStyleSupport.paintToCss(background.darker(), "transparent"),
            UiMetrics.RADIUS_MD
        )));
        addressLink.setTextFill(accent);
    }

    private String buttonStyle(Color background, Color border, Color text, double horizontalPadding) {
        return compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            -fx-border-color: %s;
            -fx-border-radius: %.1f;
            -fx-background-radius: %.1f;
            -fx-text-fill: %s;
            -fx-padding: %.1f %.1f %.1f %.1f;
            -fx-opacity: 1.0;
            """.formatted(
            UiStyleSupport.paintToCss(background, "transparent"),
            UiStyleSupport.paintToCss(background.darker(), "transparent"),
            UiStyleSupport.paintToCss(border, "transparent"),
            UiMetrics.RADIUS_MD,
            UiMetrics.RADIUS_MD,
            UiStyleSupport.paintToCss(text, "transparent"),
            UiMetrics.SPACE_1,
            horizontalPadding,
            UiMetrics.SPACE_1,
            horizontalPadding
        ));
    }

    private static Color blend(Color base, Color mix, double weight) {
        return base.interpolate(mix, clamp(weight));
    }

    private static Color alpha(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(opacity));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String compact(String style) {
        return style.replace("\n", "").trim();
    }

    private enum ButtonRole {
        NEUTRAL,
        SECONDARY,
        EMPHASIS
    }

    private record ButtonStyles(ButtonRole role, double horizontalPadding) {
    }
}
