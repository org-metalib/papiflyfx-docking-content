package org.metalib.papifly.fx.hugo.api;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HugoPreviewStateCodec {

    public static final String KEY_SITE_DIR = "siteDir";
    public static final String KEY_RELATIVE_PATH = "relativePath";
    public static final String KEY_DRAFTS = "drafts";

    private HugoPreviewStateCodec() {
    }

    public static Map<String, Object> toMap(HugoPreviewState state) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (state == null) {
            return map;
        }
        if (state.siteDir() != null && !state.siteDir().isBlank()) {
            map.put(KEY_SITE_DIR, state.siteDir());
        }
        map.put(KEY_RELATIVE_PATH, normalizePath(state.relativePath()));
        map.put(KEY_DRAFTS, state.drafts());
        return map;
    }

    public static HugoPreviewState fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new HugoPreviewState(null, "/", true);
        }
        return new HugoPreviewState(
            asString(map.get(KEY_SITE_DIR)),
            normalizePath(asString(map.getOrDefault(KEY_RELATIVE_PATH, "/"))),
            asBoolean(map.getOrDefault(KEY_DRAFTS, Boolean.TRUE), true)
        );
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        return value instanceof Boolean b ? b : defaultValue;
    }
}
