package org.metalib.papifly.fx.tree.util;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class GlyphCache {

    private static final Font DEFAULT_FONT = Font.font("System", 13);

    private final Text measureNode = new Text();
    private Font font;
    private double lineHeight;
    private double charWidth;
    private double baselineOffset;

    public GlyphCache() {
        setFont(DEFAULT_FONT);
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public double getCharWidth() {
        return charWidth;
    }

    public double getBaselineOffset() {
        return baselineOffset;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font == null ? DEFAULT_FONT : font;
        measureNode.setFont(this.font);
        measureNode.setText("M");
        charWidth = measureNode.getLayoutBounds().getWidth();
        measureNode.setText("Hg");
        lineHeight = measureNode.getLayoutBounds().getHeight();
        baselineOffset = -measureNode.getLayoutBounds().getMinY();
    }

    public double measureTextWidth(String value) {
        measureNode.setText(value == null ? "" : value);
        return measureNode.getLayoutBounds().getWidth();
    }
}
