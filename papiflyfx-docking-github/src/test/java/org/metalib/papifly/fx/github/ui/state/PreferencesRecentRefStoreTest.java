package org.metalib.papifly.fx.github.ui.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.github.model.GitRefKind;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferencesRecentRefStoreTest {

    @Test
    void recordsNewestFirstAndDeDuplicates(@TempDir Path tempDir) {
        Preferences preferences = Preferences.userRoot().node("papiflyfx-test-" + tempDir.getFileName());
        PreferencesRecentRefStore store = new PreferencesRecentRefStore(preferences, 5);
        URI remoteUrl = URI.create("https://github.com/org/repo");

        store.record(remoteUrl, new RecentRefStore.Entry("refs/heads/main", "main", GitRefKind.LOCAL_BRANCH));
        store.record(remoteUrl, new RecentRefStore.Entry("refs/tags/v1.0.0", "v1.0.0", GitRefKind.TAG));
        store.record(remoteUrl, new RecentRefStore.Entry("refs/heads/main", "main", GitRefKind.LOCAL_BRANCH));

        List<RecentRefStore.Entry> entries = store.load(remoteUrl);

        assertEquals(2, entries.size());
        assertEquals("refs/heads/main", entries.getFirst().fullRefName());
        assertEquals("refs/tags/v1.0.0", entries.get(1).fullRefName());
    }
}
