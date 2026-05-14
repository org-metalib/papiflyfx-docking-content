package org.metalib.papifly.fx.github.model;

import java.util.Objects;

public record BranchRef(
    String name,
    String fullName,
    boolean local,
    boolean remote,
    boolean current,
    String remoteName,
    String trackingTarget
) implements Comparable<BranchRef> {

    public BranchRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fullName, "fullName");
        remoteName = remoteName == null ? "" : remoteName;
        trackingTarget = trackingTarget == null ? "" : trackingTarget;
    }

    public BranchRef(
        String name,
        String fullName,
        boolean local,
        boolean remote,
        boolean current
    ) {
        this(name, fullName, local, remote, current, deriveRemoteName(fullName), "");
    }

    @Override
    public int compareTo(BranchRef other) {
        int localOrder = Boolean.compare(other.local, local);
        if (localOrder != 0) {
            return localOrder;
        }
        int currentOrder = Boolean.compare(other.current, current);
        if (currentOrder != 0) {
            return currentOrder;
        }
        return name.compareToIgnoreCase(other.name);
    }

    private static String deriveRemoteName(String fullName) {
        String prefix = "refs/remotes/";
        if (!fullName.startsWith(prefix)) {
            return "";
        }
        String value = fullName.substring(prefix.length());
        int slashIndex = value.indexOf('/');
        if (slashIndex < 0) {
            return value;
        }
        return value.substring(0, slashIndex);
    }
}
