# 

Let's refactor the UI/UX! The toolbar should contain a name of the repository and a name of the current branch/tag.
It can show a small status indicator like a green dot if no modifications made and red one if there are some.
When a user clicks on the name of the repository it should open the repository in the default browser.
When a user clicks on the name of the branch/tag it should open popup menu like shown in the screenshot.
Work on these ideas to refactor the UI/UX and comeup with the a new design. Depict the new design in
spec/papiflyfx-docking-github/review1-ui/ui-refactor.md.
Use spec/papiflyfx-docking-github/review1-ui/idea-branch-popup.png.

## Planning

### codex

check spec/papiflyfx-docking-github/review1-ui/ui-refactor.md document and write a plan for suggested refactoring in 
spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md

## copilot+sonnet
check spec/papiflyfx-docking-github/review1-ui/ui-refactor.md document and write a plan for suggested refactoring in
spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-copilot-sonnet.md

## claude+opus
check spec/papiflyfx-docking-github/review1-ui/ui-refactor.md document and write a plan for suggested refactoring in
spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-claude+opus.md

## add from copilot+sonnet to codex
Check spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-copilot-sonnet.md and add what's been missed to
spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md

## add from claude+opus to codex
Check spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-claude-opus.md and add what's been missed to
spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md

## Implementation

- Implement everything specified by `spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md`. when you’re done with a task or phase, mark it
  as completed in the `spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md` document.
- Add new `spec/papiflyfx-docking-github/review1-ui/ui-refactor-progress.md` file to track your progress.
- Do not stop until all tasks and phases are completed. Do not add unnecessary comments or javadocs, do not use any or unknown types.
- Continuously run typecheck to make sure you’re not introducing new issues.

## Revisions

### SamplesApp is failing

> SamplesApp is failing

### Color schema is not set for all elements

> lower-frequency actions color schema does not follow the current theme

### Popup action is not in focus

When I open a popup menu with action and branch/tag list the focus is on the search box, I have to click twice if I choose a branch or tag.
I should be able to click once.

You did not resolve the issue. The search button is in focus, but if click on a bracnh name or an action like new branch the focus moves to the whole list first and I have to click again to enforce the
selection. (brittle dialog dependency)

## Hide all actions

All actions should go to what is called `lower-frequency actions` amd should not be present on a tool bar or branch/tag list.
Use spec/papiflyfx-docking-github/review1-ui/ui-refactor-plan-codex.md as a reference

When you finish update spec/papiflyfx-docking-github/review1-ui/ui-refactor-progress.md and the spec.

The tasks:
1. Remove action rows from the ref popup state and keep it branch/tag switching only.
2. Collapse the main action bar so it only shows the lower-frequency actions trigger, with all commands moved into that menu.
3. Rewrite the affected FX/theme assertions and update the progress/spec docs to reflect the new action contract.
4. Run compile/tests as I go so type or behavior regressions don’t accumulate.
