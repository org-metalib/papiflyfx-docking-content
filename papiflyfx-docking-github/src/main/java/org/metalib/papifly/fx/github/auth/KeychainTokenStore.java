package org.metalib.papifly.fx.github.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class KeychainTokenStore implements CredentialStore {

    private final String serviceName;
    private final String accountName;
    private final CredentialStore fallbackStore;

    public KeychainTokenStore(String serviceName, String accountName) {
        this(serviceName, accountName, new PreferencesTokenStore());
    }

    public KeychainTokenStore(String serviceName, String accountName, CredentialStore fallbackStore) {
        this.serviceName = serviceName;
        this.accountName = accountName;
        this.fallbackStore = fallbackStore;
    }

    @Override
    public Optional<String> getToken() {
        if (!isMacOs()) {
            return fallbackStore.getToken();
        }
        try {
            Process process = new ProcessBuilder(
                "security",
                "find-generic-password",
                "-s", serviceName,
                "-a", accountName,
                "-w"
            ).start();
            int code = process.waitFor();
            if (code != 0) {
                return fallbackStore.getToken();
            }
            String token = readOutput(process).trim();
            if (token.isBlank()) {
                return fallbackStore.getToken();
            }
            return Optional.of(token);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return fallbackStore.getToken();
        }
    }

    @Override
    public void setToken(String token) {
        if (token == null || token.isBlank()) {
            clearToken();
            return;
        }
        fallbackStore.setToken(token);
        if (!isMacOs()) {
            return;
        }
        runCommand(
            "security",
            "add-generic-password",
            "-U",
            "-s", serviceName,
            "-a", accountName,
            "-w", token.trim()
        );
    }

    @Override
    public void clearToken() {
        fallbackStore.clearToken();
        if (!isMacOs()) {
            return;
        }
        runCommand(
            "security",
            "delete-generic-password",
            "-s", serviceName,
            "-a", accountName
        );
    }

    private static boolean isMacOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac");
    }

    private static String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private static void runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            process.waitFor();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
