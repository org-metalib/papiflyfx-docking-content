package org.metalib.papifly.fx.tree.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TreeStateCodec {

    private TreeStateCodec() {}

    public static Map<String, Object> toMap(TreeViewStateData data) {
        TreeViewStateData safeData = data == null ? TreeViewStateData.empty() : data;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("expandedPaths", encodePaths(safeData.expandedPaths()));
        map.put("expandedInfoPaths", encodePaths(safeData.expandedInfoPaths()));
        map.put("selectedPaths", encodePaths(safeData.selectedPaths()));
        map.put("focusedPath", encodePath(safeData.focusedPath()));
        map.put("scrollOffset", safeData.scrollOffset());
        map.put("horizontalScrollOffset", safeData.horizontalScrollOffset());
        return map;
    }

    public static TreeViewStateData fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return TreeViewStateData.empty();
        }
        List<List<Integer>> expandedPaths = decodePaths(map.get("expandedPaths"));
        List<List<Integer>> expandedInfoPaths = decodeRequiredPaths(map, "expandedInfoPaths");
        List<List<Integer>> selectedPaths = decodePaths(map.get("selectedPaths"));
        List<Integer> focusedPath = decodePath(map.get("focusedPath"));
        double scrollOffset = decodeDouble(map.get("scrollOffset"), 0.0);
        double horizontalScrollOffset = decodeDouble(map.get("horizontalScrollOffset"), 0.0);
        return new TreeViewStateData(expandedPaths, expandedInfoPaths, selectedPaths, focusedPath, scrollOffset, horizontalScrollOffset);
    }

    private static List<List<Integer>> decodeRequiredPaths(Map<String, Object> map, String key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Missing " + key);
        }
        Object value = map.get(key);
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("Invalid " + key);
        }
        return decodePaths(value);
    }

    private static List<Object> encodePaths(List<List<Integer>> paths) {
        List<Object> encoded = new ArrayList<>();
        if (paths == null) {
            return encoded;
        }
        for (List<Integer> path : paths) {
            encoded.add(encodePath(path));
        }
        return encoded;
    }

    private static List<Object> encodePath(List<Integer> path) {
        List<Object> encoded = new ArrayList<>();
        if (path == null) {
            return encoded;
        }
        encoded.addAll(path);
        return encoded;
    }

    private static List<List<Integer>> decodePaths(Object value) {
        if (!(value instanceof List<?> rawPaths)) {
            return List.of();
        }
        List<List<Integer>> decoded = new ArrayList<>();
        for (Object rawPath : rawPaths) {
            decoded.add(decodePath(rawPath));
        }
        return decoded;
    }

    private static List<Integer> decodePath(Object value) {
        if (!(value instanceof List<?> rawPath)) {
            return List.of();
        }
        List<Integer> decoded = new ArrayList<>();
        for (Object segment : rawPath) {
            if (segment instanceof Number number) {
                decoded.add(number.intValue());
            }
        }
        return decoded;
    }

    private static double decodeDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}
