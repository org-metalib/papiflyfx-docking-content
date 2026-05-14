package org.metalib.papifly.fx.media.model;

public record MediaState(
    String  mediaUrl,
    UrlKind urlKind,
    long    currentTimeMs,
    double  volume,
    boolean muted,
    double  zoomLevel,
    double  panX,
    double  panY
) {
    public static MediaState empty() {
        return new MediaState(null, UrlKind.UNKNOWN, 0L, 1.0, false, 1.0, 0.0, 0.0);
    }

    public static MediaState ofUrl(String url, UrlKind kind) {
        return new MediaState(url, kind, 0L, 1.0, false, 1.0, 0.0, 0.0);
    }
}
