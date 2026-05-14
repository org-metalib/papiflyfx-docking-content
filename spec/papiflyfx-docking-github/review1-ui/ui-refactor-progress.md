# GitHub Toolbar UI Refactor Progress

## Status

- Phase 1. Data and command groundwork: completed
- Phase 2. Toolbar layout refactor: completed
- Phase 3. Ref popup implementation: completed
- Phase 4. Action migration and cleanup: completed
- Phase 5. Testing, snapshots, and docs: completed

## Notes

- Added richer ref, chip, status, and popup DTOs.
- Added recent-ref persistence abstraction with a preferences-backed implementation.
- Extended the Git repository layer for tag listing, current-ref resolution, safe update, and richer checkout handling.
- Refactored `GitHubToolbarViewModel` to build a coherent toolbar snapshot and popup state while preserving existing legacy accessors.
- Rebuilt `GitHubToolbar` around repo/ref pills, contextual chips, a lower-frequency actions menu, and a transient status slot.
- Consolidated all repository and branch commands into lower-frequency actions (`...`) so they no longer appear on the toolbar or in the ref popup.
- Added an anchored ref popup with search, keyboard navigation, submenu support, dirty-checkout safeguards, recent-ref tracking, and ref-only content.
- Rewrote the GitHub FX, view-model, theme, API, recent-store, and JGit tests to match the new contract.
- Generated review snapshots under `spec/papiflyfx-docking-github/review1-ui/`.
- Validation passed with `mvn -pl papiflyfx-docking-github -am -Dtestfx.headless=true test`.
