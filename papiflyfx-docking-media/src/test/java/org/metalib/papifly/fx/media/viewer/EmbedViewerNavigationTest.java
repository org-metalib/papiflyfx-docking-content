package org.metalib.papifly.fx.media.viewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbedViewerNavigationTest {

    @Test
    void doesNotOpenExternallyWithoutWrapper() {
        assertFalse(EmbedViewer.shouldOpenExternally("https://www.youtube.com/watch?v=abc", null));
    }

    @Test
    void keepsLocalWrapperNavigationInternal() {
        String wrapper = EmbedViewer.wrapperUrlForTesting("https://www.youtube-nocookie.com/embed/abc");
        String localReload = wrapper.substring(0, wrapper.indexOf('?')) + "?src=local";
        assertFalse(EmbedViewer.shouldOpenExternally(wrapper, wrapper));
        assertFalse(EmbedViewer.shouldOpenExternally(localReload, wrapper));
        assertFalse(EmbedViewer.shouldOpenExternally("about:blank", wrapper));
    }

    @Test
    void opensExternalLinksWhenWrapperActive() {
        String wrapper = EmbedViewer.wrapperUrlForTesting("https://www.youtube-nocookie.com/embed/abc");
        assertTrue(EmbedViewer.shouldOpenExternally("https://www.youtube.com/watch?v=abc", wrapper));
    }
}
