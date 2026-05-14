package org.metalib.papifly.fx.github.auth;

import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SecretStore;

import java.util.Optional;

public class SecretStoreCredentialAdapter implements CredentialStore {

    private final SecretStore secretStore;

    public SecretStoreCredentialAdapter(SecretStore secretStore) {
        this.secretStore = secretStore;
    }

    @Override
    public Optional<String> getToken() {
        return secretStore.getSecret(SecretKeyNames.githubPat());
    }

    @Override
    public void setToken(String token) {
        secretStore.setSecret(SecretKeyNames.githubPat(), token);
    }

    @Override
    public void clearToken() {
        secretStore.clearSecret(SecretKeyNames.githubPat());
    }
}
