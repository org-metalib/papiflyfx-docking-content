package org.metalib.papifly.fx.tree.search;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TreeSearchSession<T> {

    private final TreeSearchEngine<T> engine;
    private final Supplier<TreeItem<T>> rootSupplier;
    private final Supplier<Function<T, String>> textExtractorSupplier;
    private final BooleanSupplier includeRootSupplier;

    private String query = "";
    private List<TreeItem<T>> matches = List.of();
    private int currentMatchIndex = -1;

    public TreeSearchSession(
        TreeSearchEngine<T> engine,
        Supplier<TreeItem<T>> rootSupplier,
        Supplier<Function<T, String>> textExtractorSupplier,
        BooleanSupplier includeRootSupplier
    ) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.rootSupplier = Objects.requireNonNull(rootSupplier, "rootSupplier");
        this.textExtractorSupplier = Objects.requireNonNull(textExtractorSupplier, "textExtractorSupplier");
        this.includeRootSupplier = Objects.requireNonNull(includeRootSupplier, "includeRootSupplier");
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        recompute();
    }

    public String getQuery() {
        return query;
    }

    public void refresh() {
        recompute();
    }

    public void clear() {
        query = "";
        matches = List.of();
        currentMatchIndex = -1;
    }

    public int getMatchCount() {
        return matches.size();
    }

    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }

    public TreeItem<T> getCurrentMatch() {
        if (currentMatchIndex < 0 || currentMatchIndex >= matches.size()) {
            return null;
        }
        return matches.get(currentMatchIndex);
    }

    public TreeItem<T> next() {
        if (matches.isEmpty()) {
            return null;
        }
        if (currentMatchIndex < 0) {
            currentMatchIndex = 0;
        } else {
            currentMatchIndex = (currentMatchIndex + 1) % matches.size();
        }
        return matches.get(currentMatchIndex);
    }

    public TreeItem<T> previous() {
        if (matches.isEmpty()) {
            return null;
        }
        if (currentMatchIndex < 0) {
            currentMatchIndex = matches.size() - 1;
        } else {
            currentMatchIndex = (currentMatchIndex - 1 + matches.size()) % matches.size();
        }
        return matches.get(currentMatchIndex);
    }

    private void recompute() {
        matches = engine.findAll(
            rootSupplier.get(),
            query,
            textExtractorSupplier.get(),
            includeRootSupplier.getAsBoolean()
        );
        currentMatchIndex = matches.isEmpty() ? -1 : 0;
    }
}
