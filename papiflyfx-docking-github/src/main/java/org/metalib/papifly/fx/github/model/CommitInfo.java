package org.metalib.papifly.fx.github.model;

import java.time.Instant;
import java.util.Objects;

public record CommitInfo(
    String hash,
    String shortHash,
    String message,
    String author,
    Instant timestamp
) {

    public CommitInfo {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(shortHash, "shortHash");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(timestamp, "timestamp");
    }
}
