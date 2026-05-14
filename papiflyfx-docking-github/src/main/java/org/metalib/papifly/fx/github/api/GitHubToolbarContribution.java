package org.metalib.papifly.fx.github.api;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.metalib.papifly.fx.github.auth.CredentialStore;
import org.metalib.papifly.fx.github.auth.PatCredentialStore;

import java.util.Objects;

public final class GitHubToolbarContribution {

    public enum Position {
        TOP,
        BOTTOM
    }

    private final GitHubToolbar toolbar;
    private final Position position;

    public GitHubToolbarContribution(GitHubRepoContext context, Position position) {
        this(new GitHubToolbar(context, new PatCredentialStore()), position);
    }

    public GitHubToolbarContribution(GitHubRepoContext context, CredentialStore credentialStore, Position position) {
        this(new GitHubToolbar(context, credentialStore), position);
    }

    public GitHubToolbarContribution(GitHubToolbar toolbar, Position position) {
        this.toolbar = Objects.requireNonNull(toolbar, "toolbar");
        this.position = Objects.requireNonNull(position, "position");
    }

    public Position position() {
        return position;
    }

    public Node toolbarNode() {
        return toolbar;
    }

    public GitHubToolbar toolbar() {
        return toolbar;
    }

    public void mount(BorderPane host) {
        if (position == Position.TOP) {
            host.setTop(toolbar);
        } else {
            host.setBottom(toolbar);
        }
    }
}
