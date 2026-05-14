package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record CurrentRefState(
    String displayName,
    String fullName,
    GitRefKind kind,
    String trackingLabel,
    StatusDotState statusDotState,
    boolean defaultBranch,
    boolean detached,
    boolean remoteOnly
) {

    public CurrentRefState {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(statusDotState, "statusDotState");
        trackingLabel = trackingLabel == null ? "" : trackingLabel;
    }

    public enum StatusDotState {
        CLEAN,
        DIRTY,
        NEUTRAL
    }
}
