# Progress: Inline Collapsible Node Info (Option B)

## Completed phases

- [x] Phase 1: API + node-info state model
- [x] Phase 2: mixed flattened rows (item + info rows)
- [x] Phase 3: variable-height viewport metrics + hit-testing
- [x] Phase 4: inline JavaFX node host virtualization
- [x] Phase 5: input/edit/drag-drop integration
- [x] Phase 6: state persistence + adapter version bump
- [x] Phase 7: tests and regression validation

## Implemented surfaces

- Added `TreeNodeInfoProvider`, `TreeNodeInfoMode`, `TreeNodeInfoModel`.
- Refactored flattening to mixed row kinds with item/info lookup helpers.
- Refactored viewport geometry to variable row heights with binary-search hit testing.
- Added `TreeInlineInfoHost` and integrated inline node overlay synchronization in `TreeView`.
- Added pointer info-toggle hit zone and platform keyboard toggles for focused-item info (`⌘I` on macOS, `Alt+Enter` on Windows/Linux).
- Updated drag/drop to normalize info-row hits to owner item rows.
- Extended persisted state with `expandedInfoPaths`, updated codec contract, and bumped state adapter version to `2`.

## Added/updated tests

- Added `TreeNodeInfoModelTest`.
- Added `TreeViewportTest`.
- Added `TreeStateCodecTest`.
- Updated `FlattenedTreeTest` with info-row assertions.
- Updated `TreeViewFxTest` with pointer toggle, keyboard toggle, state restore, and non-highlighted inline info row coverage (`selectedItemDoesNotHighlightInlineInfoRow`).

## Post-delivery stabilization

- Fixed row background rendering so selection/hover highlights apply only to `ITEM` rows in `TreeBackgroundPass`.
- Confirmed selected items with expanded node info no longer highlight their inline `INFO` rows.
- Implemented both node-info toggle policies end-to-end:
  - `SINGLE` mode keeps only the last toggled info row expanded,
  - `MULTIPLE` mode allows multiple info rows to stay expanded.
- Added FX regression coverage for both policies in `TreeViewFxTest` and exposed a policy selector in `TreeViewNodeInfoSample`.

## Validation

- [x] `./mvnw -pl papiflyfx-docking-tree -am -DskipTests compile`
- [x] `./mvnw -pl papiflyfx-docking-tree test -Dtestfx.headless=true`

## SamplesApp integration updates

- Added new sample `TreeViewNodeInfoSample` and registered it in `SampleCatalog` as `Tree View (Inline Node Info + Policies)`.
- Added inline node-info demo content for rich text, table-like grid, and card/form-like metadata.
- Applied responsiveness fix after runtime feedback:
  - switched sample info content from heavier controls to lightweight labels/grid,
  - set `showRoot(false)`,
  - reduced default expanded info rows.
- Fixed tree interaction blocking root cause: inline info host overlay now uses `setPickOnBounds(false)` so empty overlay area no longer consumes mouse events.
- Fixed inline content rendering after info expansion by syncing overlay after viewport layout and forcing hosted parent node layout.
- Added FX regression test `expandedInlineInfoMountsVisibleContent` to prevent blank expanded info panels.
- Updated `TreeViewNodeInfoSample` info card/value colors to theme-aware palette (dark/light adaptive) with live updates when switching SamplesApp theme mode.
- Made policy behavior explicit in the sample UI with an `Info policy` selector (`SINGLE`/`MULTIPLE`) and helper text explaining exclusive vs non-exclusive expansion behavior.

## Samples validation

- [x] `./mvnw -pl papiflyfx-docking-samples -am -DskipTests compile`
- [x] `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest test -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false`
