package org.metalib.papifly.fx.media.model;

public enum UrlKind {
    FILE_IMAGE,
    FILE_VIDEO,
    FILE_AUDIO,
    FILE_SVG,
    STREAM_HLS,
    STREAM_HTTP_VIDEO,
    EMBED,
    RTSP,
    UNKNOWN;

    public String key() { return name().toLowerCase().replace('_', '-'); }

    public static UrlKind fromKey(String key) {
        if (key == null) return UNKNOWN;
        for (UrlKind k : values()) {
            if (k.key().equals(key)) return k;
        }
        return UNKNOWN;
    }
}
