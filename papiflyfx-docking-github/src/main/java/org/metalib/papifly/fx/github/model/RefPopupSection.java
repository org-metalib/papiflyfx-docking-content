package org.metalib.papifly.fx.github.model;

import java.util.List;
import java.util.Objects;

public record RefPopupSection(
    String title,
    List<RefPopupEntry> entries
) {

    public RefPopupSection {
        Objects.requireNonNull(title, "title");
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}
