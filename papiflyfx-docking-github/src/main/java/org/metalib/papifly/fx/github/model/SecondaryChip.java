package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record SecondaryChip(
    String text,
    Variant variant
) {

    public SecondaryChip {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(variant, "variant");
    }

    public enum Variant {
        ACCENT,
        SUCCESS,
        WARNING,
        DANGER,
        MUTED
    }
}
