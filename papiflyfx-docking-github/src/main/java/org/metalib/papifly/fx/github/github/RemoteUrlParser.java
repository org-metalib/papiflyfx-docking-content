package org.metalib.papifly.fx.github.github;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RemoteUrlParser {

    private static final Pattern HTTPS_PATTERN = Pattern.compile("^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern SSH_PATTERN = Pattern.compile("^ssh://git@github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern SCP_PATTERN = Pattern.compile("^git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?$");

    private RemoteUrlParser() {
    }

    public static RemoteCoordinates parse(String remoteUrl) {
        Objects.requireNonNull(remoteUrl, "remoteUrl");
        String value = remoteUrl.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Remote URL is empty");
        }

        Matcher https = HTTPS_PATTERN.matcher(value);
        if (https.matches()) {
            return new RemoteCoordinates(https.group(1), https.group(2));
        }

        Matcher ssh = SSH_PATTERN.matcher(value);
        if (ssh.matches()) {
            return new RemoteCoordinates(ssh.group(1), ssh.group(2));
        }

        Matcher scp = SCP_PATTERN.matcher(value);
        if (scp.matches()) {
            return new RemoteCoordinates(scp.group(1), scp.group(2));
        }

        throw new IllegalArgumentException("Unsupported GitHub remote URL: " + remoteUrl);
    }

    public record RemoteCoordinates(String owner, String repo) {
        public RemoteCoordinates {
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(repo, "repo");
            if (owner.isBlank()) {
                throw new IllegalArgumentException("owner must not be blank");
            }
            if (repo.isBlank()) {
                throw new IllegalArgumentException("repo must not be blank");
            }
        }
    }
}
