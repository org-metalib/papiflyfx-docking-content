package org.metalib.papifly.fx.github.model;

import java.util.List;
import java.util.Objects;

public sealed interface RefPopupEntry permits RefPopupEntry.Action, RefPopupEntry.Ref {

    String id();

    String primaryText();

    String secondaryText();

    boolean enabled();

    record Action(
        String id,
        String primaryText,
        String secondaryText,
        Command command,
        boolean enabled,
        String targetRefName,
        GitRefKind targetRefKind
    ) implements RefPopupEntry {

        public Action {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(primaryText, "primaryText");
            secondaryText = secondaryText == null ? "" : secondaryText;
            Objects.requireNonNull(command, "command");
            targetRefName = targetRefName == null ? "" : targetRefName;
        }

        public Action(
            String id,
            String primaryText,
            String secondaryText,
            Command command,
            boolean enabled
        ) {
            this(id, primaryText, secondaryText, command, enabled, "", null);
        }
    }

    record Ref(
        String id,
        String primaryText,
        String secondaryText,
        GitRefKind refKind,
        String fullRefName,
        boolean current,
        boolean enabled,
        List<Action> submenuEntries
    ) implements RefPopupEntry {

        public Ref {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(primaryText, "primaryText");
            secondaryText = secondaryText == null ? "" : secondaryText;
            Objects.requireNonNull(refKind, "refKind");
            Objects.requireNonNull(fullRefName, "fullRefName");
            submenuEntries = List.copyOf(submenuEntries == null ? List.of() : submenuEntries);
        }

        public boolean hasSubmenu() {
            return !submenuEntries.isEmpty();
        }
    }

    enum Command {
        UPDATE,
        COMMIT,
        PUSH,
        NEW_BRANCH,
        CHECKOUT_REVISION,
        ROLLBACK,
        TOKEN,
        CHECKOUT,
        CHECKOUT_AND_TRACK,
        OPEN_COMMIT,
        COMPARE_WITH_WORKING_TREE,
        SHOW_DIFF_WITH_WORKING_TREE,
        RENAME,
        DELETE_LOCAL_BRANCH
    }
}
