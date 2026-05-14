package org.metalib.papifly.fx.github.ui.state;

import org.metalib.papifly.fx.github.model.GitRefKind;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public interface RecentRefStore {

    List<Entry> load(URI remoteUrl);

    void record(URI remoteUrl, Entry entry);

    record Entry(
        String fullRefName,
        String displayName,
        GitRefKind kind
    ) {

        public Entry {
            Objects.requireNonNull(fullRefName, "fullRefName");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(kind, "kind");
        }
    }
}
