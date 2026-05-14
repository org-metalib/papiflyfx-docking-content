package org.metalib.papifly.fx.github.ui.theme;

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

class GitHubToolbarThemeMapperTest {

    @Test
    void mapNullUsesDarkThemeFallback() {
        GitHubToolbarTheme result = GitHubToolbarThemeMapper.map(null);
        assertNotNull(result);
        assertEquals(Theme.dark().headerBackground(), result.toolbarBackground());
        assertEquals(Theme.dark().accentColor(), result.accent());
    }

    @Test
    void mapDarkThemePropagatesAccentAndMetrics() {
        GitHubToolbarTheme result = GitHubToolbarThemeMapper.map(Theme.dark());
        assertEquals(Theme.dark().accentColor(), result.accent());
        assertEquals(Theme.dark().accentColor(), result.focusBorder());
        assertEquals(Theme.dark().accentColor(), result.busyIndicator());
        assertEquals(UiMetrics.RADIUS_MD, result.cornerRadius());
        assertEquals(UiMetrics.CONTROL_HEIGHT_REGULAR, result.buttonHeight());
        assertEquals(UiMetrics.TOOLBAR_HEIGHT, result.toolbarHeight());
        assertEquals(UiMetrics.SPACE_2, result.groupGap());
    }

    @Test
    void mapLightThemeProducesDarkPrimaryText() {
        GitHubToolbarTheme result = GitHubToolbarThemeMapper.map(Theme.light());
        assertNotNull(result);
        Color text = (Color) result.textPrimary();
        assertTrue(text.getBrightness() < 0.5);
        assertFalse(GitHubToolbarThemeMapper.isDark(Theme.light().background()));
    }

    @Test
    void customAccentPropagatesToBusyAndFocus() {
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
        GitHubToolbarTheme result = GitHubToolbarThemeMapper.map(custom);
        assertEquals(Color.RED, result.accent());
        assertEquals(Color.RED, result.focusBorder());
        assertEquals(Color.RED, result.busyIndicator());
    }

    @Test
    void spaciousThemeDimensionsSnapToSharedGrid() {
        Theme spacious = Theme.of(
            Theme.dark().colors(),
            Theme.dark().fonts(),
            new ThemeDimensions(
                8.0,
                1.0,
                40.0,
                32.0,
                new Insets(9.0, 13.0, 11.0, 15.0),
                12.0,
                24.0
            )
        );

        GitHubToolbarTheme result = GitHubToolbarThemeMapper.map(spacious);

        assertEquals(12.0, result.cornerRadius(), 0.01);
        assertEquals(36.0, result.buttonHeight(), 0.01);
        assertEquals(56.0, result.toolbarHeight(), 0.01);
        assertEquals(12.0, result.groupGap(), 0.01);
        assertEquals(12.0, result.contentPadding().getTop(), 0.01);
        assertEquals(20.0, result.contentPadding().getRight(), 0.01);
        assertEquals(16.0, result.contentPadding().getBottom(), 0.01);
        assertEquals(24.0, result.contentPadding().getLeft(), 0.01);
    }

    @Test
    void isDarkDetectsDarkColors() {
        assertTrue(GitHubToolbarThemeMapper.isDark(Color.BLACK));
        assertTrue(GitHubToolbarThemeMapper.isDark(Color.web("#1e1e1e")));
        assertFalse(GitHubToolbarThemeMapper.isDark(Color.WHITE));
        assertFalse(GitHubToolbarThemeMapper.isDark(Color.web("#f0f0f0")));
    }
}
