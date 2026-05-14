#

## Research

### Prompt 1

What it takes to implement a search feature for the tree component? The feature gets triggered either pushing Cmd+F or
simply start typing a sequence of character. It should show a search dialog similar found in the code component.
Consider an opportunity for code sharing.

It should use a depth-first search. If a matching `TreeItem` is found (case-insensitive partial match), the method should:
1. Select the item in the `TreeView`'s SelectionModel.
2. Automatically expand all parent `TreeItem`s so the found item is visible in the UI.
3. Scroll to the item in the view.

Please provide the Java code for this search method.

Put your findings to spec/papiflyfx-docking-tree/review3-search/research.md

## Planning

Write a very detailed `spec/papiflyfx-docking-tree/review3-search/plan.md` document outlining how to implement
 spec/papiflyfx-docking-tree/review3-search/research.md. Include code snippets.

## Implementation

Implement everything specified by `spec/papiflyfx-docking-tree/review3-search/plan.md`. when you’re done with a task or phase, mark it
as completed in the `spec/papiflyfx-docking-tree/review3-search/plan.md` document.
Add new `spec/papiflyfx-docking-tree/review3-search/progress.md` file to track your progress.
Do not stop until all tasks and phases are completed. Do not add unnecessary comments or javadocs, do not use any or unknown types.
Continuously run typecheck to make sure you’re not introducing new issues.

## Issue 1

Instead of showing one line search box tree search pops a rectangular overlay taking almost whole space making impossible
 to navigate when the size of treeview is narrow.

Theming is not correct either comparing to code editor implementation

Fix it