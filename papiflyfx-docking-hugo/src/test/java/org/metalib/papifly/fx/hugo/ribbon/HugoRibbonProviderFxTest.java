package org.metalib.papifly.fx.hugo.ribbon;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.docks.ribbon.Ribbon;
import org.metalib.papifly.fx.docks.ribbon.RibbonManager;
import org.metalib.papifly.fx.hugo.FxTestUtil;
import org.metalib.papifly.fx.hugo.api.HugoRibbonActions;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class HugoRibbonProviderFxTest {

    private RibbonManager ribbonManager;
    private Ribbon ribbon;

    @Start
    void start(Stage stage) {
        ribbonManager = new RibbonManager(List.of(new HugoRibbonProvider()));
        ribbon = new Ribbon(ribbonManager);
        stage.setScene(new Scene(ribbon, 1100, 300));
        stage.show();
        settle();
    }

    @Test
    void explicitMetadataControlsMountedContextualTabAndCapabilityEnablement() {
        assertTrue(FxTestUtil.callFx(() -> ribbonManager.hasTab("hugo")));
        assertFalse(FxTestUtil.callFx(() -> ribbonManager.hasTab("hugo-editor")));
        assertFalse(command("hugo.ribbon.build.site").enabled().get());

        StubActions actions = new StubActions();
        FxTestUtil.runFx(() -> ribbonManager.setContext(new RibbonContext(
            "dock-hugo",
            "draft.txt",
            "sample.other",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "draft.txt"),
            Map.of(HugoRibbonActions.class, actions)
        ).withAttribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY, "hugo")));
        settle();

        assertTrue(FxTestUtil.callFx(() -> ribbonManager.hasTab("hugo-editor")));
        RibbonCommand frontMatter = command("hugo.ribbon.editor.front-matter");
        assertTrue(frontMatter.enabled().get());

        FxTestUtil.runFx(frontMatter::execute);
        assertEquals(1, actions.frontMatterCount);

        FxTestUtil.runFx(() -> {
            ribbon.setSelectedTabId("hugo-editor");
            ribbonManager.setContext(new RibbonContext(
                "dock-code",
                "content/post.md",
                "sample.markdown",
                Map.of(RibbonContextAttributes.DOCK_TITLE, "content/post.md"),
                Map.of(HugoRibbonActions.class, actions)
            ).withAttribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY, "code"));
        });
        settle();

        assertFalse(FxTestUtil.callFx(() -> ribbonManager.hasTab("hugo-editor")));
        assertEquals("hugo", FxTestUtil.callFx(ribbon::getSelectedTabId));
        assertFalse(FxTestUtil.callFx(() ->
            ribbonManager.getCommandRegistry().contains("hugo.ribbon.editor.front-matter")));
    }

    private RibbonCommand command(String id) {
        return FxTestUtil.callFx(() -> ribbonManager.getCommandRegistry().find(id).orElseThrow());
    }

    private static void settle() {
        FxTestUtil.waitForFx();
        FxTestUtil.waitForFx();
    }

    private static final class StubActions implements HugoRibbonActions {
        private int frontMatterCount;

        @Override
        public boolean isServerRunning() {
            return false;
        }

        @Override
        public boolean canRunHugoCommands() {
            return true;
        }

        @Override
        public void toggleServer() {
        }

        @Override
        public void newContent(String relativePath) {
        }

        @Override
        public void build() {
        }

        @Override
        public void mod(String subCommand) {
        }

        @Override
        public void env() {
        }

        @Override
        public void frontMatterTemplate() {
            frontMatterCount++;
        }

        @Override
        public void insertShortcode(String shortcodeName) {
        }
    }
}
