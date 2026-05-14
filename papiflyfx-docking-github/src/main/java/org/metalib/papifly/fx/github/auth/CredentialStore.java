package org.metalib.papifly.fx.github.auth;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.util.Optional;

public interface CredentialStore {

    Optional<String> getToken();

    void setToken(String token);

    void clearToken();

    default boolean isAuthenticated() {
        return getToken().filter(token -> !token.isBlank()).isPresent();
    }

    default CredentialsProvider toJGitCredentials() {
        return getToken()
            .filter(token -> !token.isBlank())
            .<CredentialsProvider>map(token -> new UsernamePasswordCredentialsProvider(token, ""))
            .orElse(null);
    }
}
