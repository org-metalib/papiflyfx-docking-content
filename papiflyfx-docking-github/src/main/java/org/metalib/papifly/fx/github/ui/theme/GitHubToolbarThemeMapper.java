package org.metalib.papifly.fx.github.ui.theme;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiMetrics;

public final class GitHubToolbarThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    private GitHubToolbarThemeMapper() {
    }

    public static GitHubToolbarTheme map(Theme theme) {
        Theme resolved = theme == null ? Theme.dark() : theme;
        boolean dark = isDark(resolved.background());
        Theme fallback = UiCommonThemeSupport.fallbackTheme(resolved);

        Color background = asColor(resolved.background(), UiCommonThemeSupport.background(fallback));
        Color toolbarBackground = asColor(resolved.headerBackground(), UiCommonThemeSupport.headerBackground(fallback));
        Color activeBackground = asColor(resolved.headerBackgroundActive(),
            blend(toolbarBackground, asColor(resolved.accentColor(), UiCommonThemeSupport.accent(fallback)), dark ? 0.08 : 0.04));
        Color accent = asColor(resolved.accentColor(), UiCommonThemeSupport.accent(fallback));
        Color textPrimary = asColor(resolved.textColor(), UiCommonThemeSupport.textPrimary(fallback));
        Color textActive = asColor(resolved.textColorActive(), UiCommonThemeSupport.textActive(fallback));
        Color border = asColor(resolved.borderColor(), UiCommonThemeSupport.border(fallback));
        Color hover = asColor(resolved.buttonHoverBackground(), blend(activeBackground, accent, dark ? 0.10 : 0.06));
        Color pressed = asColor(resolved.buttonPressedBackground(), blend(activeBackground, accent, dark ? 0.18 : 0.12));
        Color groupBackground = blend(toolbarBackground, background, dark ? 0.28 : 0.18);
        Color groupBorder = blend(border, accent, dark ? 0.08 : 0.05);
        Color controlBackground = blend(activeBackground, background, dark ? 0.12 : 0.04);
        Color textMuted = blend(textPrimary, background, dark ? 0.36 : 0.48);
        Color textDisabled = blend(textPrimary, background, dark ? 0.52 : 0.62);
        Color linkText = blend(accent, textActive, dark ? 0.18 : 0.12);
        Color success = UiCommonThemeSupport.success(resolved);
        Color warning = UiCommonThemeSupport.warning(resolved);
        Color danger = UiCommonThemeSupport.danger(resolved);
        Color badgeBackground = blend(groupBackground, background, dark ? 0.10 : 0.04);
        Color badgeBorder = blend(border, textPrimary, dark ? 0.14 : 0.08);
        Color statusBackground = blend(groupBackground, background, dark ? 0.36 : 0.24);
        Color errorBackground = alpha(danger, dark ? 0.18 : 0.12);
        Color busyTrack = alpha(border, dark ? 0.35 : 0.24);
        Color busyIndicator = accent;
        Color shadow = UiCommonThemeSupport.shadowColor(resolved, 0.28, 0.16);

        Insets basePadding = resolved.contentPadding() == null ? Insets.EMPTY : resolved.contentPadding();
        Insets contentPadding = new Insets(
            Math.max(UiMetrics.SPACE_2, snapToGrid(basePadding.getTop() + UiMetrics.SPACE_1)),
            Math.max(UiMetrics.SPACE_3, snapToGrid(basePadding.getRight() + UiMetrics.SPACE_2)),
            Math.max(UiMetrics.SPACE_2, snapToGrid(basePadding.getBottom() + UiMetrics.SPACE_1)),
            Math.max(UiMetrics.SPACE_3, snapToGrid(basePadding.getLeft() + UiMetrics.SPACE_2))
        );

        return new GitHubToolbarTheme(
            toolbarBackground,
            border,
            groupBackground,
            groupBorder,
            controlBackground,
            hover,
            pressed,
            border,
            accent,
            textPrimary,
            textMuted,
            textDisabled,
            linkText,
            accent,
            success,
            warning,
            danger,
            badgeBackground,
            badgeBorder,
            statusBackground,
            errorBackground,
            busyTrack,
            busyIndicator,
            shadow,
            Math.max(UiMetrics.RADIUS_MD, snapToGrid(resolved.cornerRadius() + UiMetrics.SPACE_1)),
            Math.max(UiMetrics.RADIUS_SM, snapToGrid(resolved.cornerRadius())),
            Math.max(UiMetrics.TOOLBAR_HEIGHT, snapToGrid(resolved.headerHeight() + UiMetrics.SPACE_4)),
            Math.max(UiMetrics.CONTROL_HEIGHT_REGULAR, snapToGrid(resolved.tabHeight() + UiMetrics.SPACE_1)),
            contentPadding,
            Math.max(UiMetrics.SPACE_1, snapToGrid(resolved.buttonSpacing()))
        );
    }

    static boolean isDark(Paint paint) {
        if (paint instanceof Color color) {
            return color.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }

    private static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
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

    private static double snapToGrid(double value) {
        return Math.max(UiMetrics.SPACE_1, Math.rint(value / UiMetrics.SPACE_1) * UiMetrics.SPACE_1);
    }
}
