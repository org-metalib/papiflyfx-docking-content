# Tree search in `papiflyfx-docking-tree` (review3)

## What it takes

### 1) Input triggers
- `Cmd+F`/`Ctrl+F`: handle in `TreeView.installHandlers()` from `setOnKeyPressed`.
- Type-to-search: add `setOnKeyTyped` and accumulate printable chars (no ctrl/meta/alt).
- Use a short debounce reset (for example `PauseTransition` ~700ms) so typed characters form one query sequence.

### 2) Search UI (similar to code editor)
- `CodeEditor` uses an overlay controller (`SearchController`) anchored top-right, with explicit `open()/close()` lifecycle.
- Tree can follow the same interaction model: compact overlay, query field, next/prev, ESC closes.
- Do **not** directly reuse `code.search.SearchController` in place: it is tightly coupled to `Document`/`SearchModel`.

### 3) Code sharing opportunity
- Extract a small shared overlay base (layout, open/close, theming hooks, icon buttons) and keep search logic module-specific:
  - `code` keeps `Document` offset search.
  - `tree` adds DFS over `TreeItem<T>`.
- `SearchIcons` is also a good extraction candidate into a shared UI package.

### 4) Tree-specific search behavior
- Traverse with **depth-first pre-order**.
- Match rule: case-insensitive partial text match.
- On match:
  1. expand all parents,
  2. select/focus in `TreeSelectionModel`,
  3. reveal item using `TreeViewport.ensureItemVisible(...)`,
  4. mark viewport dirty.

---

## Java code for the search method

Below is a `TreeView<T>`-compatible method (for this repository’s custom tree component, not `javafx.scene.control.TreeView`):

```java
import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public TreeItem<T> searchAndRevealFirst(String query, Function<T, String> textExtractor) {
    String normalizedQuery = normalizeQuery(query);
    if (normalizedQuery.isEmpty()) {
        return null;
    }

    TreeItem<T> rootItem = getRoot();
    if (rootItem == null) {
        return null;
    }

    TreeItem<T> match = depthFirstMatch(rootItem, normalizedQuery, textExtractor);
    if (match == null) {
        return null;
    }

    expandParents(match);
    selectionModel.selectOnly(match);
    selectionModel.setFocusedItem(match);
    viewport.ensureItemVisible(match);
    viewport.markDirty();
    return match;
}

private TreeItem<T> depthFirstMatch(
    TreeItem<T> rootItem,
    String normalizedQuery,
    Function<T, String> textExtractor
) {
    Deque<TreeItem<T>> stack = new ArrayDeque<>();
    stack.push(rootItem);

    while (!stack.isEmpty()) {
        TreeItem<T> current = stack.pop();
        if (matches(current, normalizedQuery, textExtractor)) {
            return current;
        }

        List<TreeItem<T>> children = current.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            stack.push(children.get(i));
        }
    }
    return null;
}

private boolean matches(TreeItem<T> item, String normalizedQuery, Function<T, String> textExtractor) {
    T value = item.getValue();
    String text = textExtractor == null ? String.valueOf(value) : textExtractor.apply(value);
    String normalizedText = text == null ? "" : text.toLowerCase(Locale.ROOT);
    return normalizedText.contains(normalizedQuery);
}

private void expandParents(TreeItem<T> item) {
    TreeItem<T> parent = item.getParent();
    while (parent != null) {
        expansionModel.setExpanded(parent, true);
        parent = parent.getParent();
    }
}

private static String normalizeQuery(String query) {
    if (query == null) {
        return "";
    }
    return query.trim().toLowerCase(Locale.ROOT);
}
```

## Integration note
- If you later need standard JavaFX `TreeView`, the same DFS + expand-parent logic applies, but reveal step becomes `treeView.scrollTo(rowIndex)`.
