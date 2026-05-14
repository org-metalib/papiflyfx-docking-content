package org.metalib.papifly.fx.hugo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.hugo.process.HugoServerOptions;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugoServerProcessManagerTest {

    @Test
    void startStopLifecycle(@TempDir Path tempDir) throws Exception {
        Path script = writeExecutableScript(tempDir, "ready.sh", """
            #!/bin/sh
            PORT="$1"
            echo "Web Server is available at http://127.0.0.1:${PORT}/"
            while true; do
              sleep 1
            done
            """);

        HugoServerProcessManager manager = new HugoServerProcessManager(
            (siteRoot, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofSeconds(2)
        );

        URI endpoint = manager.start(tempDir, HugoServerOptions.defaults(19000));
        assertNotNull(endpoint);
        assertEquals("127.0.0.1", endpoint.getHost());
        assertTrue(endpoint.getPort() >= 19000);
        assertTrue(manager.isRunning());

        manager.stop();
        assertFalse(manager.isRunning());

        manager.stop();
        assertFalse(manager.isRunning());

        manager.close();
    }

    @Test
    void readinessTimeoutThrows(@TempDir Path tempDir) throws Exception {
        Path script = writeExecutableScript(tempDir, "timeout.sh", """
            #!/bin/sh
            sleep 10
            """);

        HugoServerProcessManager manager = new HugoServerProcessManager(
            (siteRoot, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofMillis(300)
        );

        assertThrows(TimeoutException.class,
            () -> manager.start(tempDir, HugoServerOptions.defaults(19500)));

        manager.close();
    }

    @Test
    void readinessRegexPath(@TempDir Path tempDir) throws Exception {
        Path script = writeExecutableScript(tempDir, "regex.sh", """
            #!/bin/sh
            PORT="$1"
            echo "Web Server is available at http://127.0.0.1:${PORT}/"
            sleep 3
            """);

        HugoServerProcessManager manager = new HugoServerProcessManager(
            (siteRoot, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofSeconds(2)
        );

        URI endpoint = manager.start(tempDir, HugoServerOptions.defaults(19600));
        assertEquals("127.0.0.1", endpoint.getHost());
        manager.stop();
        manager.close();
    }

    private Path writeExecutableScript(Path dir, String name, String script) throws Exception {
        Path file = dir.resolve(name);
        Files.writeString(file, script);
        assertTrue(file.toFile().setExecutable(true));
        return file;
    }
}
