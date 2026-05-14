# Implementation plan: tree search (review3)

## 1) Goal

Implement the search behavior described in `spec/papiflyfx-docking-tree/review3-search/research.md` for `papiflyfx-docking-tree`:

- Trigger search from **Cmd/Ctrl+F**.
- Trigger search by **typing while the tree is focused**.
- Show a search dialog similar to the code editor overlay.
- Use **depth-first search** with **case-insensitive partial match**.
- On match: select item, expand ancestors, reveal in viewport.

This plan is implementation-oriented and intentionally detailed so it can be executed step-by-step with low risk.

## Implementation status

- [x] Phase A — DFS engine and reveal core
- [x] Phase B — Search session and navigation model
- [x] Phase C — Overlay UI for tree search
- [x] Phase D — Keyboard triggers (Cmd/Ctrl+F + typing)
- [x] Phase E — API additions in `TreeView<T>`
- [x] Phase F — Code sharing opportunity
- [x] Phase G — Tests
- [x] Phase H — Validation
- [x] Phase I — Documentation updates

---

## 2) Current baseline (what exists today)

Relevant tree internals:

- `TreeView<T>` wires keyboard and pointer handlers (`installHandlers()`).
- `TreeInputController<T>` handles navigation/edit keys via `handleKeyPressed(...)`.
- `TreeSelectionModel<T>` supports `selectOnly(...)` and focused item handling.
- `TreeExpansionModel<T>` supports `setExpanded(item, true/false)`.
- `TreeViewport<T>` supports reveal via `ensureItemVisible(item)` and redraw via `markDirty()`.
- `TreeEditController<T>` overlays a `TextField` for rename/edit mode.

Relevant code-editor search reference:

- `papiflyfx-docking-code/.../search/SearchController.java` already implements a robust overlay lifecycle:
  - `open(...)`, `close()`, `isOpen()`
  - search field + next/prev + close
  - theme-aware styling

---

## 3) Scope and boundaries

### In scope (for this feature)

1. Tree-local search UI overlay and keyboard triggers.
2. DFS search engine for `TreeItem<T>`.
3. Match navigation (at least first match and next/prev).
4. Match reveal workflow (expand/select/focus/scroll).
5. Tests for keyboard triggers and reveal behavior.

### Out of scope (deferred)

1. Replace mode for tree nodes.
2. Highlighting matched text inside rendered rows.
3. Global cross-component search.
4. Full refactor of code-editor search module (only minimal shared extraction if low risk).

---

## 4) Proposed architecture

### 4.1 New tree search package

Create package:

`papiflyfx-docking-tree/src/main/java/org/metalib/papifly/fx/tree/search`

Recommended classes:

1. `TreeSearchEngine<T>`
   - Pure traversal/matching logic (no JavaFX Node dependencies).
   - Produces ordered DFS results and supports next/previous navigation.

2. `TreeSearchSession<T>`
   - Holds current query, result list, current index.
   - Recomputes results when query changes.

3. `TreeSearchOverlay`
   - Search UI panel (query field, next/prev, close, result label).
   - Callback-based API to avoid coupling to `TreeView<T>`.

### 4.2 Integration point

Integrate at `TreeView<T>` (best place because it already orchestrates controllers, viewport, focus, and disposal):

- Add search fields (`engine/session/overlay`).
- Add key routing for Cmd/Ctrl+F and KEY_TYPED sequence.
- Add reveal method used by overlay callbacks.
- Add disposal cleanup for overlay handlers/timers.

### 4.3 Why not embed in `TreeInputController<T>`?

`TreeInputController` currently focuses on navigation and edit commands. Search overlay concerns UI lifecycle and view composition; that makes `TreeView<T>` the cleaner orchestration boundary, while `TreeInputController` remains navigation-only.

---

## 5) Detailed implementation steps

## Phase A — DFS engine and reveal core

### A.1 Add engine class (`TreeSearchEngine<T>`)

Responsibilities:

- Normalize query (trim + lower-case).
- DFS pre-order traversal.
- Case-insensitive `contains(...)` matching.
- Provide ordered matches for next/prev behavior.

Code sketch:

```java
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
        String normalized = normalize(query);
        if (root == null || normalized.isEmpty()) {
            return List.of();
        }
        List<TreeItem<T>> matches = new ArrayList<>();
        Deque<TreeItem<T>> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeItem<T> current = stack.pop();
            if ((includeRoot || current != root) && matches(current, normalized, textExtractor)) {
                matches.add(current);
            }
            List<TreeItem<T>> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return matches;
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
```

### A.2 Add reveal helper in `TreeView<T>`

Code sketch:

```java
private void revealMatch(TreeItem<T> match) {
    if (match == null) {
        return;
    }
    TreeItem<T> parent = match.getParent();
    while (parent != null) {
        expansionModel.setExpanded(parent, true);
        parent = parent.getParent();
    }
    selectionModel.selectOnly(match);
    selectionModel.setFocusedItem(match);
    viewport.ensureItemVisible(match);
    viewport.markDirty();
}
```

This directly fulfills the required select + expand + scroll behavior.

---

## Phase B — Search session and navigation model

### B.1 Add session state class (`TreeSearchSession<T>`)

Responsibilities:

- Store current query.
- Store current DFS-ordered match list.
- Track active match index.
- Expose `setQuery(...)`, `next()`, `previous()`, `current()`, `count()`.

Code sketch:

```java
public final class TreeSearchSession<T> {
    private final TreeSearchEngine<T> engine;
    private final Supplier<TreeItem<T>> rootSupplier;
    private final Function<T, String> textExtractor;
    private final boolean includeRoot;

    private String query = "";
    private List<TreeItem<T>> matches = List.of();
    private int index = -1;

    // ctor omitted for brevity

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        this.matches = engine.findAll(rootSupplier.get(), this.query, textExtractor, includeRoot);
        this.index = matches.isEmpty() ? -1 : 0;
    }

    public TreeItem<T> current() {
        return (index < 0 || index >= matches.size()) ? null : matches.get(index);
    }

    public TreeItem<T> next() {
        if (matches.isEmpty()) return null;
        index = (index + 1) % matches.size();
        return matches.get(index);
    }

    public TreeItem<T> previous() {
        if (matches.isEmpty()) return null;
        index = (index - 1 + matches.size()) % matches.size();
        return matches.get(index);
    }

    public int count() { return matches.size(); }
    public int activeIndex() { return index; }
}
```

---

## Phase C — Overlay UI for tree search

### C.1 Add `TreeSearchOverlay` (compact, search-only)

Minimum controls:

- Query field (`TextField`)
- Previous button
- Next button
- Close button
- Match count label (`"3 of 12"` / `"No results"`)

Overlay API:

- `open(String initialQuery)`
- `close()`
- `isOpen()`
- callbacks:
  - `onQueryChanged(String)`
  - `onNext()`
  - `onPrevious()`
  - `onClose()`

Code sketch:

```java
public final class TreeSearchOverlay extends VBox {
    private final TextField queryField = new TextField();
    private final Label resultLabel = new Label();
    private Runnable onNext = () -> {};
    private Runnable onPrevious = () -> {};
    private Consumer<String> onQueryChanged = s -> {};
    private Runnable onClose = () -> {};

    public TreeSearchOverlay() {
        setManaged(false);
        setVisible(false);
        queryField.textProperty().addListener((obs, oldV, newV) -> onQueryChanged.accept(newV));
        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) onPrevious.run(); else onNext.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });
        // build HBox row with buttons + label
    }

    public void open(String initialQuery) { /* set text, show, focus/select */ }
    public void close() { /* hide + callback */ }
}
```

### C.2 Integrate overlay into `TreeView<T>`

In constructor, add overlay as top-right stacked child:

```java
StackPane.setAlignment(searchOverlay, Pos.TOP_RIGHT);
StackPane.setMargin(searchOverlay, new Insets(8));
getChildren().addAll(viewport, dragDropController.overlayCanvas(), editController.editorNode(), searchOverlay);
```

Wire callbacks:

```java
searchOverlay.setOnQueryChanged(query -> {
    searchSession.setQuery(query);
    searchOverlay.updateCount(searchSession.activeIndex(), searchSession.count());
    revealMatch(searchSession.current());
});
searchOverlay.setOnNext(() -> revealMatch(searchSession.next()));
searchOverlay.setOnPrevious(() -> revealMatch(searchSession.previous()));
searchOverlay.setOnClose(this::requestFocus);
```

---

## Phase D — Keyboard triggers (Cmd/Ctrl+F + typing)

### D.1 Key pressed handling for Cmd/Ctrl+F

Update `TreeView.installHandlers()`:

```java
private void installHandlers() {
    setOnKeyPressed(event -> {
        if (isOpenSearchShortcut(event)) {
            openSearchOverlayWithSelectionSeed();
            event.consume();
            return;
        }
        inputController.handleKeyPressed(event);
    });
    setOnKeyTyped(this::handleSearchKeyTyped);
    // existing pointer handlers unchanged
}

private boolean isOpenSearchShortcut(KeyEvent event) {
    return event.getCode() == KeyCode.F
        && (event.isMetaDown() || event.isControlDown())
        && !event.isAltDown();
}
```

### D.2 Key typed handling for “start typing to search”

Rules:

1. Ignore if `editController.isEditing()`.
2. Ignore control/meta/alt combos.
3. Ignore non-printable characters.
4. Open overlay with typed char seed.

Code sketch:

```java
private void handleSearchKeyTyped(KeyEvent event) {
    if (event == null || event.isConsumed() || editController.isEditing()) {
        return;
    }
    String ch = event.getCharacter();
    if (ch == null || ch.isEmpty()) return;
    char c = ch.charAt(0);
    if (c < 32 || c == 127) return;
    if (event.isControlDown() || event.isMetaDown() || event.isAltDown()) return;

    if (!searchOverlay.isOpen()) {
        searchOverlay.open(ch);
        event.consume();
    }
}
```

Optional enhancement (if focus transfer races in some platforms): maintain a 500–700ms typed buffer using `PauseTransition` and pass the accumulated seed to `open(...)`.

---

## Phase E — API additions in `TreeView<T>`

Expose small optional API for host integration and tests:

- `public void openSearch()`
- `public void closeSearch()`
- `public boolean isSearchOpen()`
- `public TreeItem<T> searchAndRevealFirst(String query)` (or keep package-private if only internal)

Implementation sketch:

```java
public void openSearch() {
    TreeItem<T> focused = selectionModel.getFocusedItem();
    String seed = focused == null ? "" : String.valueOf(focused.getValue());
    searchOverlay.open(seed);
}
```

---

## Phase F — Code sharing opportunity (recommended staged approach)

## F.1 Stage 1 (low risk, immediate delivery)

Keep tree search implementation in tree module but mimic code editor UX patterns:

- same overlay lifecycle (`open/close/isOpen`)
- same Enter / Shift+Enter / Esc behavior
- similar icon usage and compact sizing

## F.2 Stage 2 (post-feature hardening)

Extract reusable overlay shell to `papiflyfx-docking-api` (shared dependency for both code and tree):

- `org.metalib.papifly.fx.searchui.SearchOverlayBase`
- `SearchIconPaths` (SVG constants + helper)
- optional shared stylesheet

Potential abstraction:

```java
public abstract class SearchOverlayBase extends VBox {
    protected final TextField queryField;
    protected final Label matchLabel;
    public abstract void open(String initialQuery);
    public abstract void close();
}
```

This reduces duplicated UI plumbing while preserving module-specific search engines.

---

## 6) Test plan

## Unit tests (new)

1. `TreeSearchEngineTest`
   - DFS order is pre-order.
   - Match is case-insensitive partial.
   - Empty/null query returns no results.
   - `includeRoot=false` excludes hidden root.

## FX tests (extend `TreeViewFxTest`)

1. `cmdFOpensSearchOverlay()`
2. `typingPrintableCharOpensSearchOverlayWithSeedQuery()`
3. `searchSelectsAndExpandsAndRevealsMatch()`
4. `searchNextAndPreviousCycleThroughMatches()`
5. `escapeClosesSearchOverlayAndReturnsFocusToTree()`

FX test sketch:

```java
@Test
void searchSelectsAndExpandsAndRevealsMatch() {
    runOnFx(() -> treeView.openSearch());
    runOnFx(() -> treeView.searchAndRevealFirst("target"));

    TreeItem<String> focused = callOnFx(() -> treeView.getSelectionModel().getFocusedItem());
    assertEquals("target-node", focused.getValue());
    assertTrue(callOnFx(() -> treeView.getExpansionModel().isExpanded(focused.getParent())));
}
```

---

## 7) Validation commands

Run only tree module tests during implementation iterations:

```bash
./mvnw -pl papiflyfx-docking-tree -am -Dtestfx.headless=true test
```

Run targeted tests while developing:

```bash
./mvnw -pl papiflyfx-docking-tree -am -Dtest=TreeSearchEngineTest,TreeViewFxTest -Dtestfx.headless=true test
```

---

## 8) Risks and mitigations

1. **Risk:** Key event conflicts with existing navigation/edit behavior.  
   **Mitigation:** only consume events for explicit search triggers; keep default path unchanged.

2. **Risk:** Hidden match when ancestors remain collapsed.  
   **Mitigation:** always expand ancestor chain before reveal.

3. **Risk:** show-root=false edge case when root matches.  
   **Mitigation:** default `includeRoot=false` for user search results.

4. **Risk:** Overlay focus traps key events unexpectedly.  
   **Mitigation:** explicit `Esc` close behavior + return focus to `TreeView`.

5. **Risk:** Visual mismatch with code editor overlay.  
   **Mitigation:** reuse icon sizing/layout semantics and optionally extract shared base in stage 2.

---

## 9) Delivery sequence (execution order)

1. Add `TreeSearchEngine<T>` + unit tests.
2. Add `TreeSearchSession<T>`.
3. Add `TreeSearchOverlay`.
4. Integrate into `TreeView<T>` (fields, handlers, reveal, disposal).
5. Add/adjust `TreeViewFxTest` scenarios.
6. Run tree module tests headless.
7. Optional: start stage-2 extraction for shared overlay base.

---

## 10) Progress and next steps

### Current progress

- Research completed in `research.md`.
- All implementation phases (A through I) are completed.
- Tree search is implemented with DFS matching, overlay UI, keyboard triggers, TreeView API wiring, shared search UI extraction, and passing tests.
- Post-implementation follow-up fix is completed: tree search overlay now remains a compact single-line control and uses corrected search theming consistent with the code editor overlay.

### Immediate next steps

1. No remaining implementation phases in this plan.
