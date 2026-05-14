package org.metalib.papifly.fx.tree.util;

import java.util.List;

public record TreeViewStateData(
    List<List<Integer>> expandedPaths,
    List<List<Integer>> expandedInfoPaths,
    List<List<Integer>> selectedPaths,
    List<Integer> focusedPath,
    double scrollOffset,
    double horizontalScrollOffset
) {
    public static TreeViewStateData empty() {
        return new TreeViewStateData(List.of(), List.of(), List.of(), List.of(), 0.0, 0.0);
    }
}
