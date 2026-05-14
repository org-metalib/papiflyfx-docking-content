package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record TagRef(
    String name,
    String fullName,
    boolean current
) implements Comparable<TagRef> {

    public TagRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fullName, "fullName");
    }

    @Override
    public int compareTo(TagRef other) {
        int currentOrder = Boolean.compare(other.current, current);
        if (currentOrder != 0) {
            return currentOrder;
        }
        return name.compareToIgnoreCase(other.name);
    }
}
