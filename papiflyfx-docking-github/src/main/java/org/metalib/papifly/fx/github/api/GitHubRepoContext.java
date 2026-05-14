package org.metalib.papifly.fx.github.api;

import org.metalib.papifly.fx.github.github.RemoteUrlParser;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

public record GitHubRepoContext(
    URI remoteUrl,
    Path localClonePath,
    String owner,
    String repo
) {

    public GitHubRepoContext {
        Objects.requireNonNull(remoteUrl, "remoteUrl");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(repo, "repo");
    }

    public static GitHubRepoContext of(URI remoteUrl, Path localClonePath) {
        RemoteUrlParser.RemoteCoordinates coordinates = RemoteUrlParser.parse(remoteUrl.toString());
        return new GitHubRepoContext(remoteUrl, localClonePath, coordinates.owner(), coordinates.repo());
    }

    public static GitHubRepoContext remoteOnly(URI remoteUrl) {
        return of(remoteUrl, null);
    }

    public boolean hasLocalClone() {
        return localClonePath != null;
    }
}
