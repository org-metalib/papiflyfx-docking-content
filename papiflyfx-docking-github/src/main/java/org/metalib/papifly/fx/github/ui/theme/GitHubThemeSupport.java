package org.metalib.papifly.fx.github.ui.theme;

import javafx.scene.Parent;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;

public final class GitHubThemeSupport {

    public static final String TOOLBAR_STYLESHEET = "/org/metalib/papifly/fx/github/ui/github-toolbar.css";
    public static final String DIALOG_STYLESHEET = "/org/metalib/papifly/fx/github/ui/github-dialog.css";

    private GitHubThemeSupport() {
    }

    public static void ensureStylesheetLoaded(Parent parent, String resourcePath) {
        URL stylesheetUrl = GitHubThemeSupport.class.getResource(resourcePath);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!parent.getStylesheets().contains(stylesheet)) {
            parent.getStylesheets().add(stylesheet);
        }
    }

    public static String themeVariables(GitHubToolbarTheme theme) {
        Theme fallbackTheme = Theme.dark();
        Color accent = UiStyleSupport.asColor(theme.accent(), UiCommonThemeSupport.accent(fallbackTheme));
        Color success = UiStyleSupport.asColor(theme.success(), UiCommonThemeSupport.success(fallbackTheme));
        Color warning = UiStyleSupport.asColor(theme.warning(), UiCommonThemeSupport.warning(fallbackTheme));
        Color danger = UiStyleSupport.asColor(theme.danger(), UiCommonThemeSupport.danger(fallbackTheme));
        return UiStyleSupport.metricVariables() + """
            -pf-github-toolbar-bg: %s;
            -pf-github-toolbar-border: %s;
            -pf-github-group-bg: %s;
            -pf-github-group-border: %s;
            -pf-github-control-bg: %s;
            -pf-github-control-hover-bg: %s;
            -pf-github-control-pressed-bg: %s;
            -pf-github-control-border: %s;
            -pf-github-focus-border: %s;
            -pf-github-text: %s;
            -pf-github-muted-text: %s;
            -pf-github-disabled-text: %s;
            -pf-github-link: %s;
            -pf-github-accent: %s;
            -pf-github-success: %s;
            -pf-github-warning: %s;
            -pf-github-danger: %s;
            -pf-github-badge-bg: %s;
            -pf-github-badge-border: %s;
            -pf-github-status-bg: %s;
            -pf-github-error-bg: %s;
            -pf-github-shadow: %s;
            -pf-github-busy-track: %s;
            -pf-github-busy-indicator: %s;
            -pf-ui-surface-panel: %s;
            -pf-ui-surface-panel-subtle: %s;
            -pf-ui-surface-overlay: %s;
            -pf-ui-surface-control: %s;
            -pf-ui-surface-control-hover: %s;
            -pf-ui-surface-control-pressed: %s;
            -pf-ui-surface-selected: %s;
            -pf-ui-text-primary: %s;
            -pf-ui-text-muted: %s;
            -pf-ui-text-disabled: %s;
            -pf-ui-border-default: %s;
            -pf-ui-border-subtle: %s;
            -pf-ui-border-focus: %s;
            -pf-ui-accent: %s;
            -pf-ui-accent-subtle: %s;
            -pf-ui-success: %s;
            -pf-ui-success-subtle: %s;
            -pf-ui-warning: %s;
            -pf-ui-warning-subtle: %s;
            -pf-ui-danger: %s;
            -pf-ui-danger-subtle: %s;
            -pf-ui-shadow-overlay: %s;
            """.formatted(
            UiStyleSupport.paintToCss(theme.toolbarBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.toolbarBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.groupBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.groupBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackgroundHover(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackgroundPressed(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.focusBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.textPrimary(), "transparent"),
            UiStyleSupport.paintToCss(theme.textMuted(), "transparent"),
            UiStyleSupport.paintToCss(theme.textDisabled(), "transparent"),
            UiStyleSupport.paintToCss(theme.linkText(), "transparent"),
            UiStyleSupport.paintToCss(theme.accent(), "transparent"),
            UiStyleSupport.paintToCss(theme.success(), "transparent"),
            UiStyleSupport.paintToCss(theme.warning(), "transparent"),
            UiStyleSupport.paintToCss(theme.danger(), "transparent"),
            UiStyleSupport.paintToCss(theme.badgeBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.badgeBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.statusBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.errorBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.shadow(), "transparent"),
            UiStyleSupport.paintToCss(theme.busyTrack(), "transparent"),
            UiStyleSupport.paintToCss(theme.busyIndicator(), "transparent"),
            UiStyleSupport.paintToCss(theme.groupBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.badgeBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.groupBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackground(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackgroundHover(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBackgroundPressed(), "transparent"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(accent, UiCommonThemeSupport.accent(fallbackTheme), 0.12), "transparent"),
            UiStyleSupport.paintToCss(theme.textPrimary(), "transparent"),
            UiStyleSupport.paintToCss(theme.textMuted(), "transparent"),
            UiStyleSupport.paintToCss(theme.textDisabled(), "transparent"),
            UiStyleSupport.paintToCss(theme.groupBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.controlBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.focusBorder(), "transparent"),
            UiStyleSupport.paintToCss(theme.accent(), "transparent"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(accent, UiCommonThemeSupport.accent(fallbackTheme), 0.14), "transparent"),
            UiStyleSupport.paintToCss(theme.success(), "transparent"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(success, UiCommonThemeSupport.success(fallbackTheme), 0.14), "transparent"),
            UiStyleSupport.paintToCss(theme.warning(), "transparent"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(warning, UiCommonThemeSupport.warning(fallbackTheme), 0.14), "transparent"),
            UiStyleSupport.paintToCss(theme.danger(), "transparent"),
            UiStyleSupport.paintToCss(UiStyleSupport.alpha(danger, UiCommonThemeSupport.danger(fallbackTheme), 0.18), "transparent"),
            UiStyleSupport.paintToCss(theme.shadow(), "transparent")
        );
    }
}
