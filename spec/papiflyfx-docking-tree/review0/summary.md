# SamplesApp navigation crash summary

## Issue
Navigating the SamplesApp tree with keyboard up/down repeatedly could eventually crash JavaFX rendering with:

`java.lang.NullPointerException: Cannot invoke "com.sun.prism.RTTexture.createGraphics()" because "<local9>" is null`

## Root cause
SamplesApp replaced the center content on each selection change but did not dispose the previous sample node tree. Many samples create `DockManager` instances with canvas-heavy content (`CodeEditor`, `TreeView`) and bind manager/theme state, so undisposed content accumulated and stale render resources were retained.

## Fix implemented
- Added lifecycle cleanup in `SamplesApp`:
  - dispose current content before loading a newly selected sample;
  - dispose content on app stop and stage close.
- Implemented recursive disposal in `SamplesApp` that:
  - disposes `DisposableContent` nodes;
  - detects `DockManager` roots and disposes each manager exactly once.
- Updated `DockManager` to support safe external cleanup:
  - publish a root marker property: `DockManager.ROOT_PANE_MANAGER_PROPERTY`;
  - unbind `themeProperty` during `dispose()` when bound;
  - clear marker and release manager references (`floatingWindowManager`, `contentFactory`, `ownerStage`) during disposal.

## Verification
- Added `DockManagerDisposeFxTest` covering:
  - root marker presence on manager root pane;
  - `dispose()` unbinds theme when manager theme is externally bound.
- Ran and passed:
  - `./mvnw -pl papiflyfx-docking-docks -am -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DockManagerDisposeFxTest test`
  - `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SamplesSmokeTest test`
