package org.metalib.papifly.fx.media.stream;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.media.model.UrlKind;

import static org.junit.jupiter.api.Assertions.*;

class StreamUrlDetectorTest {

    @Test void detectsM3u8AsHls() {
        assertEquals(UrlKind.STREAM_HLS, StreamUrlDetector.detect("https://cdn.example.com/stream.m3u8"));
    }

    @Test void detectsYouTubeAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://www.youtube.com/watch?v=abc123"));
    }

    @Test void detectsVimeoAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://vimeo.com/123456789"));
    }

    @Test void detectsTwitchAsEmbed() {
        assertEquals(UrlKind.EMBED, StreamUrlDetector.detect("https://www.twitch.tv/somechannel"));
    }

    @Test void detectsRtspAsRtsp() {
        assertEquals(UrlKind.RTSP, StreamUrlDetector.detect("rtsp://192.168.1.10/camera"));
    }

    @Test void detectsRtmpAsRtsp() {
        assertEquals(UrlKind.RTSP, StreamUrlDetector.detect("rtmp://live.example.com/stream"));
    }

    @Test void detectsMp4HttpAsStreamVideo() {
        assertEquals(UrlKind.STREAM_HTTP_VIDEO, StreamUrlDetector.detect("https://example.com/video.mp4"));
    }

    @Test void detectsLocalMp4AsFileVideo() {
        assertEquals(UrlKind.FILE_VIDEO, StreamUrlDetector.detect("file:///home/user/video.mp4"));
    }

    @Test void detectsPngAsFileImage() {
        assertEquals(UrlKind.FILE_IMAGE, StreamUrlDetector.detect("file:///home/user/photo.png"));
    }

    @Test void detectsSvgAsFileSvg() {
        assertEquals(UrlKind.FILE_SVG, StreamUrlDetector.detect("file:///icons/logo.svg"));
    }

    @Test void nullReturnsUnknown() {
        assertEquals(UrlKind.UNKNOWN, StreamUrlDetector.detect(null));
    }

    @Test void blankReturnsUnknown() {
        assertEquals(UrlKind.UNKNOWN, StreamUrlDetector.detect("  "));
    }
}
