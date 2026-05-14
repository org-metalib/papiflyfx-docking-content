#

## Research

What it takes to implement a collapsible node info, that shows some extra information about a node?
The info section could be a rich text (html formatted), table, or card/form.

Put your findings to spec/papiflyfx-docking-tree/review2-node-info/research.md

## Planning

Write a very detailed `spec/papiflyfx-docking-tree/review2-node-info/plan.md` document outlining how to implement
 spec/papiflyfx-docking-tree/review2-node-info/research.md. Include code snippets.
 We are going with Option B (full inline behavior). Do not include anything for the other options to the plan.
 Do not include compatibility requirement.

## Implementation

Implement everything specified by `spec/papiflyfx-docking-tree/review2-node-info/plan.md`.
When you’re done with a task or phase, mark it as completed in the `spec/papiflyfx-docking-tree/review2-node-info/plan.md` document.
Add new `spec/papiflyfx-docking-tree/review2-node-info/progress.md` file to track your progress.
Do not stop until all tasks and phases are completed. Do not add unnecessary comments or javadocs, do not use any or unknown types.
Continuously run typecheck to make sure you’re not introducing new issues.

## Key Mapping

> what would be a good key sequence to open/close tree info node for Mac OSX and Windows/Linux

Implemented mapping: macOS uses `⌘I`; Windows/Linux use `Alt+Enter`.
The shortcut acts as a toggle (same key sequence opens and closes node info).

## SamplesApp

Add a treeview with node info sample to SamplesApp

## Issue 1

spec/papiflyfx-docking-tree/review2-node-info/issues/node-info-content-not-shown.png

## Issue 2

for the selected tree node item with node item info, the node item info must not be highlighted

Implemented: selection/hover highlight is now restricted to `ITEM` rows, so inline `INFO` rows keep their normal row background even when the owning item is selected/focused.
Regression coverage: `TreeViewFxTest.selectedItemDoesNotHighlightInlineInfoRow`.

update the spec documents.

## Issue 3

there are two info node policies must be implemented, when toggling on one tree node item info turns off the others.
An when it does not letting the others be visible as well.

Implemented: node-info toggle policies now support both `SINGLE` (exclusive) and `MULTIPLE` (non-exclusive) modes.
Use `treeView.setNodeInfoMode(TreeNodeInfoMode.SINGLE)` to collapse other expanded info rows on toggle, or `treeView.setNodeInfoMode(TreeNodeInfoMode.MULTIPLE)` to keep other expanded info rows visible.
Regression coverage: `TreeViewFxTest.singleInfoModeKeepsOnlyLastToggledInlineInfoExpanded` and `TreeViewFxTest.multipleInfoModeKeepsMultipleInlineInfoRowsExpanded`.
