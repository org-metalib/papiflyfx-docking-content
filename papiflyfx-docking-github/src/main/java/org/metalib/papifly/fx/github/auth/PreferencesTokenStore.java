package org.metalib.papifly.fx.github.auth;

import java.util.Optional;
import java.util.prefs.Preferences;

public class PreferencesTokenStore implements CredentialStore {

    private static final String TOKEN_KEY = "github.token";

    private final Preferences preferences;

    public PreferencesTokenStore() {
        this(Preferences.userNodeForPackage(PreferencesTokenStore.class));
    }

    public PreferencesTokenStore(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public Optional<String> getToken() {
        String token = preferences.get(TOKEN_KEY, "");
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    @Override
    public void setToken(String token) {
        if (token == null || token.isBlank()) {
            preferences.remove(TOKEN_KEY);
            return;
        }
        preferences.put(TOKEN_KEY, token.trim());
    }

    @Override
    public void clearToken() {
        preferences.remove(TOKEN_KEY);
    }
}
