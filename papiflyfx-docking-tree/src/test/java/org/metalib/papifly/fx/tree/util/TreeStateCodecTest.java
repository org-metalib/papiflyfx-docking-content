package org.metalib.papifly.fx.tree.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeStateCodecTest {

    @Test
    void roundTripIncludesExpandedInfoPaths() {
        TreeViewStateData data = new TreeViewStateData(
            List.of(List.of(0), List.of(1, 2)),
            List.of(List.of(3), List.of(4, 5)),
            List.of(List.of(0, 1)),
            List.of(0, 1, 2),
            120.5,
            32.0
        );

        Map<String, Object> encoded = TreeStateCodec.toMap(data);
        TreeViewStateData decoded = TreeStateCodec.fromMap(encoded);

        assertEquals(data, decoded);
    }

    @Test
    void fromMapRejectsMissingExpandedInfoPaths() {
        Map<String, Object> payload = Map.of(
            "expandedPaths", List.of(List.of(0)),
            "selectedPaths", List.of(List.of(0, 1)),
            "focusedPath", List.of(0, 1),
            "scrollOffset", 10.0,
            "horizontalScrollOffset", 4.0
        );

        assertThrows(IllegalArgumentException.class, () -> TreeStateCodec.fromMap(payload));
    }
}
