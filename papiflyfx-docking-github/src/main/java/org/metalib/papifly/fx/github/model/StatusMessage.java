package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record StatusMessage(
    Kind kind,
    String text
) {

    public static final StatusMessage IDLE = new StatusMessage(Kind.IDLE, "");

    public StatusMessage {
        Objects.requireNonNull(kind, "kind");
        text = text == null ? "" : text;
    }

    public enum Kind {
        IDLE,
        BUSY,
        SUCCESS,
        ERROR
    }
}
