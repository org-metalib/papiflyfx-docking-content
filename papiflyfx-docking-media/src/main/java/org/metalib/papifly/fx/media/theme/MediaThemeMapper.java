package org.metalib.papifly.fx.media.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;

public final class MediaThemeMapper {

    private MediaThemeMapper() {}

    public static Paint controlBackground(Theme t) { return t.headerBackground(); }
    public static Paint controlForeground(Theme t) { return t.textColor(); }
    public static Paint accent(Theme t)            { return t.accentColor(); }
    public static Paint border(Theme t)            { return t.borderColor(); }

    public static Color toColor(Paint p) {
        return p instanceof Color c ? c : UiCommonThemeSupport.textPrimary(Theme.dark());
    }
}
