package org.metalib.papifly.fx.github.model;

import java.net.URI;
import java.util.Objects;

public record PullRequestResult(
    int number,
    URI url
) {

    public PullRequestResult {
        Objects.requireNonNull(url, "url");
    }
}
