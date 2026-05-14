# Plan: Collapsible node info research

## Problem
Assess what code and API changes are needed in `papiflyfx-docking-tree` to support collapsible per-node info sections that can display rich content (HTML-like rich text, table, or card/form).

## Approach
1. Inspect tree rendering, layout, input, and model code to identify extension points and constraints.
2. Derive minimal and scalable implementation options for collapsible node info.
3. Write findings and recommendations into `spec/papiflyfx-docking-tree/review2-node-info/research.md`.

## Todos
- inspect-tree-module
- define-node-info-design
- write-research-doc

## Notes
Focus on smallest safe change set first, then list optional enhancements for richer content and performance.