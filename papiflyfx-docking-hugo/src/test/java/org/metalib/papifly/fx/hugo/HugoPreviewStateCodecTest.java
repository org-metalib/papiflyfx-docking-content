package org.metalib.papifly.fx.hugo;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.hugo.api.HugoPreviewState;
import org.metalib.papifly.fx.hugo.api.HugoPreviewStateCodec;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugoPreviewStateCodecTest {

    @Test
    void roundTripPreservesValues() {
        HugoPreviewState state = new HugoPreviewState("/tmp/site", "/docs/intro/", true);
        Map<String, Object> map = HugoPreviewStateCodec.toMap(state);
        HugoPreviewState restored = HugoPreviewStateCodec.fromMap(map);

        assertEquals(state.siteDir(), restored.siteDir());
        assertEquals(state.relativePath(), restored.relativePath());
        assertEquals(state.drafts(), restored.drafts());
    }

    @Test
    void nullMapUsesDefaults() {
        HugoPreviewState restored = HugoPreviewStateCodec.fromMap(null);

        assertEquals("/", restored.relativePath());
        assertTrue(restored.drafts());
    }
}
