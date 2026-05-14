package org.metalib.papifly.fx.hugo.api;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.hugo.FxTestUtil;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class HugoPreviewDockLifecycleFxTest {

    private DockManager dockManager;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();
        dockManager.setOwnerStage(stage);
        stage.setScene(new Scene(dockManager.getRootPane(), 900, 600));
        stage.show();
    }

    @Test
    void previewSupportsDockFloatMinimizeAndClose(@TempDir Path tempDir) throws Exception {
        Path script = writeReadyScript(tempDir.resolve("hugo-ready.sh"));
        HugoServerProcessManager processManager = new HugoServerProcessManager(
            (rootPath, port, options) -> List.of("sh", script.toString(), Integer.toString(port)),
            Duration.ofSeconds(2)
        );
        HugoCliProbe probe = new HugoCliProbe(List.of("sh", "-c", "echo hugo"));

        HugoPreviewPane pane = FxTestUtil.callFx(() -> new HugoPreviewPane(
            new HugoPreviewConfig(tempDir, "hugo:lifecycle", "/", 20200, false, false),
            probe,
            processManager
        ));

        DockLeaf leaf = FxTestUtil.callFx(() -> {
            DockLeaf created = dockManager.createLeaf("Hugo", pane);
            DockTabGroup group = dockManager.createTabGroup();
            group.addLeaf(created);
            dockManager.setRoot(group);
            return created;
        });

        FxTestUtil.runFx(() -> pane.startServerAndLoad("/"));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> processManager.isRunning()));

        FxTestUtil.runFx(() -> dockManager.floatLeaf(leaf));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> dockManager.getFloatingWindowManager().isFloating(leaf)));

        FxTestUtil.runFx(() -> dockManager.dockLeaf(leaf));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> !dockManager.getFloatingWindowManager().isFloating(leaf)));

        FxTestUtil.runFx(() -> dockManager.minimizeLeaf(leaf));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> dockManager.getMinimizedStore().isMinimized(leaf)));

        FxTestUtil.runFx(() -> dockManager.restoreLeaf(leaf));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> !dockManager.getMinimizedStore().isMinimized(leaf)));

        FxTestUtil.runFx(leaf::requestClose);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
            () -> FxTestUtil.callFx(() -> !processManager.isRunning()));

        assertFalse(processManager.isRunning());
        assertTrue(FxTestUtil.callFx(() -> leaf.getContent() == null));
    }

    private Path writeReadyScript(Path path) throws Exception {
        Files.writeString(path, """
            #!/bin/sh
            PORT="$1"
            echo "Web Server is available at http://127.0.0.1:${PORT}/"
            while true; do
              sleep 1
            done
            """);
        if (!path.toFile().setExecutable(true)) {
            throw new IllegalStateException("Unable to mark script executable");
        }
        return path;
    }
}
