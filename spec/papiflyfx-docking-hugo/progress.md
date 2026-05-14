# papiflyfx-docking-hugo Progress

## Completed Phases

### Phase 1: Module Bootstrap
- [x] Added new module `papiflyfx-docking-hugo`.
- [x] Updated root aggregator and parent dependency management (`javafx-web`).
- [x] Added module `pom.xml` with JavaFX/TestFX/JUnit setup.
- [x] Added `ContentStateAdapter` service registration resource.

### Phase 2: Process Layer
- [x] Implemented `HugoCliProbe`.
- [x] Implemented `HugoServerOptions`.
- [x] Implemented `ProcessLogPump`.
- [x] Implemented `HugoServerProcessManager` with readiness probing, port selection, lifecycle state, log pumping, and stop strategy.
- [x] Added tests: `HugoCliProbeTest`, `HugoServerProcessManagerTest`.

### Phase 3: UI Layer
- [x] Implemented `HugoPreviewPane` (WebView/WebEngine integration, toolbar/status wiring, navigation policy, placeholder handling, cleanup).
- [x] Implemented `HugoPreviewToolbar` and `HugoPreviewStatusBar`.
- [x] Implemented `HugoThemeMapper` and CSS user stylesheet injection.
- [x] Implemented `UrlPolicy` and `WebViewNavigator` helpers.
- [x] Added FX tests: `HugoPreviewPaneFxTest`, `HugoPreviewDockLifecycleFxTest`.

### Phase 4: Docking Persistence
- [x] Implemented `HugoPreviewFactory`.
- [x] Implemented state model/codec: `HugoPreviewState`, `HugoPreviewStateCodec`.
- [x] Implemented `HugoPreviewStateAdapter`.
- [x] Added tests: `HugoPreviewStateCodecTest`, `HugoPreviewStateAdapterTest`.

### Phase 5: Samples and Validation
- [x] Added `HugoPreviewSample` in `papiflyfx-docking-samples`.
- [x] Added module dependency in samples `pom.xml`.
- [x] Registered sample in `SampleCatalog`.
- [x] Validated dock lifecycle interactions (dock/float/minimize/restore/close) via FX test.

## Validation Commands

- [x] `mvn -pl papiflyfx-docking-hugo -am -DskipTests compile`
- [x] `mvn -pl papiflyfx-docking-hugo test`
- [x] `mvn -pl papiflyfx-docking-samples -am -DskipTests compile`
- [x] `mvn -pl papiflyfx-docking-samples test -Dtestfx.headless=true`

## Notes

- Build emits pre-existing warning about duplicate `maven-release-plugin` declarations in parent `pom.xml` pluginManagement.
- JavaFX native-access warnings appear in tests (`javafx.web`, `javafx.media`) and do not fail the build.
