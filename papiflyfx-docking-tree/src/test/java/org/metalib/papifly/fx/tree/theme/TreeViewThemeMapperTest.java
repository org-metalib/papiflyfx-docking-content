package org.metalib.papifly.fx.tree.theme;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docking.api.ThemeColors;
import org.metalib.papifly.fx.docking.api.ThemeDimensions;
import org.metalib.papifly.fx.docking.api.ThemeFonts;
import org.metalib.papifly.fx.ui.UiMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeViewThemeMapperTest {

    @Test
    void mapNullReturnsDefaultDark() {
        TreeViewTheme result = TreeViewThemeMapper.map(null);
        assertNotNull(result);
        assertEquals(TreeViewTheme.dark(), result);
    }

    @Test
    void mapDarkThemeUsesStandardMetricsAndAccentStates() {
        TreeViewTheme result = TreeViewThemeMapper.map(Theme.dark());
        assertEquals(Theme.dark().accentColor(), result.focusedBorder());
        assertEquals(UiMetrics.SPACE_6, result.rowHeight());
        assertEquals(UiMetrics.SPACE_5, result.indentWidth());
        assertEquals(UiMetrics.SPACE_4, result.iconSize());
    }

    @Test
    void mapLightThemeProducesDarkPrimaryText() {
        TreeViewTheme result = TreeViewThemeMapper.map(Theme.light());
        assertNotNull(result);
        Color text = (Color) result.textColor();
        assertTrue(text.getBrightness() < 0.5);
        assertFalse(TreeViewThemeMapper.isDark(Theme.light().background()));
    }

    @Test
    void customAccentPropagatesToFocusAndSelection() {
        Theme custom = Theme.of(
            new ThemeColors(
                Color.rgb(30, 30, 30),
                Color.rgb(45, 45, 45),
                Color.rgb(60, 60, 60),
                Color.RED,
                Color.rgb(200, 200, 200),
                Color.WHITE,
                Color.rgb(60, 60, 60),
                Color.rgb(80, 80, 80),
                Color.rgb(0, 122, 204, 0.3),
                Color.rgb(70, 70, 70),
                Color.rgb(90, 90, 90),
                Color.rgb(40, 40, 40)
            ),
            new ThemeFonts(
                javafx.scene.text.Font.font(12),
                javafx.scene.text.Font.font(12)
            ),
            new ThemeDimensions(
                4.0,
                1.0,
                28.0,
                24.0,
                Insets.EMPTY,
                8.0,
                24.0
            )
        );

        TreeViewTheme result = TreeViewThemeMapper.map(custom);
        assertEquals(Color.RED, result.focusedBorder());
        assertEquals(UiMetrics.SPACE_6, result.rowHeight());
    }

    @Test
    void spaciousThemeDimensionsExpandMetricsOnGrid() {
        Theme spacious = Theme.of(
            Theme.dark().colors(),
            Theme.dark().fonts(),
            new ThemeDimensions(
                8.0,
                1.0,
                40.0,
                36.0,
                Insets.EMPTY,
                12.0,
                24.0
            )
        );

        TreeViewTheme result = TreeViewThemeMapper.map(spacious);

        assertEquals(36.0, result.rowHeight(), 0.01);
        assertEquals(24.0, result.indentWidth(), 0.01);
        assertEquals(UiMetrics.SPACE_4, result.iconSize(), 0.01);
    }
}
