package org.metalib.papifly.fx.tree.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiMetrics;

public record TreeViewTheme(
    Paint background,
    Paint rowBackground,
    Paint rowBackgroundAlternate,
    Paint selectedBackground,
    Paint selectedBackgroundUnfocused,
    Paint focusedBorder,
    Paint hoverBackground,
    Paint textColor,
    Paint textColorSelected,
    Paint disclosureColor,
    Paint connectingLineColor,
    Paint scrollbarTrackColor,
    Paint scrollbarThumbColor,
    Paint scrollbarThumbHoverColor,
    Paint scrollbarThumbActiveColor,
    Font font,
    double rowHeight,
    double indentWidth,
    double iconSize
) {
    public static TreeViewTheme dark() {
        return TreeViewThemeMapper.map(Theme.dark());
    }

    public static TreeViewTheme light() {
        return TreeViewThemeMapper.map(Theme.light());
    }
}
