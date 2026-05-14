# Plan: Tree search research (review3)

## Problem
Document what it takes to add search to `papiflyfx-docking-tree` with Cmd/Ctrl+F and type-to-search triggers,
code-sharing opportunities with the code editor search UI, and provide Java code for DFS search selection/reveal behavior.
                                                                                                                            
## Approach
1. Inspect current tree keyboard/input architecture and viewport APIs.
2. Inspect code editor search dialog architecture for reusable parts.
3. Draft a concrete DFS search method that:
   - matches case-insensitive partial text,
   - expands ancestor chain,
   - selects/focuses found item,
   - scrolls it into view.
4. Write findings and code into `spec/papiflyfx-docking-tree/review3-search/research.md`.

## Todos
- inspect-tree-search
- draft-dfs-method
- write-research-doc

## Notes
- Keep proposal aligned with existing custom `TreeView<T>` (not JavaFX `javafx.scene.control.TreeView`).
- Reuse code editor overlay shell pattern where feasible without coupling tree logic to document-based search model.