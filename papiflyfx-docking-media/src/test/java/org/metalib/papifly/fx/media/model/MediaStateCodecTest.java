package org.metalib.papifly.fx.media.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaStateCodecTest {

    @Test
    void roundTripFullState() {
        MediaState original = new MediaState(
            "file:///video.mp4", UrlKind.FILE_VIDEO, 5000L, 0.75, true, 2.0, -10.0, 5.0);
        MediaState restored = MediaStateCodec.fromMap(MediaStateCodec.toMap(original));
        assertEquals(original, restored);
    }

    @Test
    void roundTripEmbedKind() {
        MediaState original = new MediaState(
            "https://www.youtube.com/embed/abc", UrlKind.EMBED, 0L, 1.0, false, 1.0, 0.0, 0.0);
        MediaState restored = MediaStateCodec.fromMap(MediaStateCodec.toMap(original));
        assertEquals(UrlKind.EMBED, restored.urlKind());
    }

    @Test
    void fromNullMapReturnsEmpty() {
        MediaState s = MediaStateCodec.fromMap(null);
        assertEquals(MediaState.empty(), s);
    }

    @Test
    void missingKeysUseDefaults() {
        MediaState s = MediaStateCodec.fromMap(java.util.Map.of());
        assertEquals(1.0, s.volume());
        assertEquals(1.0, s.zoomLevel());
        assertFalse(s.muted());
        assertEquals(UrlKind.UNKNOWN, s.urlKind());
    }
}
