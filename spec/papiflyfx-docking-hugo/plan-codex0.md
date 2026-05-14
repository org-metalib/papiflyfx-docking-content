# papiflyfx-docking-hugo Implementation Plan

## 1. Purpose

Implement a new `papiflyfx-docking-hugo` component that integrates naturally into the PapiflyFX docking ecosystem and provides a dockable Hugo preview panel powered by:

- JavaFX `WebView`/`WebEngine` for internal rendering.
- Hugo CLI (`hugo server`) for content serving and live update.
- Docking API state persistence (`LeafContentData` + `ContentStateAdapter`).
- No internal Java HTTP server and no third-party dependencies.

This plan merges the strongest ideas from:

- `spec/papiflyfx-docking-hugo/hugo-chatgpt.md`
- `spec/papiflyfx-docking-hugo/hugo-gemini.md`
- `spec/papiflyfx-docking-hugo/hugo-grok.md`

while enforcing the local constraints for this repository.

## 2. Hard Constraints

The implementation must satisfy all of the following:

1. New component/module name: `papiflyfx-docking-hugo`.
2. Render Hugo output in an internal JavaFX browser (`WebView`).
3. Use Hugo CLI and assume it is preinstalled.
4. Explicitly check whether Hugo CLI is available.
5. Provide a clickable URL that opens in the default system browser.
6. Do not implement or embed an internal Java server.
7. Start/stop Hugo server process when needed.
8. Target Java 25 and JavaFX 23.0.1.
9. Native-image support is out of scope.
10. Use only available Java/JDK/JavaFX components (no new external dependencies).

## 3. Architecture Decision

### 3.1 Selected Runtime Model

Use **Hugo native dev server only**:

- Start process: `hugo server ...`
- Render URL: `http://127.0.0.1:<port>/...` inside `WebView`
- Stop process on dock disposal or explicit stop

### 3.2 Why This Model

Based on source synthesis:

- From ChatGPT/Gemini: HTTP origin avoids `file://` origin/CORS fragility, and Hugo server provides native live-reload behavior.
- From Grok: this keeps implementation direct and process-driven, with practical toolbar UX.
- Repository constraint: no internal server means we must not use `HttpServer`, Jetty, Undertow, or equivalents.

### 3.3 Explicitly Rejected

- Internal Java server (`com.sun.net.httpserver.HttpServer`, Undertow, Jetty) -> rejected by requirement.
- Loading Hugo output purely via `file://` -> rejected due predictable relative-path/CORS/runtime behavior risks.

## 4. Scope and Non-Goals

### 4.1 In Scope (MVP)

1. Dockable Hugo preview pane.
2. Hugo preflight availability check.
3. Start/stop Hugo server process.
4. Embedded `WebView` navigation controls.
5. Open current URL in default browser.
6. Docking theme integration (`Theme` binding and runtime updates).
7. Persist/restore core state using `ContentStateAdapter`.
8. Tests for process orchestration and restore behavior.

### 4.2 Non-Goals

1. Bundling Hugo binary.
2. Implementing internal file server.
3. Native-image integration.
4. Advanced multi-site workspace manager (future phase).

## 5. Proposed Module and File Layout

```text
papiflyfx-docking-hugo/
  pom.xml
  src/main/java/org/metalib/papifly/fx/hugo/api/
    HugoPreviewPane.java
    HugoPreviewFactory.java
    HugoPreviewStateAdapter.java
    HugoPreviewConfig.java
    HugoPreviewState.java
    HugoProcessState.java
  src/main/java/org/metalib/papifly/fx/hugo/process/
    HugoCliProbe.java
    HugoServerProcessManager.java
    HugoServerOptions.java
    ProcessLogPump.java
  src/main/java/org/metalib/papifly/fx/hugo/web/
    WebViewNavigator.java
    UrlPolicy.java
  src/main/java/org/metalib/papifly/fx/hugo/theme/
    HugoThemeMapper.java
  src/main/resources/META-INF/services/
    org.metalib.papifly.fx.docking.api.ContentStateAdapter
  src/test/java/org/metalib/papifly/fx/hugo/
    HugoCliProbeTest.java
    HugoServerProcessManagerTest.java
    HugoPreviewPaneFxTest.java
    HugoPreviewStateAdapterTest.java
```

## 6. Maven Integration Plan

### 6.1 Parent Aggregator (`/pom.xml`)

Add module:

```xml
<modules>
  ...
  <module>papiflyfx-docking-hugo</module>
</modules>
```

### 6.2 New Module POM (`papiflyfx-docking-hugo/pom.xml`)

Use the same style as existing modules and only JavaFX/JUnit/TestFX artifacts already used in repo:

```xml
<dependencies>
  <dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-api</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <classifier>${javafx.platform}</classifier>
  </dependency>
  <dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>${javafx.version}</version>
    <classifier>${javafx.platform}</classifier>
  </dependency>

  <!-- tests: same set already used by other modules -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

No additional libraries are introduced.

## 7. Core Domain Model

### 7.1 Preview Configuration

```java
package org.metalib.papifly.fx.hugo.api;

import java.nio.file.Path;

public record HugoPreviewConfig(
    Path siteRoot,
    String contentId,
    String basePath,
    int preferredPort,
    boolean autoStart,
    boolean allowExternalNavigation
) {
    public HugoPreviewConfig {
        if (siteRoot == null) throw new IllegalArgumentException("siteRoot is required");
        if (contentId == null || contentId.isBlank()) throw new IllegalArgumentException("contentId is required");
        if (preferredPort <= 0) preferredPort = 1313;
        if (basePath == null || basePath.isBlank()) basePath = "/";
    }
}
```

### 7.2 Persisted State Shape

Use `LeafContentData` map schema:

```text
typeKey:  "hugo-preview"
version:  1
state:
  siteRoot:       String absolute path
  basePath:       String ("/", "/docs/", "/guide/")
  relativePath:   String ("/posts/intro/")
  preferredPort:  Number
  autoStart:      Boolean
```

No process PID or ephemeral runtime flags are persisted.

## 8. Hugo CLI Availability Check

### 8.1 Behavior

At pane creation (or first start request):

1. Execute `hugo version`.
2. If exit code `0`, mark CLI available.
3. If command missing/fails/timeouts, mark unavailable and show actionable UI message.

### 8.2 Implementation Snippet

```java
package org.metalib.papifly.fx.hugo.process;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class HugoCliProbe {

    public boolean isAvailable(Duration timeout) {
        ProcessBuilder pb = new ProcessBuilder("hugo", "version");
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
```

## 9. Hugo Server Lifecycle Management

This incorporates the process orchestration and stream-handling ideas from Gemini/Grok.

### 9.1 Command Strategy

Start command:

```bash
hugo server --bind 127.0.0.1 --port <port> --baseURL http://127.0.0.1:<port>/ --disableFastRender
```

Stop command:

- `process.destroy()`, wait short timeout.
- if still alive -> `process.destroyForcibly()`.

### 9.2 Manager Snippet

```java
package org.metalib.papifly.fx.hugo.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class HugoServerProcessManager implements AutoCloseable {

    private final AtomicReference<Process> processRef = new AtomicReference<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hugo-log-pump");
        t.setDaemon(true);
        return t;
    });

    private volatile int boundPort = -1;

    public synchronized URI start(Path siteRoot, int preferredPort) throws Exception {
        if (isRunning()) {
            return endpoint();
        }
        int port = selectPort(preferredPort);
        ProcessBuilder pb = new ProcessBuilder(buildCommand(port));
        pb.directory(siteRoot.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        processRef.set(process);
        boundPort = port;
        ioExecutor.submit(() -> consumeOutput(process));
        awaitReadiness(Duration.ofSeconds(12));
        return endpoint();
    }

    public synchronized void stop() {
        Process process = processRef.getAndSet(null);
        if (process == null) {
            boundPort = -1;
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            boundPort = -1;
        }
    }

    public synchronized boolean isRunning() {
        Process process = processRef.get();
        return process != null && process.isAlive();
    }

    public synchronized URI endpoint() {
        if (boundPort <= 0) throw new IllegalStateException("server not started");
        return URI.create("http://127.0.0.1:" + boundPort + "/");
    }

    @Override
    public void close() {
        stop();
        ioExecutor.shutdownNow();
    }

    private List<String> buildCommand(int port) {
        return List.of(
            "hugo", "server",
            "--bind", "127.0.0.1",
            "--port", Integer.toString(port),
            "--baseURL", "http://127.0.0.1:" + port + "/",
            "--disableFastRender"
        );
    }

    private int selectPort(int preferredPort) throws Exception {
        for (int i = 0; i < 30; i++) {
            int candidate = preferredPort + i;
            if (isPortFree(candidate)) return candidate;
        }
        throw new IllegalStateException("No free port available for Hugo server");
    }

    private boolean isPortFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void consumeOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Future: route to app logger / console dock
            }
        } catch (Exception ignored) {
            // stream closed on process stop
        }
    }

    private void awaitReadiness(Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!isRunning()) throw new IllegalStateException("Hugo server exited during startup");
            try (java.net.Socket ignored = new java.net.Socket()) {
                ignored.connect(new InetSocketAddress("127.0.0.1", boundPort), 200);
                return;
            } catch (Exception ignored) {
                Thread.sleep(80);
            }
        }
        throw new TimeoutException("Timed out waiting for Hugo server readiness");
    }
}
```

## 10. JavaFX Dock Content Design

This combines:

- ChatGPT: `DisposableContent`, `LoadWorker` hooks, state persistence integration.
- Gemini: FX-thread discipline and cleanup sequence.
- Grok: toolbar usability and external-browser affordance.

### 10.1 UI Composition

- `BorderPane`
- Top: toolbar (`Start`, `Stop`, `Reload`, `Back`, `Forward`, `Open in Browser`)
- Center: `WebView`
- Bottom: status label (`Hugo not installed`, `Running`, `Stopped`, `Load failed`)

### 10.2 Pane Skeleton

```java
package org.metalib.papifly.fx.hugo.api;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.hugo.theme.HugoThemeMapper;

import java.awt.Desktop;
import java.net.URI;
import java.time.Duration;

public final class HugoPreviewPane extends BorderPane implements DisposableContent {

    private final HugoPreviewConfig config;
    private final HugoCliProbe cliProbe = new HugoCliProbe();
    private final HugoServerProcessManager processManager = new HugoServerProcessManager();

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private final Label status = new Label("Idle");
    private final Hyperlink urlLink = new Hyperlink("-");
    private final HBox toolbar = new HBox(8);
    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;

    private volatile String lastRelativePath = "/";

    public HugoPreviewPane(HugoPreviewConfig config) {
        this.config = config;
        setCenter(webView);
        buildToolbar();
        setTop(toolbar);
        setBottom(status);
        bindEngineSignals();
        applyDockingTheme(themeProperty.get());

        if (!cliProbe.isAvailable(Duration.ofSeconds(3))) {
            status.setText("Hugo CLI is not available in PATH");
            return;
        }
        if (config.autoStart()) {
            startServerAndLoad(config.basePath());
        }
    }

    private void buildToolbar() {
        Button start = new Button("Start");
        Button stop = new Button("Stop");
        Button reload = new Button("Reload");
        Button back = new Button("Back");
        Button forward = new Button("Forward");
        Button openExternal = new Button("Open in Browser");

        start.setOnAction(e -> startServerAndLoad(config.basePath()));
        stop.setOnAction(e -> stopServer());
        reload.setOnAction(e -> engine.reload());
        back.setOnAction(e -> engine.executeScript("history.back()"));
        forward.setOnAction(e -> engine.executeScript("history.forward()"));
        openExternal.setOnAction(e -> openInBrowser(engine.getLocation()));
        urlLink.setOnAction(e -> openInBrowser(engine.getLocation()));

        toolbar.getChildren().setAll(start, stop, reload, back, forward, openExternal, urlLink);
    }

    private void bindEngineSignals() {
        engine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation == null || newLocation.isBlank()) return;
            urlLink.setText(newLocation);
            lastRelativePath = toRelativePath(newLocation);
        });
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                status.setText("Loaded");
            } else if (newState == Worker.State.FAILED) {
                status.setText("Load failed");
            }
        });
    }

    public void startServerAndLoad(String relativePath) {
        try {
            URI base = processManager.start(config.siteRoot(), config.preferredPort());
            String target = base.resolve(normalizePath(relativePath)).toString();
            engine.load(target);
            status.setText("Running: " + base);
        } catch (Exception ex) {
            status.setText("Failed to start Hugo server: " + ex.getMessage());
        }
    }

    public void stopServer() {
        processManager.stop();
        status.setText("Stopped");
        engine.load("about:blank");
    }

    public String currentRelativePath() {
        return lastRelativePath;
    }

    public HugoPreviewConfig getConfig() {
        return config;
    }

    @Override
    public void dispose() {
        stopServer();
        Platform.runLater(() -> setCenter(null));
    }

    private void openInBrowser(String location) {
        try {
            if (location == null || location.isBlank()) return;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(location));
            }
        } catch (Exception ignored) {
            // Keep UX non-fatal
        }
    }

    private String toRelativePath(String location) {
        try {
            URI uri = URI.create(location);
            String p = uri.getRawPath();
            if (p == null || p.isBlank()) return "/";
            return p;
        } catch (Exception ex) {
            return "/";
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    public void bindThemeProperty(ObjectProperty<Theme> externalTheme) {
        unbindThemeProperty();
        if (externalTheme == null) {
            return;
        }
        boundThemeProperty = externalTheme;
        themeChangeListener = (obs, oldTheme, newTheme) -> applyDockingTheme(newTheme);
        boundThemeProperty.addListener(themeChangeListener);
        applyDockingTheme(boundThemeProperty.get());
    }

    public void unbindThemeProperty() {
        if (boundThemeProperty != null && themeChangeListener != null) {
            boundThemeProperty.removeListener(themeChangeListener);
        }
        boundThemeProperty = null;
        themeChangeListener = null;
    }

    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    private void applyDockingTheme(Theme theme) {
        Theme resolved = theme == null ? Theme.dark() : theme;
        themeProperty.set(resolved);
        setBackground(new Background(new BackgroundFill(
            resolved.background(),
            javafx.scene.layout.CornerRadii.EMPTY,
            javafx.geometry.Insets.EMPTY)));
        toolbar.setBackground(new Background(new BackgroundFill(
            HugoThemeMapper.toolbarBackground(resolved),
            javafx.scene.layout.CornerRadii.EMPTY,
            javafx.geometry.Insets.EMPTY)));
        status.setTextFill(HugoThemeMapper.toColor(resolved.textColor()));
        urlLink.setTextFill(HugoThemeMapper.toColor(resolved.accentColor()));
    }
}
```

## 11. External Navigation Policy

Adopt a strict local-preview policy:

1. Internal navigation allowed only for active Hugo origin (`127.0.0.1:<boundPort>`).
2. If navigation target is external:
   - cancel/rollback internal navigation,
   - open in system browser,
   - keep preview dock on local content.

```java
engine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
    if (newLocation == null || oldLocation == null) return;
    URI target = URI.create(newLocation);
    URI base = processManager.endpoint();
    boolean sameHost = "127.0.0.1".equalsIgnoreCase(target.getHost());
    boolean samePort = target.getPort() == base.getPort();
    if (!(sameHost && samePort)) {
        Platform.runLater(() -> engine.load(oldLocation));
        openInBrowser(newLocation);
    }
});
```

## 12. Docking Integration

### 12.1 Content Factory

```java
package org.metalib.papifly.fx.hugo.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

import java.nio.file.Path;

public final class HugoPreviewFactory implements ContentFactory {

    public static final String FACTORY_ID = "hugo-preview";

    private final Path defaultSiteRoot;

    public HugoPreviewFactory(Path defaultSiteRoot) {
        this.defaultSiteRoot = defaultSiteRoot;
    }

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) return null;
        return new HugoPreviewPane(new HugoPreviewConfig(
            defaultSiteRoot,
            "hugo:default",
            "/",
            1313,
            true,
            false
        ));
    }
}
```

### 12.2 Content State Adapter

```java
package org.metalib.papifly.fx.hugo.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HugoPreviewStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return HugoPreviewFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof HugoPreviewPane pane)) return Map.of();
        Map<String, Object> state = new LinkedHashMap<>();
        HugoPreviewConfig cfg = pane.getConfig();
        state.put("siteRoot", cfg.siteRoot().toAbsolutePath().toString());
        state.put("basePath", cfg.basePath());
        state.put("relativePath", pane.currentRelativePath());
        state.put("preferredPort", cfg.preferredPort());
        state.put("autoStart", cfg.autoStart());
        return state;
    }

    @Override
    public Node restore(LeafContentData content) {
        Map<String, Object> state = content != null ? content.state() : null;
        if (state == null || state.isEmpty()) {
            return null;
        }
        String siteRoot = String.valueOf(state.getOrDefault("siteRoot", ""));
        String basePath = String.valueOf(state.getOrDefault("basePath", "/"));
        int preferredPort = ((Number) state.getOrDefault("preferredPort", 1313)).intValue();
        boolean autoStart = Boolean.TRUE.equals(state.get("autoStart"));
        String relativePath = String.valueOf(state.getOrDefault("relativePath", "/"));

        HugoPreviewPane pane = new HugoPreviewPane(new HugoPreviewConfig(
            Path.of(siteRoot),
            content != null ? content.contentId() : "hugo:restored",
            basePath,
            preferredPort,
            autoStart,
            false
        ));
        if (autoStart) {
            pane.startServerAndLoad(relativePath);
        }
        return pane;
    }
}
```

Add service registration:

```text
# src/main/resources/META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter
org.metalib.papifly.fx.hugo.api.HugoPreviewStateAdapter
```

### 12.3 DockManager Wiring Example

```java
ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new HugoPreviewStateAdapter());

DockManager dm = new DockManager();
dm.setOwnerStage(stage);
dm.setContentStateRegistry(registry);
dm.setContentFactory(new HugoPreviewFactory(Path.of("/workspace/docs-site")));

HugoPreviewPane preview = new HugoPreviewPane(new HugoPreviewConfig(
    Path.of("/workspace/docs-site"),
    "hugo:docs",
    "/",
    1313,
    true,
    false
));

DockLeaf leaf = dm.createLeaf("Docs Preview", preview);
leaf.setContentFactoryId(HugoPreviewFactory.FACTORY_ID);
leaf.setContentData(LeafContentData.of(
    HugoPreviewFactory.FACTORY_ID,
    "hugo:docs",
    HugoPreviewStateAdapter.VERSION
));
preview.bindThemeProperty(dm.themeProperty());
```

## 13. Docking Theme Integration

Theme support should follow the same runtime pattern used by `CodeEditor`, `TreeView`, and `MediaViewer`:

1. Expose `bindThemeProperty(ObjectProperty<Theme>)`.
2. Expose `unbindThemeProperty()`.
3. Apply style immediately and reactively when theme changes.
4. Keep mapping logic in a dedicated mapper class.

### 13.1 Theme Mapper Snippet

```java
package org.metalib.papifly.fx.hugo.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

public final class HugoThemeMapper {

    private HugoThemeMapper() {}

    public static Paint toolbarBackground(Theme t) {
        return t.headerBackground();
    }

    public static Paint statusBackground(Theme t) {
        return t.background();
    }

    public static Paint statusText(Theme t) {
        return t.textColor();
    }

    public static Paint linkColor(Theme t) {
        return t.accentColor();
    }

    public static Color toColor(Paint paint) {
        return paint instanceof Color c ? c : Color.GRAY;
    }
}
```

### 13.2 Theming Behavior in `HugoPreviewPane`

1. Toolbar/background/status colors update with dock theme changes.
2. Controls use theme text and accent colors for consistency.
3. Web content remains Hugo-controlled; the component themes only JavaFX chrome around `WebView`.

### 13.3 Theming in DockManager Wiring

Always bind preview pane theme to manager theme:

```java
preview.bindThemeProperty(dm.themeProperty());
```

### 13.4 Theme-focused Tests

1. Bind to `Theme.dark()` then `Theme.light()`.
2. Assert toolbar background and status/link text paints are updated.
3. Assert no listener leak after `unbindThemeProperty()` and `dispose()`.

## 14. Concurrency and Threading Rules

From Gemini/ChatGPT source guidance:

1. All `WebEngine` calls must run on JavaFX Application Thread.
2. Process output pumping runs on background thread(s).
3. If output parsing updates UI status, marshal via `Platform.runLater`.

```java
ioExecutor.submit(() -> {
    for (String line : linesFromHugo) {
        Platform.runLater(() -> status.setText(line));
    }
});
```

## 15. Resource Cleanup Contract

When dock leaf closes, `DockLeaf.dispose()` calls `DisposableContent.dispose()`.  
`HugoPreviewPane.dispose()` must:

1. stop Hugo process,
2. clear web content (`about:blank`),
3. release UI references/listeners.

Recommended cleanup order:

```java
public void dispose() {
    stopServer();                  // terminate subprocess first
    engine.load("about:blank");   // release DOM/resources
    setCenter(null);              // detach WebView node
}
```

## 16. Error Handling Matrix

| Condition | Detection | User-facing behavior | Recovery |
|---|---|---|---|
| Hugo CLI missing | `hugo version` fails | Status: "Hugo CLI is not available in PATH" | Install Hugo, restart/start again |
| Site root invalid | `!Files.isDirectory(siteRoot)` | Status: "Invalid Hugo site root" | Choose valid path |
| Port unavailable | startup exception | Status with port error | Retry with next free port |
| Hugo exits early | process not alive during startup | Status: "Hugo server exited during startup" | Inspect logs, retry |
| Web load failure | `LoadWorker.State.FAILED` | Status: "Load failed" | Reload or restart server |
| External link click | URL host/port mismatch | open in default browser | no interruption to dock preview |

## 17. Test Plan

### 16.1 Unit Tests

1. `HugoCliProbeTest`
   - returns `false` for invalid binary path override.
2. `HugoServerProcessManagerTest`
   - start/stop lifecycle.
   - stop is idempotent.
   - readiness timeout path.
3. `HugoPreviewStateAdapterTest`
   - save map keys and types.
   - restore builds pane with expected config.

### 16.2 UI Tests (TestFX)

1. `HugoPreviewPaneFxTest`
   - pane renders toolbar and WebView.
   - `Start` updates status and URL label (when Hugo available).
   - `Stop` transitions to `about:blank`.
2. Docking session roundtrip:
   - create leaf with hugo preview.
   - save session string.
   - restore session.
   - assert content restored through adapter.

### 16.3 Conditional Test Execution

Tests requiring real Hugo binary should skip when unavailable:

```java
Assumptions.assumeTrue(new HugoCliProbe().isAvailable(Duration.ofSeconds(2)));
```

## 18. Implementation Phases

### Phase 1: Module Bootstrap

1. Create `papiflyfx-docking-hugo` module.
2. Add dependencies (JavaFX controls/web + docking api + tests).
3. Add service registration skeleton.

### Phase 2: Process Layer

1. Implement `HugoCliProbe`.
2. Implement `HugoServerProcessManager`.
3. Add process unit tests.

### Phase 3: UI Layer

1. Implement `HugoPreviewPane`.
2. Add toolbar actions and status reporting.
3. Add docking theme binding and reactive style updates.
4. Add external browser link behavior.

### Phase 4: Docking Persistence

1. Implement `HugoPreviewFactory`.
2. Implement `HugoPreviewStateAdapter`.
3. Register adapter and validate `DockManager` save/restore path.

### Phase 5: Samples and Validation

1. Add sample under `papiflyfx-docking-samples`.
2. Validate start/stop behavior while docking, floating, minimizing, and closing.
3. Run tests:
   - `mvn test -pl papiflyfx-docking-hugo`
   - `mvn test -pl papiflyfx-docking-samples -Dtestfx.headless=true`

## 19. Acceptance Criteria

1. New dock content renders Hugo site in internal WebView.
2. Missing Hugo binary is detected and shown clearly.
3. Component can start and stop Hugo server repeatedly without orphan process.
4. Component reacts to `DockManager` theme changes via bound `Theme` property.
5. Current URL/path can be opened in default system browser from clickable control.
6. Session save/restore recreates component and restores preview path.
7. No new runtime dependencies beyond existing JavaFX/JDK and project modules.

## 20. Source-Idea Traceability

### Adopted from `hugo-chatgpt.md`

1. Dock lifecycle integration via `DisposableContent`.
2. Persistence alignment with `LeafContentData` and adapter chain.
3. `WebEngine` load-state observation and navigation helpers.
4. External-link sandboxing pattern.

### Adopted from `hugo-gemini.md`

1. Strict FX-thread rule for `WebView/WebEngine`.
2. Process stream gobbling to avoid deadlocks.
3. Controlled subprocess lifecycle tied to dock disposal.
4. Aggressive cleanup (`about:blank`, detach resources).

### Adopted from `hugo-grok.md`

1. Practical panel UX: toolbar with refresh/nav/open external.
2. `ProcessBuilder` orchestration examples for `hugo server`.
3. Local developer workflow emphasis.

### Intentionally Not Adopted

1. Internal Java HTTP server strategies from sources (conflicts with hard constraint).
2. Dependency-heavy server options (Jetty/Undertow).
3. Native-image strategy (explicitly out of requested scope).
