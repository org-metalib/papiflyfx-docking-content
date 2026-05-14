package org.metalib.papifly.fx.hugo.ribbon;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonControlSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoRibbonActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugoRibbonProviderTest {

    private static final String LEGACY_ACTIVE_CONTENT_NODE = "activeContentNode";

    @Test
    void mapsHugoCommandsAndRoutesToActiveActions() {
        StubActions actions = new StubActions();
        RibbonContext context = new RibbonContext(
            "dock-hugo",
            "hugo:docs",
            HugoPreviewFactory.FACTORY_ID,
            Map.of(
                RibbonContextAttributes.DOCK_TITLE, "content/posts/welcome.md",
                RibbonContextAttributes.CONTENT_FACTORY_ID, HugoPreviewFactory.FACTORY_ID
            ),
            Map.of(HugoRibbonActions.class, actions)
        );
        HugoRibbonProvider provider = new HugoRibbonProvider();

        assertFalse(context.attribute(LEGACY_ACTIVE_CONTENT_NODE).isPresent());
        List<RibbonTabSpec> tabs = visibleTabs(provider, context);
        assertEquals(2, tabs.size());
        RibbonTabSpec hugoTab = tabs.stream().filter(tab -> tab.id().equals("hugo")).findFirst().orElseThrow();
        RibbonTabSpec editorTab = tabs.stream().filter(tab -> tab.id().equals("hugo-editor")).findFirst().orElseThrow();

        command(hugoTab, "hugo.ribbon.development.server").execute();
        command(hugoTab, "hugo.ribbon.new-content.post").execute();
        command(hugoTab, "hugo.ribbon.build.site").execute();
        command(hugoTab, "hugo.ribbon.modules.tidy").execute();
        command(hugoTab, "hugo.ribbon.environment.env").execute();
        command(editorTab, "hugo.ribbon.editor.front-matter").execute();
        command(editorTab, "hugo.ribbon.editor.shortcode.youtube").execute();

        assertEquals(1, actions.toggleCount);
        assertEquals(1, actions.newContentCount);
        assertEquals(1, actions.buildCount);
        assertEquals(1, actions.modCount);
        assertEquals(1, actions.envCount);
        assertEquals(1, actions.frontMatterCount);
        assertEquals(1, actions.shortcodeCount);
    }

    @Test
    void contextualEditorTabAppearsForMarkdownContextAndHidesOtherwise() {
        HugoRibbonProvider provider = new HugoRibbonProvider();

        RibbonContext markdownContext = new RibbonContext(
            "dock-md",
            "content/post.md",
            "sample.markdown",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "content/post.md")
        );
        List<RibbonTabSpec> markdownTabs = visibleTabs(provider, markdownContext);
        assertTrue(markdownTabs.stream().anyMatch(tab -> tab.id().equals("hugo-editor")));
        RibbonTabSpec editorTab = markdownTabs.stream().filter(tab -> tab.id().equals("hugo-editor")).findFirst().orElseThrow();
        assertFalse(command(editorTab, "hugo.ribbon.editor.front-matter").enabled().get());

        RibbonContext javaContext = new RibbonContext(
            "dock-java",
            "src/App.java",
            "sample.code",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "App.java")
        );
        List<RibbonTabSpec> javaTabs = visibleTabs(provider, javaContext);
        assertFalse(javaTabs.stream().anyMatch(tab -> tab.id().equals("hugo-editor")));
    }

    @Test
    void contextualEditorTabPrefersExplicitMetadataOverLegacyHeuristics() {
        HugoRibbonProvider provider = new HugoRibbonProvider();

        RibbonContext explicitHugo = new RibbonContext(
            "dock-explicit",
            "draft.txt",
            "sample.other",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "draft.txt")
        ).withAttribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY, "hugo");
        assertTrue(hasEditorTab(provider, explicitHugo));

        RibbonContext explicitCodeMarkdown = new RibbonContext(
            "dock-code",
            "content/post.md",
            "sample.markdown",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "content/post.md")
        ).withAttribute(RibbonContextAttributes.CONTENT_DOMAIN_KEY, "code");
        assertFalse(hasEditorTab(provider, explicitCodeMarkdown),
            "explicit non-Hugo domain should suppress legacy markdown/path fallback");
    }

    @Test
    void contextualEditorTabKeepsLegacyHeuristicFallbacks() {
        HugoRibbonProvider provider = new HugoRibbonProvider();
        List<LegacyCase> cases = List.of(
            new LegacyCase(
                "active type key",
                new RibbonContext("dock-type", "draft.txt", HugoPreviewFactory.FACTORY_ID,
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "draft.txt"))
            ),
            new LegacyCase(
                "content factory id",
                new RibbonContext("dock-factory", "draft.txt", "sample.other",
                    Map.of(
                        RibbonContextAttributes.DOCK_TITLE, "draft.txt",
                        RibbonContextAttributes.CONTENT_FACTORY_ID, HugoPreviewFactory.FACTORY_ID
                    ))
            ),
            new LegacyCase(
                ".md file extension",
                new RibbonContext("dock-md", "docs/readme.md", "sample.other",
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "readme.txt"))
            ),
            new LegacyCase(
                ".markdown file extension",
                new RibbonContext("dock-markdown", "docs/readme.markdown", "sample.other",
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "readme.txt"))
            ),
            new LegacyCase(
                "/content/ path segment",
                new RibbonContext("dock-content", "site/content/page.txt", "sample.other",
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "page.txt"))
            ),
            new LegacyCase(
                "type key containing markdown",
                new RibbonContext("dock-type-markdown", "docs/readme.txt", "sample.markdown",
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "readme.txt"))
            ),
            new LegacyCase(
                "type key containing hugo",
                new RibbonContext("dock-type-hugo", "docs/readme.txt", "sample.hugo",
                    Map.of(RibbonContextAttributes.DOCK_TITLE, "readme.txt"))
            )
        );

        for (LegacyCase legacyCase : cases) {
            assertTrue(hasEditorTab(provider, legacyCase.context()), legacyCase.label());
        }
    }

    @Test
    void contextualEditorTabHidesForNegativeNonHugoContent() {
        HugoRibbonProvider provider = new HugoRibbonProvider();

        RibbonContext javaContext = new RibbonContext(
            "dock-java",
            "src/App.java",
            "sample.code",
            Map.of(RibbonContextAttributes.DOCK_TITLE, "App.java")
        );

        assertFalse(hasEditorTab(provider, javaContext));
    }

    @Test
    void resolvesTypedCapabilitiesWithoutLegacyNodeAttribute() {
        StubActions actions = new StubActions();
        RibbonContext context = new RibbonContext(
            "dock-hugo",
            "content/posts/welcome.md",
            HugoPreviewFactory.FACTORY_ID,
            Map.of(
                RibbonContextAttributes.DOCK_TITLE, "content/posts/welcome.md",
                RibbonContextAttributes.CONTENT_FACTORY_ID, HugoPreviewFactory.FACTORY_ID
            ),
            Map.of(HugoRibbonActions.class, actions)
        );
        HugoRibbonProvider provider = new HugoRibbonProvider();

        assertFalse(context.attribute(LEGACY_ACTIVE_CONTENT_NODE).isPresent());
        assertEquals(actions, context.capability(HugoRibbonActions.class).orElseThrow());
        assertEquals(actions, context.capability(StubActions.class).orElseThrow());

        RibbonTabSpec hugoTab = visibleTabs(provider, context).stream()
            .filter(tab -> tab.id().equals("hugo"))
            .findFirst()
            .orElseThrow();
        RibbonCommand build = command(hugoTab, "hugo.ribbon.build.site");
        assertTrue(build.enabled().get());

        build.execute();

        assertEquals(1, actions.buildCount);
    }

    private static List<RibbonTabSpec> visibleTabs(HugoRibbonProvider provider, RibbonContext context) {
        return provider.getTabs(context).stream()
            .filter(tab -> tab.isVisible(context))
            .toList();
    }

    private static boolean hasEditorTab(HugoRibbonProvider provider, RibbonContext context) {
        return visibleTabs(provider, context).stream().anyMatch(tab -> tab.id().equals("hugo-editor"));
    }

    private static RibbonCommand command(RibbonTabSpec tab, String id) {
        return commands(tab).stream()
            .filter(command -> command.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing command: " + id));
    }

    private static List<RibbonCommand> commands(RibbonTabSpec tab) {
        List<RibbonCommand> commands = new ArrayList<>();
        for (RibbonGroupSpec group : tab.groups()) {
            for (RibbonControlSpec control : group.controls()) {
                switch (control) {
                    case RibbonButtonSpec button -> commands.add(button.command());
                    case RibbonToggleSpec toggle -> commands.add(toggle.command());
                    case RibbonSplitButtonSpec splitButton -> {
                        commands.add(splitButton.primaryCommand());
                        commands.addAll(splitButton.secondaryCommands());
                    }
                    case RibbonMenuSpec menu -> commands.addAll(menu.items());
                    default -> {
                    }
                }
            }
        }
        return commands;
    }

    private record LegacyCase(String label, RibbonContext context) {
    }

    private static final class StubActions extends StackPane implements HugoRibbonActions {

        private int toggleCount;
        private int newContentCount;
        private int buildCount;
        private int modCount;
        private int envCount;
        private int frontMatterCount;
        private int shortcodeCount;

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
            toggleCount++;
        }

        @Override
        public void newContent(String relativePath) {
            newContentCount++;
        }

        @Override
        public void build() {
            buildCount++;
        }

        @Override
        public void mod(String subCommand) {
            modCount++;
        }

        @Override
        public void env() {
            envCount++;
        }

        @Override
        public void frontMatterTemplate() {
            frontMatterCount++;
        }

        @Override
        public void insertShortcode(String shortcodeName) {
            shortcodeCount++;
        }
    }
}
