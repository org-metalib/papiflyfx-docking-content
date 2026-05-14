package org.metalib.papifly.fx.hugo;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;

class HugoCliProbeTest {

    @Test
    void returnsFalseForMissingBinary() {
        HugoCliProbe probe = new HugoCliProbe("definitely-not-a-real-hugo-binary");
        assertFalse(probe.isAvailable(Duration.ofSeconds(1)));
    }
}
