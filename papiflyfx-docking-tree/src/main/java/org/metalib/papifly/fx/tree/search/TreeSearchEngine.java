package org.metalib.papifly.fx.tree.search;

import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class TreeSearchEngine<T> {

    public List<TreeItem<T>> findAll(TreeItem<T> root, String query, Function<T, String> textExtractor, boolean includeRoot) {
        String normalizedQuery = normalize(query);
        if (root == null || normalizedQuery.isEmpty()) {
            return List.of();
        }
        Deque<TreeItem<T>> stack = new ArrayDeque<>();
        List<TreeItem<T>> matches = new ArrayList<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeItem<T> current = stack.pop();
            if ((includeRoot || current != root) && matches(current, normalizedQuery, textExtractor)) {
                matches.add(current);
            }
            List<TreeItem<T>> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return List.copyOf(matches);
    }

    private boolean matches(TreeItem<T> item, String normalizedQuery, Function<T, String> textExtractor) {
        T value = item.getValue();
        String text = textExtractor == null ? String.valueOf(value) : textExtractor.apply(value);
        String normalizedText = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return normalizedText.contains(normalizedQuery);
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}
