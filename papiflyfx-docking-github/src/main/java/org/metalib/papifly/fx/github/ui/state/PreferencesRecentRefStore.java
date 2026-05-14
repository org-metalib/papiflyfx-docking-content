package org.metalib.papifly.fx.github.ui.state;

import org.metalib.papifly.fx.github.model.GitRefKind;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.prefs.Preferences;

public class PreferencesRecentRefStore implements RecentRefStore {

    private static final String KEY_PREFIX = "recent-ref-";

    private final Preferences preferences;
    private final int maxEntries;

    public PreferencesRecentRefStore() {
        this(Preferences.userNodeForPackage(PreferencesRecentRefStore.class), 5);
    }

    public PreferencesRecentRefStore(Preferences preferences) {
        this(preferences, 5);
    }

    public PreferencesRecentRefStore(Preferences preferences, int maxEntries) {
        this.preferences = preferences;
        this.maxEntries = Math.max(1, maxEntries);
    }

    @Override
    public List<Entry> load(URI remoteUrl) {
        String raw = preferences.get(key(remoteUrl), "");
        if (raw.isBlank()) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (String line : raw.split("\n")) {
            Entry entry = decode(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return List.copyOf(entries);
    }

    @Override
    public void record(URI remoteUrl, Entry entry) {
        List<Entry> updated = new ArrayList<>();
        updated.add(entry);
        for (Entry existing : load(remoteUrl)) {
            if (!existing.fullRefName().equals(entry.fullRefName())) {
                updated.add(existing);
            }
            if (updated.size() >= maxEntries) {
                break;
            }
        }
        List<String> encoded = updated.stream()
            .limit(maxEntries)
            .map(PreferencesRecentRefStore::encode)
            .toList();
        preferences.put(key(remoteUrl), String.join("\n", encoded));
    }

    private String key(URI remoteUrl) {
        String source = remoteUrl == null ? "" : remoteUrl.normalize().toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(bytes, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String encode(Entry entry) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return entry.kind().name()
            + "\t"
            + encoder.encodeToString(entry.fullRefName().getBytes(StandardCharsets.UTF_8))
            + "\t"
            + encoder.encodeToString(entry.displayName().getBytes(StandardCharsets.UTF_8));
    }

    private static Entry decode(String line) {
        String[] parts = line.split("\t", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            GitRefKind kind = GitRefKind.valueOf(parts[0]);
            String fullRefName = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8);
            String displayName = new String(decoder.decode(parts[2]), StandardCharsets.UTF_8);
            return new Entry(fullRefName, displayName, kind);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
