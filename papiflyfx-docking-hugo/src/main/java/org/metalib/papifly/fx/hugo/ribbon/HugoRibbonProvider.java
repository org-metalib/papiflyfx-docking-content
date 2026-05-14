package org.metalib.papifly.fx.hugo.ribbon;

import org.metalib.papifly.fx.api.ribbon.RibbonBooleanState;
import org.metalib.papifly.fx.api.ribbon.RibbonAttributeKey;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonContextAttributes;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonIconHandle;
import org.metalib.papifly.fx.api.ribbon.RibbonMenuSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonSplitButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonToggleSpec;
import org.metalib.papifly.fx.hugo.api.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.api.HugoRibbonActions;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Ribbon contribution for Hugo preview and authoring workflows.
 */
public final class HugoRibbonProvider implements RibbonProvider {

    /**
     * Collapse-order constants for Hugo groups. Higher values stay visible
     * longer under adaptive layout (see {@code RibbonGroupSpec#collapseOrder}).
     */
    private static final int COLLAPSE_LATE = 30;
    private static final int COLLAPSE_MID = 20;
    private static final int COLLAPSE_EARLY = 10;
    private static final String HUGO_DOMAIN = "hugo";

    @Override
    public String id() {
        return "hugo.ribbon.provider";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public List<RibbonTabSpec> getTabs(RibbonContext context) {
        Optional<HugoRibbonActions> actions = resolveActions(context);
        return List.of(
            new RibbonTabSpec(
                "hugo",
                "Hugo",
                50,
                false,
                ribbonContext -> true,
                List.of(
                    developmentGroup(actions),
                    newContentGroup(actions),
                    buildGroup(actions),
                    modulesGroup(actions),
                    environmentGroup(actions)
                )
            ),
            new RibbonTabSpec(
                "hugo-editor",
                "Hugo Editor",
                90,
                true,
                this::isHugoEditorContext,
                List.of(
                    editorFrontMatterGroup(actions),
                    editorShortcodesGroup(actions)
                )
            )
        );
    }

    private boolean isHugoEditorContext(RibbonContext context) {
        if (context == null) {
            return false;
        }
        Optional<String> explicitDomain = normalizedAttribute(context, RibbonContextAttributes.CONTENT_DOMAIN_KEY);
        Optional<String> explicitKind = normalizedAttribute(context, RibbonContextAttributes.CONTENT_KIND_KEY);
        if (explicitDomain.isPresent()) {
            return HUGO_DOMAIN.equals(explicitDomain.orElse(""));
        }
        if (explicitKind.isPresent()) {
            String kind = explicitKind.orElse("");
            return kind.contains("hugo") || kind.contains("markdown");
        }
        String typeKey = context.activeContentTypeKey();
        if (HugoPreviewFactory.FACTORY_ID.equals(typeKey)) {
            return true;
        }
        String contentFactoryId = context.attribute(RibbonContextAttributes.CONTENT_FACTORY_ID, String.class).orElse("");
        if (HugoPreviewFactory.FACTORY_ID.equals(contentFactoryId)) {
            return true;
        }
        String contentId = context.activeContentIdOptional().orElse("").toLowerCase(Locale.ROOT);
        String dockTitle = context.attribute(RibbonContextAttributes.DOCK_TITLE, String.class).orElse("").toLowerCase(Locale.ROOT);
        return contentId.endsWith(".md")
            || contentId.endsWith(".markdown")
            || dockTitle.endsWith(".md")
            || dockTitle.endsWith(".markdown")
            || contentId.contains("/content/")
            || dockTitle.contains("/content/")
            || typeKeyContainsHugoContext(typeKey);
    }

    private static Optional<String> normalizedAttribute(
        RibbonContext context,
        RibbonAttributeKey<String> key
    ) {
        return context.attribute(key)
            .map(value -> value.toLowerCase(Locale.ROOT).trim())
            .filter(value -> !value.isBlank());
    }

    private static boolean typeKeyContainsHugoContext(String typeKey) {
        if (typeKey == null) {
            return false;
        }
        String normalized = typeKey.toLowerCase(Locale.ROOT);
        return normalized.contains("markdown") || normalized.contains("hugo");
    }

    private static RibbonGroupSpec developmentGroup(Optional<HugoRibbonActions> actions) {
        RibbonToggleCommand toggle = toggleCommand(
            "development.server",
            "Server",
            "Start or stop hugo server",
            "play",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            HugoRibbonActions::toggleServer,
            actions.map(HugoRibbonActions::isServerRunning).orElse(false)
        );
        return new RibbonGroupSpec(
            "hugo-development",
            "Development",
            0,
            COLLAPSE_LATE,
            null,
            List.of(new RibbonToggleSpec(toggle))
        );
    }

    private static RibbonGroupSpec newContentGroup(Optional<HugoRibbonActions> actions) {
        RibbonCommand primary = command(
            "new-content.post",
            "New Post",
            "Create a new Hugo post",
            "file-add",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.newContent("content/posts/ribbon-post.md")
        );
        RibbonCommand page = command(
            "new-content.page",
            "New Page",
            "Create a new Hugo page",
            "file",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.newContent("content/page/ribbon-page.md")
        );
        RibbonCommand draft = command(
            "new-content.draft",
            "New Draft",
            "Create a new Hugo draft",
            "pencil",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.newContent("content/drafts/ribbon-draft.md")
        );
        return new RibbonGroupSpec(
            "hugo-new-content",
            "New Content",
            10,
            COLLAPSE_LATE,
            null,
            List.of(new RibbonSplitButtonSpec(primary, List.of(page, draft)))
        );
    }

    private static RibbonGroupSpec buildGroup(Optional<HugoRibbonActions> actions) {
        return new RibbonGroupSpec(
            "hugo-build",
            "Build",
            20,
            COLLAPSE_LATE,
            null,
            List.of(new RibbonButtonSpec(command(
                "build.site",
                "Build",
                "Run hugo --gc --minify",
                "package",
                actions,
                HugoRibbonActions::canRunHugoCommands,
                HugoRibbonActions::build
            )))
        );
    }

    private static RibbonGroupSpec modulesGroup(Optional<HugoRibbonActions> actions) {
        RibbonCommand tidy = command(
            "modules.tidy",
            "Mod Tidy",
            "Run hugo mod tidy",
            "package-dependencies",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.mod("tidy")
        );
        RibbonCommand get = command(
            "modules.get",
            "Mod Get",
            "Run hugo mod get",
            "download",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.mod("get")
        );
        RibbonCommand vendor = command(
            "modules.vendor",
            "Mod Vendor",
            "Run hugo mod vendor",
            "archive",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.mod("vendor")
        );
        return new RibbonGroupSpec(
            "hugo-modules",
            "Modules",
            30,
            COLLAPSE_MID,
            null,
            List.of(new RibbonMenuSpec(
                "hugo-mod-menu",
                "Hugo Mod",
                "Manage Hugo modules",
                icon("package"),
                icon("package"),
                List.of(tidy, get, vendor)
            ))
        );
    }

    private static RibbonGroupSpec environmentGroup(Optional<HugoRibbonActions> actions) {
        return new RibbonGroupSpec(
            "hugo-environment",
            "Environment",
            40,
            COLLAPSE_EARLY,
            null,
            List.of(new RibbonButtonSpec(command(
                "environment.env",
                "Env",
                "Run hugo env",
                "terminal",
                actions,
                HugoRibbonActions::canRunHugoCommands,
                HugoRibbonActions::env
            )))
        );
    }

    private static RibbonGroupSpec editorFrontMatterGroup(Optional<HugoRibbonActions> actions) {
        return new RibbonGroupSpec(
            "hugo-editor-front-matter",
            "Front Matter",
            0,
            COLLAPSE_MID,
            null,
            List.of(new RibbonButtonSpec(command(
                "editor.front-matter",
                "Template",
                "Apply front matter template helpers",
                "code",
                actions,
                HugoRibbonActions::canRunHugoCommands,
                HugoRibbonActions::frontMatterTemplate
            )))
        );
    }

    private static RibbonGroupSpec editorShortcodesGroup(Optional<HugoRibbonActions> actions) {
        RibbonCommand youtube = command(
            "editor.shortcode.youtube",
            "YouTube",
            "Insert youtube shortcode helper",
            "video",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.insertShortcode("youtube")
        );
        RibbonCommand gallery = command(
            "editor.shortcode.gallery",
            "Gallery",
            "Insert gallery shortcode helper",
            "image",
            actions,
            HugoRibbonActions::canRunHugoCommands,
            ribbonActions -> ribbonActions.insertShortcode("gallery")
        );
        return new RibbonGroupSpec(
            "hugo-editor-shortcodes",
            "Shortcodes",
            10,
            COLLAPSE_EARLY,
            null,
            List.of(new RibbonMenuSpec(
                "hugo-editor-shortcodes-menu",
                "Insert",
                "Insert Hugo shortcode helpers",
                icon("code-square"),
                icon("code-square"),
                List.of(youtube, gallery)
            ))
        );
    }

    private static RibbonCommand command(
        String id,
        String label,
        String tooltip,
        String octiconName,
        Optional<HugoRibbonActions> actions,
        java.util.function.Predicate<HugoRibbonActions> canRun,
        java.util.function.Consumer<HugoRibbonActions> run
    ) {
        boolean enabled = actions.map(canRun::test).orElse(false);
        return RibbonCommand.of(
            "hugo.ribbon." + id,
            label,
            tooltip,
            icon(octiconName),
            icon(octiconName),
            RibbonBooleanState.mutable(enabled),
            () -> actions.ifPresent(run)
        );
    }

    private static RibbonToggleCommand toggleCommand(
        String id,
        String label,
        String tooltip,
        String octiconName,
        Optional<HugoRibbonActions> actions,
        java.util.function.Predicate<HugoRibbonActions> canRun,
        java.util.function.Consumer<HugoRibbonActions> run,
        boolean selected
    ) {
        boolean enabled = actions.map(canRun::test).orElse(false);
        return RibbonToggleCommand.of(
            "hugo.ribbon." + id,
            label,
            tooltip,
            icon(octiconName),
            icon(octiconName),
            RibbonBooleanState.mutable(enabled),
            RibbonBooleanState.mutable(selected),
            () -> actions.ifPresent(run)
        );
    }

    private static RibbonIconHandle icon(String name) {
        return RibbonIconHandle.of("octicon:" + name);
    }

    private static Optional<HugoRibbonActions> resolveActions(RibbonContext context) {
        if (context == null) {
            return Optional.empty();
        }
        return context.capability(HugoRibbonActions.class);
    }
}
