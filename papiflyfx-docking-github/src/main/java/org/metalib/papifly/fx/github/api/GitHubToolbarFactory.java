package org.metalib.papifly.fx.github.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

import java.net.URI;

public class GitHubToolbarFactory implements ContentFactory {

    private final GitHubRepoContext defaultContext;

    public GitHubToolbarFactory(GitHubRepoContext defaultContext) {
        this.defaultContext = defaultContext;
    }

    public GitHubToolbarFactory(URI remoteUrl) {
        this(GitHubRepoContext.remoteOnly(remoteUrl));
    }

    @Override
    public Node create(String factoryId) {
        if (!GitHubToolbar.FACTORY_ID.equals(factoryId)) {
            return null;
        }
        if (defaultContext == null) {
            return null;
        }
        return new GitHubToolbar(defaultContext);
    }
}
