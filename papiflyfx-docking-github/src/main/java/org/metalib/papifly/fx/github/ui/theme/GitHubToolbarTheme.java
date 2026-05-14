package org.metalib.papifly.fx.github.ui.theme;

import javafx.geometry.Insets;
import javafx.scene.paint.Paint;

public record GitHubToolbarTheme(
    Paint toolbarBackground,
    Paint toolbarBorder,
    Paint groupBackground,
    Paint groupBorder,
    Paint controlBackground,
    Paint controlBackgroundHover,
    Paint controlBackgroundPressed,
    Paint controlBorder,
    Paint focusBorder,
    Paint textPrimary,
    Paint textMuted,
    Paint textDisabled,
    Paint linkText,
    Paint accent,
    Paint success,
    Paint warning,
    Paint danger,
    Paint badgeBackground,
    Paint badgeBorder,
    Paint statusBackground,
    Paint errorBackground,
    Paint busyTrack,
    Paint busyIndicator,
    Paint shadow,
    double cornerRadius,
    double compactRadius,
    double toolbarHeight,
    double buttonHeight,
    Insets contentPadding,
    double groupGap
) {
}
