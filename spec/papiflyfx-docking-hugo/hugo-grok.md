Here is a structured **research and design document** for developing a **Hugo preview docking component** within your PapiflyFX Docking Framework (https://github.com/org-metalib/papiflyfx-docking). This component will provide a dockable panel that renders and previews a Hugo-based static website directly inside a JavaFX application—ideal for developers building or maintaining Hugo sites who want an integrated live preview without leaving their tooling.

### 1. Project Overview & Goals

**Component Name Suggestion**: `HugoPreviewDockable` (or `HugoSiteViewerPane`)

**Purpose**
Embed a full Hugo static site preview inside a dockable panel using JavaFX's `WebView`. This allows real-time(ish) visualization of Hugo content during development, with docking features like drag-and-drop repositioning, floating windows, minimize/maximize, and layout persistence (leveraging PapiflyFX's JSON session support).

**Key Goals**
- Seamless integration as a custom dockable content node in PapiflyFX.
- Support loading a local Hugo site's generated static files (`public/` folder).
- Optional integration with Hugo's development server for live reloads.
- Respect JavaFX threading rules and WebView limitations.
- Provide basic controls (refresh, zoom, navigation history, open in external browser).

**Target Use Cases**
- Hugo theme developers previewing changes.
- Content authors editing Markdown and seeing rendered output in their IDE-like app.
- Multi-project workspaces where one dock shows the Hugo site preview alongside editors, terminals, etc.

### 2. Technology Background

**Hugo SSG (https://gohugo.io/)**
Hugo is a fast, open-source static site generator written in Go. It takes Markdown content, templates, and assets → generates pure static HTML/CSS/JS files (default output directory: `public/`).
- Sites run without any server (just file serving).
- Local development preview: `hugo server` → http://localhost:1313 with live reload (file watcher + WebSocket).
- Output is fully static → perfect for offline/local embedding.

**JavaFX WebView**
`javafx.scene.web.WebView` is the built-in component for rendering web content (HTML5, CSS3, JavaScript).
- Uses WebKit engine (via WebEngine).
- Loads remote URLs, local files, or HTML strings.
- Supports navigation history, zooming, JavaScript execution, alerts, etc.
- Key methods: `webEngine.load(url)`, `webEngine.loadContent(html)`, `webEngine.reload()`.
- Local file loading: Use `file:///` protocol with absolute paths (e.g., `file:///path/to/public/index.html`).
- Directory serving nuance: WebView does not run a local HTTP server → loading `file:///path/to/public/` may not resolve relative paths correctly unless the entry point is explicitly an `index.html`. Best practice: always load the root `index.html`.

**PapiflyFX Docking Framework**
Modern, actively maintained (as of March 2026) JavaFX docking system.
- Core in `papiflyfx-docking-docks` module.
- Supports drag-and-drop docking, floating stages, minimize/maximize, JSON-based layout persistence.
- Custom content is supplied as any `javafx.scene.Node` (→ `WebView` fits perfectly).
- Typical integration: Create a custom `Dockable`/`DockNode` wrapper that hosts your content pane.

### 3. Architecture & Design

**High-Level Structure**

1. **HugoPreviewPane** (extends `BorderPane` or `VBox`): Core UI node containing:
   - `WebView` as central content.
   - Optional toolbar (top): refresh button, zoom slider, URL display, "Open externally" button.
   - Status bar (bottom): loading indicator, error messages.

2. **HugoPreviewDockable**: Wraps `HugoPreviewPane` into a PapiflyFX dockable component (follow framework's `Dockable` pattern or factory method).

3. **Configuration Options** (via constructor or properties):
   - `Path siteRoot` — path to Hugo project's `public/` folder (or root if already built).
   - `boolean useDevServer` — if true, load `http://localhost:1313` instead of local files.
   - `String entryPoint` — default `/index.html`.
   - Auto-refresh interval (for polling file changes if not using `hugo server`).

**Loading Strategies**

**Option B: Hugo Dev Server (Best for live development)**
- Application starts (or detects running) `hugo server` process (via `ProcessBuilder`).
- Load: `webEngine.load("http://localhost:1313/")`.
- Hugo watches files → auto-reloads browser via WebSocket.
- Pros: Instant preview on save.
- Cons: Requires Hugo binary installed, manages external process (start/stop, port conflicts).

**Error & Loading Handling**
- Listen to `WebEngine.getLoadWorker().stateProperty()` → show progress indicator.
- Handle `Exception` → display friendly error (e.g., "Site not built – run `hugo` first").
- JavaFX threading: All `WebEngine` calls on JavaFX Application Thread.

### 4. Implementation Outline (Pseudocode)

```java
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.layout.BorderPane;
import java.nio.file.Path;
import java.net.URL;

public class HugoPreviewPane extends BorderPane {

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private Path siteRoot;

    public HugoPreviewPane(Path hugoPublicDir) {
        this.siteRoot = hugoPublicDir;
        setCenter(webView);

        // Basic toolbar (expand as needed)
        // Button refreshBtn = new Button("Refresh");
        // refreshBtn.setOnAction(e -> engine.reload());

        loadSite();
    }

    public void loadSite() {
        try {
            Path index = siteRoot.resolve("index.html");
            if (!Files.exists(index)) {
                // show error: "Build the site first with 'hugo'"
                engine.loadContent("<h1>Error: index.html not found</h1>");
                return;
            }
            URL url = index.toUri().toURL();
            engine.load(url.toExternalForm());
        } catch (Exception e) {
            // handle
        }
    }

    // Add methods: setDevServerMode(boolean), refresh(), etc.
}
```

**Integration with PapiflyFX**
Follow the docking module's README / examples for registering custom dockables. Typically:
- Create factory or builder that returns `HugoPreviewPane` wrapped in the framework's dock node.
- Use JSON persistence for remembering last loaded site path.

### 5. Challenges & Solutions

- **Relative paths in static files** — Loading `file:///.../index.html` usually resolves relative assets correctly if base URL is set properly.
  Solution: Use `engine.setUserStyleSheetLocation()` or inject base `<base href="...">` if needed.

- **JavaFX WebView limitations** — No full Chrome DevTools, some modern JS features may differ.
  Solution: For complex sites, fallback to dev server + external browser option.

- **Security / sandbox** — Local file access is allowed, but WebView restricts some cross-origin behaviors.
  Solution: Stick to same-origin (all local).

- **Performance** — Heavy sites may lag.
  Solution: Lazy load, dispose WebView when undocked/closed.

### 6. Next Steps / Roadmap

1. Prototype `HugoPreviewPane` with static loading.
2. Integrate as dockable in a minimal PapiflyFX test app.
3. Add dev server support (Process management).
4. Add UI controls + configuration dialog.
5. Test with real Hugo themes (e.g., docsy, paperesque).
6. Contribute back to repo or create companion module.

---

### Specific Docking Integration for HugoPreviewPane in PapiflyFX

Since the PapiflyFX Docking Framework (from your repo) emphasizes features like drag-and-drop panels, floating windows, minimize/maximize, and JSON-based layout persistence, but lacks detailed public documentation or examples in the README for custom integration, I'll outline a step-by-step approach based on standard JavaFX docking patterns (e.g., similar to DockFX or PerspectiveFX frameworks). These typically involve creating a dockable node that wraps your custom content (like the `HugoPreviewPane` from my previous outline) and registering it with a docking manager or root pane.

If your framework follows a modular structure (with modules like `papiflyfx-docking-docks`), assume it provides classes like `DockPane`, `DockNode`, or a `DockRegistry` for adding custom dockables. Here's how to integrate:

1. **Define Your Custom Pane**: Start with the `HugoPreviewPane` as a `Node` (e.g., extending `BorderPane` with a `WebView` inside, as sketched before).

2. **Create a Dockable Wrapper**: Wrap it in a framework-specific dockable class. If PapiflyFX uses a `DockNode` or similar:
   - Extend or instantiate a `DockNode` (assuming it exists based on common naming).
   - Set properties like title, icon, content, and docking preferences.

   Example code (adapt to actual API; check your source for exact classes):
   ```java
   import javafx.scene.Node;
   import org.metalib.papiflyfx.docking.DockNode;  // Hypothetical; replace with actual import
   import java.nio.file.Path;

   public class HugoPreviewDockable extends DockNode {  // Or use a factory if provided

       public HugoPreviewDockable(Path hugoPublicDir) {
           super();  // Call super constructor if needed
           setTitle("Hugo Site Preview");
           setContent(new HugoPreviewPane(hugoPublicDir));  // Your custom pane
           setDockable(true);  // Enable docking features
           setMinimizable(true);
           setFloatable(true);
           // Optional: Set ID for persistence, e.g., setId("hugo-preview-dock");
       }
   }
   ```

3. **Register and Add to Docking System**:
   - In your main application or workspace setup, get the root docking pane (e.g., `DockPane root = new DockPane();`).
   - Create and add the dockable instance.
   - For persistence: If PapiflyFX supports JSON sessions, ensure your dockable implements serializable properties (e.g., via `toJson()`/`fromJson()` methods if required).

   Example in app initialization:
   ```java
   import javafx.application.Application;
   import javafx.scene.Scene;
   import javafx.stage.Stage;
   import org.metalib.papiflyfx.docking.DockPane;  // Hypothetical
   import java.nio.file.Paths;

   public class MyDockingApp extends Application {
       @Override
       public void start(Stage primaryStage) {
           DockPane rootDock = new DockPane();  // Or load from JSON session

           // Create and add Hugo dockable
           HugoPreviewDockable hugoDock = new HugoPreviewDockable(Paths.get("/path/to/hugo/public"));
           rootDock.addDockable(hugoDock, DockPosition.CENTER);  // Assuming positions like CENTER, LEFT, etc.

           // Optional: Load/save layout
           // rootDock.loadFromJson("session.json");
           // Add listener: rootDock.layoutProperty().addListener((obs) -> rootDock.saveToJson("session.json"));

           Scene scene = new Scene(rootDock, 800, 600);
           primaryStage.setScene(scene);
           primaryStage.show();
       }
   }
   ```

4. **Handling Dependencies**:
   - Ensure your project includes the PapiflyFX modules (e.g., via Maven/Gradle as per repo build instructions).
   - JavaFX 17+ required (from repo notes).
   - For WebView: Add `javafx-web` module.

If this doesn't match your exact API (e.g., if it's more like a `DockingManager` with `registerDockable()`), dive into the source code in `papiflyfx-docking-docks` module for interfaces like `IDockable`. Let me know repo specifics or errors, and I can refine!

### Research on Hugo CLI Automation

Hugo's CLI is highly automatable, especially the `hugo server` command for live previews. It runs as a standalone process with options for ports, live reload, and background execution—no direct Java API, but easy integration via Java's `ProcessBuilder` to spawn and manage subprocesses. Here's a breakdown with code examples:

#### Key Hugo CLI Insights
- **`hugo server`** starts a local server (default: http://localhost:1313) with file watching and live reload via WebSockets.
- Useful flags:
  - `--port 1313` (or custom port to avoid conflicts).
  - `--watch` (default: true) for auto-rebuild on file changes.
  - `--disableLiveReload` if you don't need browser auto-refresh.
  - `--poll 700ms` for polling-based watching (fallback if native watching fails).
  - `--baseURL http://localhost:1313/` to set the root URL.
  - `--renderToMemory` for faster in-memory rendering (trades RAM for speed).
- Automation: Run as a background subprocess; capture stdout/stderr for logs/errors. Kill the process to stop.

#### Java Integration with ProcessBuilder
Use `ProcessBuilder` to execute `hugo` (assuming it's in PATH; otherwise, provide full path to binary). It handles command args as a list, redirects output, and manages working directories.

Example class to start/stop Hugo server:
```java
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class HugoServerManager {
    private Process hugoProcess;
    private final Path projectDir;  // Hugo project root

    public HugoServerManager(Path projectDir) {
        this.projectDir = projectDir;
    }

    public void startServer(int port) throws IOException {
        List<String> command = Arrays.asList(
            "hugo", "server",
            "--port", String.valueOf(port),
            "--watch",
            "--baseURL", "http://localhost:" + port + "/"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(projectDir.toString()));  // Set working dir to Hugo project
        pb.redirectErrorStream(true);  // Merge stderr into stdout

        hugoProcess = pb.start();

        // Thread to read output (logs)
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(hugoProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Hugo: " + line);  // Or log to UI
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stopServer() {
        if (hugoProcess != null && hugoProcess.isAlive()) {
            hugoProcess.destroy();  // Graceful shutdown
            // Or destroyForcibly() if needed
        }
    }

    // Usage in your app, e.g., in HugoPreviewDockable
    // HugoServerManager manager = new HugoServerManager(Paths.get("/path/to/hugo"));
    // manager.startServer(1313);
    // To load: webEngine.load("http://localhost:1313/");
}
```

- **Error Handling**: Check `hugoProcess.waitFor()` or `exitValue()` after start.
- **Port Conflicts**: Scan for free ports or use a fixed one.
- **Build Command**: Similarly automate `hugo` (without `server`) for static builds: Replace command with `Arrays.asList("hugo", "--minify")`.
- **Integration Tip**: In your dockable, add a toggle button to start/stop the server, then load the URL in WebView. Monitor process health to handle crashes.

This should slot right into your framework—e.g., start the server when the dockable is shown. If you need tweaks for Windows/macOS quirks or more advanced output parsing, share details!
