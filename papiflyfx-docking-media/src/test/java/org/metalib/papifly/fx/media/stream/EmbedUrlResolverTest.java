package org.metalib.papifly.fx.media.stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmbedUrlResolverTest {

    @Test void resolvesYouTubeWatchUrl() {
        String result = EmbedUrlResolver.resolve("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesYouTubeShortUrl() {
        String result = EmbedUrlResolver.resolve("https://youtu.be/dQw4w9WgXcQ");
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesYouTubeEmbedUrl() {
        String result = EmbedUrlResolver.resolve("https://www.youtube.com/embed/dQw4w9WgXcQ");
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesYouTubeNoCookieEmbedUrl() {
        String result = EmbedUrlResolver.resolve("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ", result);
    }

    @Test void resolvesVimeoUrl() {
        String result = EmbedUrlResolver.resolve("https://vimeo.com/123456789");
        assertEquals("https://player.vimeo.com/video/123456789", result);
    }

    @Test void resolvesTwitchChannelUrl() {
        String result = EmbedUrlResolver.resolve("https://www.twitch.tv/somechannel");
        assertTrue(result.contains("player.twitch.tv"));
        assertTrue(result.contains("channel=somechannel"));
        assertTrue(result.contains("parent=localhost"));
    }

    @Test void alreadyNonYouTubeEmbedUrlPassedThrough() {
        String embed = "https://player.vimeo.com/video/abc";
        assertEquals(embed, EmbedUrlResolver.resolve(embed));
    }
}
