package org.metalib.papifly.fx.tree.api;

public record CellState(
    boolean selected,
    boolean focused,
    boolean hovered,
    boolean expanded,
    boolean leaf,
    int depth,
    double x,
    double y,
    double width,
    double height
) {
}
