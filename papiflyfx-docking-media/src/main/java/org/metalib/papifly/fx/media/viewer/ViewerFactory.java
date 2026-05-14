package org.metalib.papifly.fx.media.viewer;

import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.media.model.UrlKind;

public final class ViewerFactory {

    private ViewerFactory() {}

    public static StackPane create(String url, UrlKind kind) {
        if (url == null || url.isBlank() || kind == UrlKind.UNKNOWN) {
            return new ErrorViewer("Unsupported or missing media URL: " + url);
        }
        return switch (kind) {
            case FILE_IMAGE                       -> new ImageViewer();
            case FILE_VIDEO, STREAM_HLS,
                 STREAM_HTTP_VIDEO               -> new VideoViewer();
            case FILE_AUDIO                       -> new AudioViewer();
            case FILE_SVG                         -> new SvgViewer(url);
            case EMBED                            -> new EmbedViewer(url);
            case RTSP                             -> createRtspViewer(url);
            default                               -> new ErrorViewer("Unsupported: " + url);
        };
    }

    private static StackPane createRtspViewer(String url) {
        // vlcj integration is optional; show a descriptive error when absent
        try {
            Class.forName("uk.co.caprica.vlcj.player.base.MediaPlayer");
            // TODO: return new VlcjVideoViewer(url);
            return new ErrorViewer("vlcj viewer not yet implemented for: " + url);
        } catch (ClassNotFoundException e) {
            return new ErrorViewer("RTSP/RTMP requires vlcj on the classpath.\n" + url);
        }
    }
}
