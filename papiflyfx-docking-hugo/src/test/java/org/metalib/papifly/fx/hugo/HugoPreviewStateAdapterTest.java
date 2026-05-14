package org.metalib.papifly.fx.hugo;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.hugo.api.HugoPreviewConfig;
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoPreviewPane;
import org.metalib.papifly.fx.hugo.api.HugoPreviewStateAdapter;
import org.metalib.papifly.fx.hugo.api.HugoPreviewStateCodec;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class HugoPreviewStateAdapterTest {

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 320, 240));
        stage.show();
    }

    @Test
    void saveStateContainsExpectedKeys(@TempDir Path tempDir) {
        HugoPreviewStateAdapter adapter = new HugoPreviewStateAdapter();
        HugoPreviewPane pane = FxTestUtil.callFx(() -> new HugoPreviewPane(new HugoPreviewConfig(
            tempDir,
            "hugo:test",
            "/",
            1313,
            false,
            false
        )));

        Map<String, Object> map = FxTestUtil.callFx(() -> adapter.saveState("hugo:test", pane));
        assertTrue(map.containsKey(HugoPreviewStateCodec.KEY_SITE_DIR));
        assertTrue(map.containsKey(HugoPreviewStateCodec.KEY_RELATIVE_PATH));
        assertTrue(map.containsKey(HugoPreviewStateCodec.KEY_DRAFTS));

        FxTestUtil.runFx(pane::dispose);
    }

    @Test
    void restoreBuildsPaneFromState(@TempDir Path tempDir) {
        HugoPreviewStateAdapter adapter = new HugoPreviewStateAdapter();
        LeafContentData contentData = LeafContentData.of(
            HugoPreviewFactory.FACTORY_ID,
            "hugo:restored",
            HugoPreviewStateAdapter.VERSION,
            Map.of(
                HugoPreviewStateCodec.KEY_SITE_DIR, tempDir.toString(),
                HugoPreviewStateCodec.KEY_RELATIVE_PATH, "/posts/intro/",
                HugoPreviewStateCodec.KEY_DRAFTS, Boolean.TRUE
            )
        );

        Node node = FxTestUtil.callFx(() -> adapter.restore(contentData));
        assertNotNull(node);
        assertTrue(node instanceof HugoPreviewPane);

        HugoPreviewPane pane = (HugoPreviewPane) node;
        Path siteRoot = FxTestUtil.callFx(pane::getCurrentSiteRoot);
        assertEquals(tempDir.toAbsolutePath().normalize(), siteRoot);

        FxTestUtil.runFx(pane::dispose);
    }
}
