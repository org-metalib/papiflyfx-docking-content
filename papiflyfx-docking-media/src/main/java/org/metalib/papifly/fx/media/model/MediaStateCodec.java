package org.metalib.papifly.fx.media.model;

import java.util.HashMap;
import java.util.Map;

public final class MediaStateCodec {

    private MediaStateCodec() {}

    public static Map<String, Object> toMap(MediaState s) {
        Map<String, Object> m = new HashMap<>();
        if (s.mediaUrl() != null) m.put("mediaUrl", s.mediaUrl());
        m.put("urlKind",       s.urlKind().key());
        m.put("currentTimeMs", s.currentTimeMs());
        m.put("volume",        s.volume());
        m.put("muted",         s.muted());
        m.put("zoomLevel",     s.zoomLevel());
        m.put("panX",          s.panX());
        m.put("panY",          s.panY());
        return m;
    }

    public static MediaState fromMap(Map<String, Object> m) {
        if (m == null) return MediaState.empty();
        return new MediaState(
            (String)  m.getOrDefault("mediaUrl",     null),
            UrlKind.fromKey((String) m.get("urlKind")),
            toLong(          m.getOrDefault("currentTimeMs", 0L)),
            toDouble(        m.getOrDefault("volume",        1.0)),
            toBoolean(       m.getOrDefault("muted",         false)),
            toDouble(        m.getOrDefault("zoomLevel",     1.0)),
            toDouble(        m.getOrDefault("panX",          0.0)),
            toDouble(        m.getOrDefault("panY",          0.0))
        );
    }

    private static long    toLong(Object v)    { return v instanceof Number n ? n.longValue()   : 0L; }
    private static double  toDouble(Object v)  { return v instanceof Number n ? n.doubleValue() : 0.0; }
    private static boolean toBoolean(Object v) { return v instanceof Boolean b ? b : false; }
}
