# papiflyfx-docking-github

As a JavaFX docking framework developer research developing a github docking component that provides the follwoing features:
- The github module name is `papiflyfx-docking-github`
- Visually github components implements a docking application toolbar that could attached to the top or bottom of the application window.
  - Shows a clickable github repository link
  - Shows the github repository current branch if it cloned locally.
- A user can switch branches
- A user can checkout a new branch
- A user can commit changes to the current branch, but not to the master/main/default branch.
- A user can rollback the last commit.
- A user can push changes to the remote repository.
- A user can create a pull request

Here is the link to the docking framework repo: https://github.com/org-metalib/papiflyfx-docking

## github gemini


## github chatgpt


## github grok

## Planning

- plan-copilot-opus.md
- plan-copilot-sonnet.md
- plan-codex.md
Write a very detailed `spec/papiflyfx-docking-github/plan-copilot-sonnet.md` document outlining how to implement this component.
Include code snippets. Do not include compatibility requirements.

incorporate ideas from the following sources:
- spec/papiflyfx-docking-github/github-chatgpt.md
- spec/papiflyfx-docking-github/github-gemini.md
- spec/papiflyfx-docking-github/github-grok.md

## Plan Synthesis 

Read three implementations of the github component:
- spec/papiflyfx-docking-github/plan-codex.md
- spec/papiflyfx-docking-github/plan-copilot-opus.md
- spec/papiflyfx-docking-github/plan-copilot-sonnet.md

Merge the three implementations into a single plan: spec/papiflyfx-docking-github/plan.md
Identify the conflicts and ask for clarification if necessary.

• Merged plan created: spec/papiflyfx-docking-github/plan.md

It includes:

1. A unified implementation plan (module, architecture, contracts, UI/VM, tests, phases).
2. A conflict resolution matrix covering all major differences between Codex/Opus/Sonnet.
3. A dedicated clarification section.

Conflicts identified and merged in the plan:

1. GitHub client (github-api vs java.net.http) -> merged to java.net.http.
2. Local Git abstraction (GitService vs GitRepository) -> merged to GitRepository + JGitRepository.
3. Toolbar API shape (GitHubToolbarPane vs GitHubToolbar) -> merged to GitHubToolbar.
4. Credential storage strategy -> merged to both in-memory and preferences-backed stores.
5. Remote-only support -> included.
6. Rollback safety behavior -> strict revert-only when commit is pushed.
7. Persistence adapter inclusion -> deferred to Phase 6.
8. docks dependency scope -> kept test-only.

Please confirm two points before implementation:

1. Keep java.net.http as the canonical GitHub integration (not org.kohsuke:github-api)?
2. Keep ContentStateAdapter persistence as Phase 6 optional (not required in v1)?

## Implementation

- Implement everything specified by `spec/papiflyfx-docking-github/plan.md`. when you’re done with a task or phase, mark it
as completed in the `spec/papiflyfx-docking-github/plan.md` document.
- Add new `spec/papiflyfx-docking-github/progress.md` file to track your progress.
- Do not stop until all tasks and phases are completed. Do not add unnecessary comments or javadocs, do not use any or unknown types.
- Continuously run typecheck to make sure you’re not introducing new issues.
- add github module readme
