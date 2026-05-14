package org.metalib.papifly.fx.hugo.api;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
import org.metalib.papifly.fx.ui.UiStyleSupport;
import org.metalib.papifly.fx.ui.UiStatusSlot;

public final class HugoPreviewStatusBar extends UiStatusSlot {

    private final Label stateLabel = new Label("Stopped");
    private final Label messageLabel = new Label();
    private HugoServerProcessManager.State currentState = HugoServerProcessManager.State.STOPPED;
    private int currentPort;
    private Theme currentTheme = Theme.dark();

    public HugoPreviewStatusBar() {
        setId("hugo-preview-status");
        stateLabel.setId("hugo-preview-status-state");
        messageLabel.setId("hugo-preview-status-message");
        setSpacing(UiMetrics.SPACE_3);
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_3, UiMetrics.SPACE_1, UiMetrics.SPACE_3));
        setAlignment(Pos.CENTER_LEFT);

        stateLabel.setMinWidth(UiMetrics.SPACE_5 * 6.0);
        stateLabel.setAlignment(Pos.CENTER);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
        getChildren().setAll(stateLabel, messageLabel);

        applyVisualStyle();
        applyStateStyle();
    }

    public void setServerState(HugoServerProcessManager.State state, int port) {
        currentState = state == null ? HugoServerProcessManager.State.STOPPED : state;
        currentPort = port;
        applyStateStyle();
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }

    public String getStateText() {
        return stateLabel.getText();
    }

    public String getMessageText() {
        return messageLabel.getText();
    }

    public void applyVisualStyle() {
        Color background = UiCommonThemeSupport.headerBackground(currentTheme);
        Color border = UiCommonThemeSupport.border(currentTheme);
        Color text = UiCommonThemeSupport.textPrimary(currentTheme);
        setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            """.formatted(
            UiStyleSupport.paintToCss(background, "transparent"),
            UiStyleSupport.paintToCss(background.darker(), "transparent")
        )));
        setBorder(new Border(new BorderStroke(
            border,
            BorderStrokeStyle.SOLID,
            javafx.scene.layout.CornerRadii.EMPTY,
            new BorderWidths(1, 0, 0, 0)
        )));
        messageLabel.setTextFill(text);
    }

    public void applyTheme(Theme theme) {
        currentTheme = UiCommonThemeSupport.resolvedTheme(theme);
        applyVisualStyle();
        applyStateStyle();
        javafx.scene.text.Font font = currentTheme.contentFont();
        if (font != null) {
            stateLabel.setFont(font);
            messageLabel.setFont(font);
        }
    }

    private void applyStateStyle() {
        Color background = UiCommonThemeSupport.background(currentTheme);
        Color panel = UiCommonThemeSupport.headerBackgroundActive(currentTheme);
        Color accent = UiCommonThemeSupport.accent(currentTheme);
        Color border = UiCommonThemeSupport.border(currentTheme);
        Color pressed = UiCommonThemeSupport.pressed(currentTheme);
        Color danger = UiCommonThemeSupport.danger(currentTheme);
        Color textPrimary = UiCommonThemeSupport.textPrimary(currentTheme);
        Color textActive = UiCommonThemeSupport.textActive(currentTheme);

        Color stateBackground;
        Color stateBorder;
        Color stateText;

        if (currentState == HugoServerProcessManager.State.RUNNING && currentPort > 0) {
            stateLabel.setText("Running on " + currentPort);
            stateBackground = accent;
            stateBorder = accent;
            stateText = textActive;
        } else {
            switch (currentState) {
                case STARTING -> {
                    stateLabel.setText("Starting");
                    stateBackground = alpha(accent, 0.16);
                    stateBorder = accent;
                    stateText = textPrimary;
                }
                case RUNNING -> {
                    stateLabel.setText("Running");
                    stateBackground = accent;
                    stateBorder = accent;
                    stateText = textActive;
                }
                case ERROR -> {
                    stateLabel.setText("Error");
                    stateBackground = blend(pressed, danger, 0.55);
                    stateBorder = danger;
                    stateText = textActive;
                }
                case STOPPED -> {
                    stateLabel.setText("Stopped");
                    stateBackground = blend(panel, background, 0.26);
                    stateBorder = alpha(border, 0.85);
                    stateText = textPrimary;
                }
                default -> {
                    stateLabel.setText("Unknown");
                    stateBackground = blend(panel, background, 0.26);
                    stateBorder = alpha(border, 0.85);
                    stateText = textPrimary;
                }
            }
        }

        stateLabel.setStyle(compact("""
            -fx-background-color: linear-gradient(to bottom, %s, %s);
            -fx-text-fill: %s;
            -fx-padding: %.1f %.1f %.1f %.1f;
            -fx-background-radius: %.1f;
            -fx-border-radius: %.1f;
            -fx-border-color: %s;
            -fx-opacity: 1.0;
            """.formatted(
            UiStyleSupport.paintToCss(stateBackground, "transparent"),
            UiStyleSupport.paintToCss(stateBackground.darker(), "transparent"),
            UiStyleSupport.paintToCss(stateText, "transparent"),
            UiMetrics.SPACE_1,
            UiMetrics.SPACE_3,
            UiMetrics.SPACE_1,
            UiMetrics.SPACE_3,
            UiMetrics.RADIUS_PILL,
            UiMetrics.RADIUS_PILL,
            UiStyleSupport.paintToCss(stateBorder, "transparent")
        )));
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
}
