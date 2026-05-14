package org.metalib.papifly.fx.github.auth;

import org.metalib.papifly.fx.settings.api.SecretStore;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PatCredentialStore implements CredentialStore {

    private final AtomicReference<String> token = new AtomicReference<>("");
    private final CredentialStore delegate;

    public PatCredentialStore() {
        SecretStore secretStore = locateSecretStore();
        this.delegate = secretStore == null ? null : new SecretStoreCredentialAdapter(secretStore);
    }

    @Override
    public Optional<String> getToken() {
        if (delegate != null) {
            return delegate.getToken();
        }
        String current = token.get();
        if (current == null || current.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(current);
    }

    @Override
    public void setToken(String token) {
        if (delegate != null) {
            delegate.setToken(token);
            return;
        }
        this.token.set(token == null ? "" : token.trim());
    }

    @Override
    public void clearToken() {
        if (delegate != null) {
            delegate.clearToken();
            return;
        }
        token.set("");
    }

    private SecretStore locateSecretStore() {
        try {
            Class<?> runtimeClass = Class.forName("org.metalib.papifly.fx.settings.runtime.SettingsRuntime");
            Object value = runtimeClass.getMethod("defaultSecretStore").invoke(null);
            if (value instanceof SecretStore secretStore) {
                return secretStore;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }
}
