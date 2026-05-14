# Tree Node Info Navigation

## Research

What it takes to implement a feature flag docking tree component that toggles focusing on a node info either with keys of mouse click~~~~~~~~~~~~

Put your findings to spec/papiflyfx-docking-tree/review2-node-info/navigation/research.md

## Planning

Write a very detailed `spec/papiflyfx-docking-tree/review2-node-info/navigation/plan.md` document outlining how to implement
 spec/papiflyfx-docking-tree/review2-node-info/navigation/research.md. Include code snippets.

 Do not include compatibility requirement.

## Implementation

Implement everything specified by `spec/papiflyfx-docking-tree/review2-node-info/navigation/plan.md`.
When you’re done with a task or phase, mark it as completed in the `spec/papiflyfx-docking-tree/review2-node-info/navigation/plan.md` document.
Add new `spec/papiflyfx-docking-tree/review2-node-info/navigation/progress.md` file to track your progress.
Do not stop until all tasks and phases are completed. Do not add unnecessary comments or javadocs, do not use any or unknown types.
Continuously run typecheck to make sure you’re not introducing new issues.

## SamplesApp

Update SamplesApp with this feature.

## Current Status (2026-03-02)

- Implemented feature-flagged node-info navigation in `papiflyfx-docking-tree`.
- Implemented SamplesApp integration in `TreeViewNodeInfoSample`:
  - `Info mode` selector
  - `Toggle mode` selector
  - `Mouse focus` selector
- Applied rendering fix so inline node-info rows use border-only highlight when their owner item is selected.
- Applied inline node-info indentation so info content aligns with tree depth.
- Applied connector-line refinement so the last child connector does not render as continuation.

## Validation Snapshot

- Tree compile: passed
- Tree focused FX tests: passed
- Samples compile: passed
- Samples smoke tests: passed
- Border-only node-info highlight behavior: passed (`TreeViewFxTest`)
- Inline node-info indentation behavior: passed (`TreeViewFxTest`)
- Last-connector non-continuation behavior: passed (`TreeViewFxTest`)
