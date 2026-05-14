package org.metalib.papifly.fx.media.stream;

import org.metalib.papifly.fx.media.model.UrlKind;

import java.util.Locale;
import java.util.Set;

public final class StreamUrlDetector {

    private static final Set<String> EMBED_HOSTS = Set.of(
        "www.youtube.com", "youtube.com", "youtu.be",
        "player.vimeo.com", "vimeo.com",
        "www.twitch.tv", "player.twitch.tv", "twitch.tv"
    );

    private StreamUrlDetector() {}

    public static UrlKind detect(String url) {
        if (url == null || url.isBlank()) return UrlKind.UNKNOWN;

        String lower = url.toLowerCase(Locale.ROOT);

        if (lower.startsWith("rtsp://") || lower.startsWith("rtsps://")
                || lower.startsWith("rtmp://") || lower.startsWith("rtmps://")) {
            return UrlKind.RTSP;
        }

        String host = extractHost(lower);
        if (EMBED_HOSTS.contains(host)) {
            return UrlKind.EMBED;
        }

        if (lower.endsWith(".m3u8") || lower.contains(".m3u8?")) {
            return UrlKind.STREAM_HLS;
        }
        if (lower.endsWith(".svg")) return UrlKind.FILE_SVG;
        if (lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".m4v")) {
            return lower.startsWith("http") ? UrlKind.STREAM_HTTP_VIDEO : UrlKind.FILE_VIDEO;
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav")
                || lower.endsWith(".aac") || lower.endsWith(".aiff")) {
            return UrlKind.FILE_AUDIO;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".bmp")
                || lower.endsWith(".gif")) {
            return UrlKind.FILE_IMAGE;
        }
        return UrlKind.UNKNOWN;
    }

    private static String extractHost(String url) {
        int slashSlash = url.indexOf("//");
        if (slashSlash < 0) return "";
        int start = slashSlash + 2;
        int slash  = url.indexOf('/', start);
        int query  = url.indexOf('?', start);
        int end = slash < 0 ? (query < 0 ? url.length() : query)
                            : (query < 0 ? slash : Math.min(slash, query));
        return url.substring(start, end);
    }
}
