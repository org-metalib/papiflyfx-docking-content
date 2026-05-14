# papiflyfx-docking-hugo — Implementation Plan

## 1. Executive Summary

This document specifies the design and implementation of `papiflyfx-docking-hugo`, a new Maven module within the `papiflyfx-docking` multi-module project. The module provides a dockable JavaFX component that renders a Hugo-generated website inside an embedded `WebView`, with the Hugo development server (`hugo server`) managed as a child process.

**Key constraints:**
- Java 25 / JavaFX 23.0.1 — no backward compatibility.
- Zero additional dependencies — only `papiflyfx-docking-api`, `javafx-controls`, and `javafx-web`.
- Hugo CLI must be preinstalled on the host system; the component validates availability at runtime.
- No internal (Java-side) web server; the component delegates entirely to Hugo's built-in HTTP server.
- Process lifecycle is fully managed: the component starts `hugo server` when the dock is shown and destroys it when the dock is closed or the application exits.
- External links intercepted and delegated to the system's default browser via `java.awt.Desktop`.

---

## 2. Module Structure

### 2.1 Maven Module

```
papiflyfx-docking-hugo/
├── pom.xml
└── src/
    ├── main/java/org/metalib/papifly/fx/hugo/
    │   ├── HugoPreviewPane.java            ← main UI component
    │   ├── HugoServerManager.java          ← process lifecycle
    │   ├── HugoCliDetector.java            ← CLI availability check
    │   ├── HugoThemeMapper.java            ← Theme → color/hex/rgba adapter
    │   ├── HugoPreviewFactory.java         ← ContentFactory for layout restore
    │   ├── HugoPreviewStateAdapter.java    ← ContentStateAdapter for persistence
    │   ├── HugoPreviewState.java           ← state record
    │   ├── HugoPreviewStateCodec.java      ← Map ↔ record conversion
    │   ├── HugoPreviewToolbar.java         ← navigation / address bar (themed)
    │   └── HugoPreviewStatusBar.java       ← server status, logs, errors (themed)
    └── test/java/org/metalib/papifly/fx/hugo/
        ├── HugoCliDetectorTest.java
        ├── HugoServerManagerTest.java
        ├── HugoPreviewStateCodecTest.java
        └── HugoPreviewPaneFxTest.java
```

### 2.2 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.12-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-hugo</artifactId>
    <name>papiflyfx-docking-hugo</name>
    <description>Hugo SSG preview component for PapiflyFX docking.</description>

    <properties>
        <testfx.headless>false</testfx.headless>
        <testfx.robot>glass</testfx.robot>
        <testfx.platform>Desktop</testfx.platform>
        <monocle.platform>Headless</monocle.platform>
        <prism.order>sw</prism.order>
        <prism.text>t2k</prism.text>
        <java.awt.headless>false</java.awt.headless>
    </properties>

    <dependencies>
        <!-- Compile: API interfaces (DisposableContent, ContentFactory, etc.) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Compile: JavaFX controls (Button, Label, etc.) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
        </dependency>

        <!-- Compile: JavaFX WebView / WebEngine -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-web</artifactId>
        </dependency>

        <!-- Test: docking-docks for DockManager integration tests -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

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
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${maven.surefire.plugin.version}</version>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>
                        --enable-native-access=javafx.graphics
                        --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                    </argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>headless-tests</id>
            <activation>
                <property>
                    <name>testfx.headless</name>
                    <value>true</value>
                </property>
            </activation>
            <dependencies>
                <dependency>
                    <groupId>org.testfx</groupId>
                    <artifactId>openjfx-monocle</artifactId>
                    <version>${monocle.version}</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
        </profile>
    </profiles>
</project>
```

The parent POM in the repo root must be updated to include this module:

```xml
<modules>
    ...
    <module>papiflyfx-docking-hugo</module>
</modules>
```

And a managed dependency for `javafx-web` must be present in the parent `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>${javafx.version}</version>
    <classifier>${javafx.platform}</classifier>
</dependency>
```

---

## 3. Architecture

### 3.1 Component Diagram

```
┌─────────────────────────────── HugoPreviewPane (BorderPane) ──────────────────────────────┐
│ ┌───────────────────────── HugoPreviewToolbar (HBox) ─────────────────────────────────┐   │
│ │  [◀ Back] [▶ Fwd] [↻ Reload] │ http://127.0.0.1:1313/docs/intro │ [🌐 Open] [⚙]  │   │
│ └─────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                           │
│  ┌──────────────────────────── WebView (center) ──────────────────────────────────────┐   │
│  │                                                                                    │   │
│  │              Hugo-rendered HTML content via WebEngine                               │   │
│  │              URL: http://127.0.0.1:<port>/                                         │   │
│  │                                                                                    │   │
│  └────────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                           │
│ ┌───────────────────── HugoPreviewStatusBar (HBox) ──────────────────────────────────┐   │
│ │  ● Hugo server running on port 1313  │  Last build: 42ms  │  Pages: 127            │   │
│ └─────────────────────────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────────────────────────┘

               ▲
               │ manages
               ▼
     ┌─── HugoServerManager ───┐
     │  ProcessBuilder          │
     │  "hugo server"           │──── stdout/stderr → status bar / log
     │  --port=<ephemeral>      │
     │  --bind=127.0.0.1        │
     │  --renderToMemory        │
     │  --buildDrafts           │
     └──────────────────────────┘
```

### 3.2 Lifecycle

```
DockManager.createLeaf("Hugo Preview", hugoPreviewPane)
        │
        ▼
HugoPreviewPane(Path siteRoot)
   │
   ├── HugoCliDetector.detect()  →  validates "hugo" is on PATH
   │        │
   │        ├── success → proceed
   │        └── failure → show error UI ("Hugo CLI not found …")
   │
   ├── HugoServerManager.start(siteRoot, port)
   │        │
   │        ├── ProcessBuilder("hugo", "server", …)
   │        ├── stream gobbler threads (stdout, stderr)
   │        ├── parse stdout for "Web Server is available at http://…"
   │        └── fire ready event → WebView loads URL
   │
   ├── WebView renders Hugo site
   │
   ├── On navigation to external link → delegate to Desktop.browse()
   │
   └── dispose()  →  HugoServerManager.stop()
                          │
                          ├── process.destroy()
                          ├── process.waitFor(5, SECONDS)
                          └── process.destroyForcibly() if still alive
```

### 3.3 Sequence Diagram — Startup

```
User               DockManager        HugoPreviewPane      HugoServerManager     OS
 │                     │                    │                      │              │
 │  open Hugo dock     │                    │                      │              │
 ├────────────────────►│                    │                      │              │
 │                     │  createLeaf(...)   │                      │              │
 │                     ├───────────────────►│                      │              │
 │                     │                    │  detect()            │              │
 │                     │                    ├─────────────────────►│              │
 │                     │                    │                      │ which hugo   │
 │                     │                    │                      ├─────────────►│
 │                     │                    │                      │◄─────────────┤
 │                     │                    │◄─────────────────────┤ OK           │
 │                     │                    │                      │              │
 │                     │                    │  start(dir, port)    │              │
 │                     │                    ├─────────────────────►│              │
 │                     │                    │                      │ hugo server  │
 │                     │                    │                      ├─────────────►│
 │                     │                    │                      │              │
 │                     │                    │  "server ready"      │              │
 │                     │                    │◄─────────────────────┤              │
 │                     │                    │                      │              │
 │                     │                    │ engine.load(url)     │              │
 │                     │                    ├──────────────────────┘              │
 │                     │                    │                                     │
 │                     │  SUCCEEDED         │                                     │
 │◄────────────────────┤◄──────────────────┤                                     │
```

---

## 4. Detailed Class Design

### 4.1 `HugoCliDetector`

Validates that the Hugo binary is accessible on the system PATH. Uses `ProcessBuilder` to run `hugo version` and inspects the exit code and output.

```java
package org.metalib.papifly.fx.hugo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Detects whether the Hugo CLI is installed and accessible on the system PATH.
 */
public final class HugoCliDetector {

    private HugoCliDetector() {}

    /**
     * Checks if Hugo is available.
     * @return the Hugo version string if found, or empty if not available.
     */
    public static Optional<String> detect() {
        return detect("hugo");
    }

    /**
     * Checks if Hugo is available at the given binary path.
     * @param hugoBinary path or name of the Hugo executable
     * @return the Hugo version string if found, or empty if not available.
     */
    public static Optional<String> detect(String hugoBinary) {
        try {
            var pb = new ProcessBuilder(hugoBinary, "version");
            pb.redirectErrorStream(true);
            var process = pb.start();

            String versionLine;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                versionLine = reader.readLine();
            }

            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            if (exited && process.exitValue() == 0 && versionLine != null && !versionLine.isBlank()) {
                return Optional.of(versionLine.trim());
            }
        } catch (Exception _) {
            // hugo not found or not executable
        }
        return Optional.empty();
    }
}
```

### 4.2 `HugoServerManager`

Manages the Hugo development server as a child process. Starts it on an ephemeral port bound to `127.0.0.1`, parses stdout to detect readiness, and provides clean shutdown.

```java
package org.metalib.papifly.fx.hugo;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of a {@code hugo server} child process.
 * <p>
 * The server is started on an ephemeral port bound to 127.0.0.1.
 * Stdout/stderr are consumed in daemon threads to prevent OS buffer
 * saturation and to detect server readiness and build events.
 */
public final class HugoServerManager {

    /** Possible states of the managed Hugo server. */
    public enum State {
        STOPPED, STARTING, RUNNING, ERROR
    }

    private static final Pattern URL_PATTERN =
            Pattern.compile("Web Server is available at (http://[^\\s]+)");

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.STOPPED);
    private final ObjectProperty<String> serverUrl = new SimpleObjectProperty<>();

    private Process process;
    private int port;

    private Consumer<String> stdoutHandler = _ -> {};
    private Consumer<String> stderrHandler = _ -> {};

    public ReadOnlyObjectProperty<State> stateProperty() { return state; }
    public State getState() { return state.get(); }
    public ReadOnlyObjectProperty<String> serverUrlProperty() { return serverUrl; }
    public String getServerUrl() { return serverUrl.get(); }

    public void setStdoutHandler(Consumer<String> handler) { this.stdoutHandler = handler; }
    public void setStderrHandler(Consumer<String> handler) { this.stderrHandler = handler; }

    /**
     * Finds an available ephemeral port.
     */
    private static int findFreePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Starts {@code hugo server} for the given site directory.
     *
     * @param siteDir   the Hugo project root directory
     * @param drafts    whether to include drafts ({@code --buildDrafts})
     * @throws IOException if the process cannot be started
     */
    public void start(Path siteDir, boolean drafts) throws IOException {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("Hugo server is already running");
        }

        port = findFreePort();
        Platform.runLater(() -> state.set(State.STARTING));

        var command = new java.util.ArrayList<>(List.of(
                "hugo", "server",
                "--bind", "127.0.0.1",
                "--port", String.valueOf(port),
                "--renderToMemory",
                "--disableFastRender",
                "--baseURL", "http://127.0.0.1:" + port + "/"
        ));
        if (drafts) {
            command.add("--buildDrafts");
        }

        var pb = new ProcessBuilder(command);
        pb.directory(siteDir.toFile());
        pb.redirectErrorStream(false);

        process = pb.start();

        // stdout gobbler — detect readiness URL and forward log lines
        var stdout = new Thread(() -> gobble(
                process, true, line -> {
                    stdoutHandler.accept(line);
                    var matcher = URL_PATTERN.matcher(line);
                    if (matcher.find()) {
                        var url = "http://127.0.0.1:" + port + "/";
                        Platform.runLater(() -> {
                            serverUrl.set(url);
                            state.set(State.RUNNING);
                        });
                    }
                }), "hugo-stdout-gobbler");
        stdout.setDaemon(true);
        stdout.start();

        // stderr gobbler
        var stderr = new Thread(() -> gobble(
                process, false, stderrHandler),
                "hugo-stderr-gobbler");
        stderr.setDaemon(true);
        stderr.start();

        // process exit watcher
        var exitWatcher = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                Platform.runLater(() -> {
                    if (state.get() != State.STOPPED) {
                        state.set(exitCode == 0 ? State.STOPPED : State.ERROR);
                    }
                });
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }, "hugo-exit-watcher");
        exitWatcher.setDaemon(true);
        exitWatcher.start();
    }

    private void gobble(Process proc, boolean isStdout, Consumer<String> lineConsumer) {
        var stream = isStdout ? proc.getInputStream() : proc.getErrorStream();
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        } catch (IOException _) {
            // stream closed — expected on shutdown
        }
    }

    /**
     * Stops the Hugo server process gracefully.
     */
    public void stop() {
        Platform.runLater(() -> state.set(State.STOPPED));
        if (process == null || !process.isAlive()) return;

        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException _) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    /** Returns the port the Hugo server was started on, or -1 if not started. */
    public int getPort() { return port; }
}
```

### 4.3 `HugoPreviewToolbar`

Provides back/forward/reload buttons, an address label, and an "open in browser" button.

```java
package org.metalib.papifly.fx.hugo;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import org.metalib.papifly.fx.docking.api.Theme;

import java.awt.Desktop;
import java.net.URI;

/**
 * Toolbar for the Hugo preview pane: back, forward, reload, address display,
 * and an "open in system browser" button.
 * <p>
 * Themed via {@link HugoThemeMapper}, following the same property-binding +
 * listener pattern as {@code TransportBar} in papiflyfx-docking-media.
 */
final class HugoPreviewToolbar extends HBox {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final Label addressLabel = new Label();

    HugoPreviewToolbar(WebEngine engine, ObjectProperty<Theme> parentThemeProperty) {
        themeProperty.bind(parentThemeProperty);

        setSpacing(6);
        setPadding(new Insets(4, 8, 4, 8));
        setAlignment(Pos.CENTER_LEFT);

        var backBtn = new Button("◀");
        backBtn.setTooltip(new Tooltip("Back"));
        backBtn.setOnAction(_ -> engine.executeScript("history.back()"));

        var fwdBtn = new Button("▶");
        fwdBtn.setTooltip(new Tooltip("Forward"));
        fwdBtn.setOnAction(_ -> engine.executeScript("history.forward()"));

        var reloadBtn = new Button("↻");
        reloadBtn.setTooltip(new Tooltip("Reload"));
        reloadBtn.setOnAction(_ -> engine.reload());

        addressLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addressLabel, Priority.ALWAYS);

        var openBtn = new Button("🌐");
        openBtn.setTooltip(new Tooltip("Open in system browser"));
        openBtn.setOnAction(_ -> openInSystemBrowser(engine.getLocation()));

        getChildren().addAll(backBtn, fwdBtn, reloadBtn, addressLabel, openBtn);

        // Track address changes
        engine.locationProperty().addListener((_, _, newLoc) -> {
            if (newLoc != null) addressLabel.setText(newLoc);
        });

        // Theme wiring — apply now + react to future changes
        applyTheme(themeProperty.get());
        themeProperty.addListener((_, _, t) -> { if (t != null) applyTheme(t); });
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        // Background — matches DockTabGroup header styling
        var bgColor = HugoThemeMapper.toColor(HugoThemeMapper.toolbarBackground(theme));
        setBackground(new Background(new BackgroundFill(
                bgColor,
                new CornerRadii(theme.cornerRadius(), theme.cornerRadius(), 0, 0, false),
                Insets.EMPTY)));

        // Bottom border
        setBorder(new Border(new BorderStroke(
                HugoThemeMapper.toColor(HugoThemeMapper.border(theme)),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(0, 0, theme.borderWidth(), 0))));

        // Address label
        addressLabel.setFont(theme.contentFont());
        addressLabel.setTextFill(HugoThemeMapper.foreground(theme));

        // Buttons — hover/pressed states via mouse listeners
        var fgHex = HugoThemeMapper.toHex(HugoThemeMapper.foreground(theme));
        var hoverHex = HugoThemeMapper.toHex(HugoThemeMapper.buttonHover(theme));
        var pressedHex = HugoThemeMapper.toHex(HugoThemeMapper.buttonPressed(theme));
        var btnBase = "-fx-text-fill:%s; -fx-background-color:transparent; -fx-cursor:hand; -fx-padding:4 8;"
                .formatted(fgHex);

        for (var node : getChildren()) {
            if (node instanceof Button btn) {
                btn.setFont(theme.headerFont());
                btn.setStyle(btnBase);
                btn.setOnMouseEntered(_ -> btn.setStyle(btnBase + "-fx-background-color:" + hoverHex + ";"));
                btn.setOnMouseExited(_ -> btn.setStyle(btnBase));
                btn.setOnMousePressed(_ -> btn.setStyle(btnBase + "-fx-background-color:" + pressedHex + ";"));
                btn.setOnMouseReleased(_ -> btn.setStyle(btnBase + "-fx-background-color:" + hoverHex + ";"));
            }
        }
    }

    Label getAddressLabel() { return addressLabel; }

    private void openInSystemBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception _) {
            // silently ignore
        }
    }
}
```

### 4.4 `HugoPreviewStatusBar`

Displays server state, last build duration, and recent log messages.

```java
package org.metalib.papifly.fx.hugo;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Status bar at the bottom of the Hugo preview pane.
 * Shows server status, latest log message, and build timing.
 * <p>
 * Themed via {@link HugoThemeMapper}, following the same pattern
 * as {@code MinimizedBar} in papiflyfx-docking-docks.
 */
final class HugoPreviewStatusBar extends HBox {

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final Label statusLabel = new Label("Stopped");
    private final Label messageLabel = new Label();

    HugoPreviewStatusBar(ObjectProperty<Theme> parentThemeProperty) {
        themeProperty.bind(parentThemeProperty);

        setSpacing(12);
        setPadding(new Insets(2, 8, 2, 8));
        setAlignment(Pos.CENTER_LEFT);

        statusLabel.setMinWidth(180);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        getChildren().addAll(statusLabel, messageLabel);

        applyTheme(themeProperty.get());
        themeProperty.addListener((_, _, t) -> { if (t != null) applyTheme(t); });
    }

    void setServerState(HugoServerManager.State state, int port) {
        statusLabel.setText(switch (state) {
            case STOPPED  -> "● Stopped";
            case STARTING -> "◌ Starting …";
            case RUNNING  -> "● Running on port " + port;
            case ERROR    -> "✖ Error";
        });
    }

    void setMessage(String msg) {
        messageLabel.setText(msg);
    }

    private void applyTheme(Theme theme) {
        if (theme == null) return;

        // Background — slightly darker than header for visual separation
        var bgColor = HugoThemeMapper.toColor(HugoThemeMapper.statusBarBackground(theme))
                .deriveColor(0, 1, 0.9, 1);
        setBackground(new Background(new BackgroundFill(
                bgColor, CornerRadii.EMPTY, Insets.EMPTY)));

        // Top border separating status bar from WebView
        setBorder(new Border(new BorderStroke(
                HugoThemeMapper.toColor(HugoThemeMapper.border(theme)),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(theme.borderWidth(), 0, 0, 0))));

        statusLabel.setFont(theme.contentFont());
        statusLabel.setTextFill(HugoThemeMapper.foreground(theme));

        // Message label: slightly muted
        messageLabel.setFont(theme.contentFont());
        messageLabel.setTextFill(
                HugoThemeMapper.toColor(HugoThemeMapper.foreground(theme))
                        .deriveColor(0, 1, 1, 0.7));
    }
}
```

### 4.5 `HugoPreviewPane` — Main Component

The central UI component. Extends `BorderPane`, implements `DisposableContent`. Orchestrates the `WebView`, the `HugoServerManager`, the toolbar, and the status bar.

```java
package org.metalib.papifly.fx.hugo;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Dockable preview pane for a Hugo-generated website.
 * <p>
 * Manages a {@code hugo server} child process and renders its output
 * in a JavaFX {@link WebView}. External navigation is intercepted and
 * delegated to the system's default browser.
 * <p>
 * Theming follows the framework's property-binding + listener pattern:
 * container chrome is styled via {@link HugoThemeMapper}, and the WebView
 * page content receives a minimal CSS override via
 * {@link WebEngine#setUserStyleSheetLocation(String)} to harmonize
 * background and scrollbar colors with the current docking theme.
 */
public class HugoPreviewPane extends BorderPane implements DisposableContent {

    public static final String FACTORY_ID = "hugo-preview";

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>();
    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();

    private final HugoServerManager serverManager = new HugoServerManager();
    private final HugoPreviewToolbar toolbar;
    private final HugoPreviewStatusBar statusBar;

    private Path siteDir;
    private String allowedOrigin;
    private boolean drafts = true;

    /**
     * Creates a Hugo preview pane.
     *
     * @param siteDir path to the Hugo project root directory
     */
    public HugoPreviewPane(Path siteDir) {
        this.siteDir = siteDir;

        toolbar = new HugoPreviewToolbar(engine, themeProperty);
        statusBar = new HugoPreviewStatusBar(themeProperty);

        setTop(toolbar);
        setCenter(webView);
        setBottom(statusBar);

        // Disable context menu on WebView
        webView.setContextMenuEnabled(false);

        // Wire server manager callbacks
        serverManager.setStdoutHandler(line -> Platform.runLater(() -> statusBar.setMessage(line)));
        serverManager.setStderrHandler(line -> Platform.runLater(() -> statusBar.setMessage("ERR: " + line)));

        // Watch server state transitions
        serverManager.stateProperty().addListener((_, _, newState) -> {
            statusBar.setServerState(newState, serverManager.getPort());
            if (newState == HugoServerManager.State.RUNNING) {
                var url = serverManager.getServerUrl();
                if (url != null) {
                    allowedOrigin = "http://127.0.0.1:" + serverManager.getPort();
                    engine.load(url);
                }
            }
        });

        // Track page load completion
        engine.getLoadWorker().stateProperty().addListener((_, _, newState) -> {
            if (newState == Worker.State.FAILED) {
                statusBar.setMessage("Page load failed");
            }
        });

        // Intercept navigation to external origins → open in system browser
        engine.locationProperty().addListener((_, oldLoc, newLoc) -> {
            if (newLoc != null && allowedOrigin != null && !newLoc.startsWith(allowedOrigin)) {
                Platform.runLater(() -> {
                    if (oldLoc != null) {
                        engine.load(oldLoc);
                    }
                    openInSystemBrowser(newLoc);
                });
            }
        });

        // Theme wiring — apply initial + react to changes
        applyTheme(themeProperty.get());
        themeProperty.addListener((_, _, newTheme) -> {
            if (newTheme != null) applyTheme(newTheme);
        });

        // Detect Hugo and start server
        startServer();
    }

    /** No-arg constructor for factory-based restoration. */
    public HugoPreviewPane() {
        this((Path) null);
    }

    /**
     * Binds this pane's theme to an external theme property.
     * Propagates to toolbar, status bar, and WebView page CSS.
     */
    public void bindThemeProperty(ObjectProperty<Theme> external) {
        themeProperty.bind(external);
    }

    /**
     * Sets the Hugo project directory and restarts the server.
     */
    public void setSiteDir(Path siteDir) {
        this.siteDir = siteDir;
        restartServer();
    }

    public Path getSiteDir() { return siteDir; }

    public void setDrafts(boolean drafts) { this.drafts = drafts; }
    public boolean isDrafts() { return drafts; }

    /**
     * Returns the current URL loaded in the WebView.
     */
    public String getCurrentLocation() {
        return engine.getLocation();
    }

    /**
     * Navigates the WebView to a specific path on the Hugo server.
     * @param relativePath path relative to the site root, e.g. "/docs/intro/"
     */
    public void navigateTo(String relativePath) {
        if (allowedOrigin != null && relativePath != null) {
            var path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
            engine.load(allowedOrigin + path);
        }
    }

    /**
     * Returns the full server URL for opening in an external browser.
     */
    public String getServerUrl() {
        return serverManager.getServerUrl();
    }

    /**
     * Captures the current state for session persistence.
     */
    public HugoPreviewState captureState() {
        return new HugoPreviewState(
                siteDir != null ? siteDir.toString() : null,
                extractRelativePath(),
                drafts
        );
    }

    /**
     * Restores state from a persisted record.
     */
    public void applyState(HugoPreviewState state) {
        if (state == null) return;
        this.drafts = state.drafts();
        if (state.siteDir() != null) {
            this.siteDir = Path.of(state.siteDir());
        }
        restartServer();
        // Navigate to the remembered path once the server is up
        if (state.relativePath() != null) {
            serverManager.stateProperty().addListener((_, _, s) -> {
                if (s == HugoServerManager.State.RUNNING) {
                    navigateTo(state.relativePath());
                }
            });
        }
    }

    @Override
    public void dispose() {
        serverManager.stop();
        engine.load("about:blank");
        setCenter(null);
    }

    // ── Theme integration ────────────────────────────────────────────

    /**
     * Applies the current docking theme to the container background and
     * injects a harmonizing CSS user stylesheet into the WebEngine.
     */
    private void applyTheme(Theme theme) {
        if (theme == null) return;

        // Container background (visible behind WebView during load/errors)
        setBackground(new Background(new BackgroundFill(
                HugoThemeMapper.toColor(HugoThemeMapper.containerBackground(theme)),
                CornerRadii.EMPTY, Insets.EMPTY)));

        // Inject CSS into WebView page content
        injectPageTheme(theme);
    }

    /**
     * Injects a minimal user stylesheet into the WebEngine to harmonize
     * the page background with the current docking theme.
     * <p>
     * Uses a data URI to avoid temporary CSS files. Only overrides the
     * root background color and scrollbar styling; Hugo's own CSS is
     * preserved for all other elements.
     * <p>
     * The WebEngine re-applies the user stylesheet to the current page
     * without triggering a full reload, so theme switches are
     * instantaneous and preserve scroll position.
     */
    private void injectPageTheme(Theme theme) {
        var bg     = HugoThemeMapper.toHex(HugoThemeMapper.containerBackground(theme));
        var fg     = HugoThemeMapper.toHex(HugoThemeMapper.foreground(theme));
        var accent = HugoThemeMapper.toHex(HugoThemeMapper.accent(theme));
        var scheme = isDarkTheme(theme) ? "dark" : "light";

        var css = """
                html {
                    background-color: %s !important;
                    color-scheme: %s;
                }
                ::-webkit-scrollbar { width: 10px; height: 10px; }
                ::-webkit-scrollbar-track { background: %s; }
                ::-webkit-scrollbar-thumb { background: %s; border-radius: 5px; }
                ::-webkit-scrollbar-thumb:hover { background: %s; }
                """.formatted(bg, scheme, bg,
                HugoThemeMapper.toRgba(HugoThemeMapper.foreground(theme), 0.3),
                accent);

        var encoded = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        engine.setUserStyleSheetLocation("data:text/css;base64," + encoded);
    }

    /**
     * Heuristic to determine if the current theme is "dark" based on
     * perceived luminance of the background color.
     */
    private static boolean isDarkTheme(Theme theme) {
        var bg = HugoThemeMapper.toColor(HugoThemeMapper.containerBackground(theme));
        return 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue() < 0.5;
    }

    // ── Private helpers ──────────────────────────────────────────────

    private void startServer() {
        if (siteDir == null) {
            showPlaceholder("No Hugo site directory configured.");
            return;
        }

        var hugoVersion = HugoCliDetector.detect();
        if (hugoVersion.isEmpty()) {
            showPlaceholder("Hugo CLI not found on PATH.\n"
                    + "Install Hugo (https://gohugo.io/installation/) and ensure it is on your system PATH.");
            return;
        }

        statusBar.setMessage("Hugo detected: " + hugoVersion.get());

        try {
            serverManager.start(siteDir, drafts);
        } catch (IOException e) {
            showPlaceholder("Failed to start Hugo server:\n" + e.getMessage());
        }
    }

    private void restartServer() {
        serverManager.stop();
        startServer();
    }

    private void showPlaceholder(String message) {
        var label = new Label(message);
        label.setWrapText(true);

        // Apply theme to placeholder
        var theme = themeProperty.get();
        if (theme != null) {
            label.setFont(theme.contentFont());
            label.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.foreground(theme)));
        }
        themeProperty.addListener((_, _, t) -> {
            if (t != null) {
                label.setFont(t.contentFont());
                label.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.foreground(t)));
            }
        });

        var stack = new StackPane(label);
        stack.setPadding(new Insets(24));
        setCenter(stack);
    }

    private String extractRelativePath() {
        var loc = engine.getLocation();
        if (loc == null || allowedOrigin == null || !loc.startsWith(allowedOrigin)) {
            return "/";
        }
        return loc.substring(allowedOrigin.length());
    }

    private void openInSystemBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception _) {
            // silently ignore
        }
    }
}
```

### 4.6 `HugoPreviewState` — State Record

```java
package org.metalib.papifly.fx.hugo;

/**
 * Immutable snapshot of the Hugo preview pane state for session persistence.
 *
 * @param siteDir       absolute path to the Hugo project directory
 * @param relativePath  last viewed page path relative to the site root
 * @param drafts        whether draft content was enabled
 */
public record HugoPreviewState(
        String siteDir,
        String relativePath,
        boolean drafts
) {}
```

### 4.7 `HugoPreviewStateCodec`

Converts between `HugoPreviewState` and `Map<String, Object>` for the docking framework's JSON persistence.

```java
package org.metalib.papifly.fx.hugo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec for converting {@link HugoPreviewState} to/from a
 * {@code Map<String, Object>} for docking session persistence.
 */
public final class HugoPreviewStateCodec {

    static final String KEY_SITE_DIR      = "siteDir";
    static final String KEY_RELATIVE_PATH = "relativePath";
    static final String KEY_DRAFTS        = "drafts";

    private HugoPreviewStateCodec() {}

    public static Map<String, Object> toMap(HugoPreviewState state) {
        var map = new LinkedHashMap<String, Object>();
        if (state.siteDir() != null)      map.put(KEY_SITE_DIR, state.siteDir());
        if (state.relativePath() != null)  map.put(KEY_RELATIVE_PATH, state.relativePath());
        map.put(KEY_DRAFTS, state.drafts());
        return map;
    }

    public static HugoPreviewState fromMap(Map<String, Object> map) {
        if (map == null) return new HugoPreviewState(null, "/", true);
        return new HugoPreviewState(
                (String) map.get(KEY_SITE_DIR),
                (String) map.getOrDefault(KEY_RELATIVE_PATH, "/"),
                map.containsKey(KEY_DRAFTS) ? Boolean.TRUE.equals(map.get(KEY_DRAFTS)) : true
        );
    }
}
```

### 4.8 `HugoPreviewFactory` — ContentFactory

Used by `DockManager` to recreate the Hugo preview pane when restoring a saved layout.

```java
package org.metalib.papifly.fx.hugo;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;

/**
 * Factory that creates {@link HugoPreviewPane} instances during
 * layout restoration. The factory creates an empty pane; actual
 * state (site dir, path) is restored by {@link HugoPreviewStateAdapter}.
 */
public class HugoPreviewFactory implements ContentFactory {

    public static final String FACTORY_ID = HugoPreviewPane.FACTORY_ID;

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) return null;
        return new HugoPreviewPane();
    }
}
```

### 4.9 `HugoPreviewStateAdapter` — ContentStateAdapter

Implements the docking framework's `ContentStateAdapter` contract for persisting and restoring Hugo preview state across sessions.

```java
package org.metalib.papifly.fx.hugo;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;

import java.util.Map;

/**
 * Adapter for persisting and restoring {@link HugoPreviewPane} state
 * within the PapiflyFX docking session model.
 * <p>
 * Register before setting the content factory:
 * <pre>
 *   ContentStateRegistry.register(new HugoPreviewStateAdapter());
 *   dm.setContentFactory(new HugoPreviewFactory());
 * </pre>
 */
public class HugoPreviewStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    @Override
    public String getTypeKey() {
        return HugoPreviewPane.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (!(content instanceof HugoPreviewPane pane)) return Map.of();
        return HugoPreviewStateCodec.toMap(pane.captureState());
    }

    @Override
    public Node restore(LeafContentData content) {
        var pane = new HugoPreviewPane();
        if (content != null && content.state() != null) {
            var state = HugoPreviewStateCodec.fromMap(content.state());
            pane.applyState(state);
        }
        return pane;
    }
}
```

---

## 5. Integration with Existing Framework

### 5.1 Creating a Hugo Dock Leaf

```java
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.hugo.HugoPreviewFactory;
import org.metalib.papifly.fx.hugo.HugoPreviewPane;
import org.metalib.papifly.fx.hugo.HugoPreviewStateAdapter;

import java.nio.file.Path;

// 1. Register state adapter BEFORE setting content factory
var registry = new ContentStateRegistry();
registry.register(new HugoPreviewStateAdapter());
dm.setContentStateRegistry(registry);

// 2. Set content factory (handles layout restoration)
dm.setContentFactory(new HugoPreviewFactory());

// 3. Create the Hugo preview pane
var hugoPane = new HugoPreviewPane(Path.of("/path/to/my/hugo/site"));
hugoPane.bindThemeProperty(dm.themeProperty());

// 4. Create a dock leaf
var leaf = dm.createLeaf("Hugo Preview", hugoPane);
leaf.setContentFactoryId(HugoPreviewPane.FACTORY_ID);
leaf.setContentData(LeafContentData.of(
        HugoPreviewPane.FACTORY_ID,
        "docs-site",
        HugoPreviewStateAdapter.VERSION
));

// 5. Add to layout
var tabGroup = dm.createTabGroup();
tabGroup.addLeaf(leaf);
dm.setRoot((DockElement) tabGroup);
```

### 5.2 Adding to the Samples Module

A new sample class can be added to `papiflyfx-docking-samples`:

```java
package org.metalib.papifly.fx.samples.hugo;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockElement;
import org.metalib.papifly.fx.hugo.HugoPreviewPane;
import org.metalib.papifly.fx.samples.SampleScene;

import java.nio.file.Path;

public class HugoPreviewSample implements SampleScene {

    @Override
    public String category() { return "Hugo"; }

    @Override
    public String title() { return "Hugo Preview"; }

    @Override
    public Node build(Stage ownerStage, ObjectProperty<Theme> themeProperty) {
        var dm = new DockManager();
        dm.themeProperty().bind(themeProperty);
        dm.setOwnerStage(ownerStage);

        var hugoPane = new HugoPreviewPane(Path.of(System.getProperty("user.home") + "/my-hugo-site"));
        hugoPane.bindThemeProperty(dm.themeProperty());

        var leaf = dm.createLeaf("Hugo Preview", hugoPane);
        var tabGroup = dm.createTabGroup();
        tabGroup.addLeaf(leaf);
        dm.setRoot((DockElement) tabGroup);

        return dm.getRootPane();
    }
}
```

---

## 6. Hugo CLI Integration Details

### 6.1 Hugo Server Command

The component invokes `hugo server` with the following flags:

| Flag | Purpose |
|---|---|
| `--bind 127.0.0.1` | Bind to loopback only — prevents network exposure |
| `--port <ephemeral>` | Use a dynamically allocated port to avoid conflicts |
| `--renderToMemory` | Skip disk I/O for maximum performance |
| `--disableFastRender` | Full re-render on every change for correctness |
| `--baseURL http://127.0.0.1:<port>/` | Ensure generated links match the bound address |
| `--buildDrafts` | (optional) Include draft content during development |

### 6.2 Readiness Detection

Hugo outputs a line like:

```
Web Server is available at http://127.0.0.1:1313/ (bind address 127.0.0.1)
```

The `HugoServerManager` parses stdout with a regex pattern matching `Web Server is available at (http://[^\s]+)` and transitions to `RUNNING` state when this line appears.

### 6.3 Live Reload

Hugo's built-in server injects a LiveReload JavaScript snippet into every served page. When source files change, Hugo rebuilds affected pages and sends a WebSocket message to connected browsers. The JavaFX `WebView` supports WebSockets natively, so **live reload works automatically with zero additional code**.

If WebView's WebSocket support proves unreliable for any reason, a fallback "poll + reload" strategy can be added:

```java
// Fallback: poll the Hugo server and reload WebView periodically
var timeline = new Timeline(new KeyFrame(Duration.seconds(2), _ -> {
    engine.reload();
}));
timeline.setCycleCount(Animation.INDEFINITE);
timeline.play();
```

### 6.4 CORS: Non-Issue with Hugo Server

Because the component loads content from `http://127.0.0.1:<port>` rather than `file://`, all same-origin restrictions are naturally satisfied. There is no need for:
- `sun.net.http.allowRestrictedHeaders` hacks
- Custom CORS headers
- Embedded Java web servers

This is the primary architectural advantage of delegating to Hugo's built-in server.

---

## 7. Navigation Sandboxing

### 7.1 External Link Interception

All navigation is monitored via `engine.locationProperty()`. If the destination URL does not match the allowed origin (`http://127.0.0.1:<port>`), the navigation is cancelled and the URL is opened in the system browser:

```java
engine.locationProperty().addListener((_, oldLoc, newLoc) -> {
    if (newLoc != null && allowedOrigin != null && !newLoc.startsWith(allowedOrigin)) {
        Platform.runLater(() -> engine.load(oldLoc));
        openInSystemBrowser(newLoc);
    }
});
```

### 7.2 System Browser Delegation

Uses `java.awt.Desktop` — available on all supported platforms without additional dependencies:

```java
private void openInSystemBrowser(String url) {
    try {
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        }
    } catch (Exception _) { /* ignore */ }
}
```

---

## 8. Docking Themes Integration

The Hugo preview component integrates with the PapiflyFX `Theme` record using the same **property-binding + listener** pattern established by existing modules (`papiflyfx-docking-docks`, `papiflyfx-docking-media`). Theming operates at three layers:

1. **Container chrome** — toolbar, status bar, placeholder backgrounds and text
2. **WebView container** — `BorderPane` background behind the WebView (visible during loading/errors)
3. **WebView page content** — Hugo-rendered HTML, themed via CSS injection into the WebEngine

### 8.1 Theme Property Wiring Pattern

Following the framework convention (identical to `DockManager` → `MinimizedBar` → child controls), the theme property flows top-down through the component hierarchy:

```
DockManager.themeProperty()
     │ bind()
     ▼
HugoPreviewPane.themeProperty
     │ bind()              │ bind()
     ▼                     ▼
HugoPreviewToolbar     HugoPreviewStatusBar
  .themeProperty         .themeProperty
```

Every component follows the established two-step pattern:

```java
// 1. Apply initial theme
applyTheme(themeProperty.get());

// 2. React to future changes
themeProperty.addListener((_, _, newTheme) -> {
    if (newTheme != null) applyTheme(newTheme);
});
```

### 8.2 `HugoThemeMapper` — Theme Adapter

A dedicated mapper translates the framework's `Theme` properties into values used by the Hugo preview sub-components. This mirrors the `MediaThemeMapper` from `papiflyfx-docking-media`.

```java
package org.metalib.papifly.fx.hugo;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Maps PapiflyFX {@link Theme} properties to Hugo preview component styling values.
 * Follows the same adapter pattern as {@code MediaThemeMapper} in the media module.
 */
final class HugoThemeMapper {

    private HugoThemeMapper() {}

    static Paint toolbarBackground(Theme t) { return t.headerBackground(); }
    static Paint statusBarBackground(Theme t) { return t.headerBackground(); }
    static Paint foreground(Theme t)          { return t.textColor(); }
    static Paint accent(Theme t)              { return t.accentColor(); }
    static Paint border(Theme t)              { return t.borderColor(); }
    static Paint containerBackground(Theme t) { return t.background(); }
    static Paint buttonHover(Theme t)         { return t.buttonHoverBackground(); }
    static Paint buttonPressed(Theme t)       { return t.buttonPressedBackground(); }

    static Color toColor(Paint p) {
        return p instanceof Color c ? c : Color.GRAY;
    }

    static String toHex(Paint p) {
        var c = toColor(p);
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    static String toRgba(Paint p, double alpha) {
        var c = toColor(p);
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255),
                alpha);
    }
}
```

### 8.3 Container-Level Theming (Toolbar, Status Bar, Placeholder)

The toolbar and status bar apply theme colors via the `Background` / `BackgroundFill` API (consistent with `TransportBar` and `MinimizedBar` in the existing codebase) rather than raw style strings, leveraging proper `CornerRadii` and `Insets` from the `Theme` record.

**HugoPreviewToolbar — themed `applyTheme`:**

```java
private void applyTheme(Theme theme) {
    if (theme == null) return;

    // Toolbar background — matches DockTabGroup header styling
    var bgColor = HugoThemeMapper.toColor(HugoThemeMapper.toolbarBackground(theme));
    setBackground(new Background(new BackgroundFill(
            bgColor, new CornerRadii(theme.cornerRadius(), theme.cornerRadius(), 0, 0, false),
            Insets.EMPTY)));

    // Border bottom
    setBorder(new Border(new BorderStroke(
            HugoThemeMapper.toColor(HugoThemeMapper.border(theme)),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(0, 0, theme.borderWidth(), 0))));

    // Address label
    addressLabel.setFont(theme.contentFont());
    addressLabel.setTextFill(HugoThemeMapper.foreground(theme));

    // Navigation buttons — styled to match framework button conventions
    var fgHex = HugoThemeMapper.toHex(HugoThemeMapper.foreground(theme));
    var hoverHex = HugoThemeMapper.toHex(HugoThemeMapper.buttonHover(theme));
    var pressedHex = HugoThemeMapper.toHex(HugoThemeMapper.buttonPressed(theme));
    var btnStyle = """
            -fx-text-fill: %s;
            -fx-background-color: transparent;
            -fx-cursor: hand;
            -fx-padding: 4 8;
            """.formatted(fgHex);

    for (var node : getChildren()) {
        if (node instanceof Button btn) {
            btn.setFont(theme.headerFont());
            btn.setStyle(btnStyle);
            btn.setOnMouseEntered(_ -> btn.setStyle(btnStyle
                    + "-fx-background-color: " + hoverHex + ";"));
            btn.setOnMouseExited(_ -> btn.setStyle(btnStyle));
            btn.setOnMousePressed(_ -> btn.setStyle(btnStyle
                    + "-fx-background-color: " + pressedHex + ";"));
            btn.setOnMouseReleased(_ -> btn.setStyle(btnStyle
                    + "-fx-background-color: " + hoverHex + ";"));
        }
    }
}
```

**HugoPreviewStatusBar — themed `applyTheme`:**

```java
private void applyTheme(Theme theme) {
    if (theme == null) return;

    var bgColor = HugoThemeMapper.toColor(HugoThemeMapper.statusBarBackground(theme));
    setBackground(new Background(new BackgroundFill(
            bgColor.deriveColor(0, 1, 0.9, 1),  // slightly darker than header
            CornerRadii.EMPTY, Insets.EMPTY)));

    setBorder(new Border(new BorderStroke(
            HugoThemeMapper.toColor(HugoThemeMapper.border(theme)),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(theme.borderWidth(), 0, 0, 0))));

    statusLabel.setFont(theme.contentFont());
    statusLabel.setTextFill(HugoThemeMapper.foreground(theme));

    messageLabel.setFont(theme.contentFont());
    messageLabel.setTextFill(HugoThemeMapper.toColor(
            HugoThemeMapper.foreground(theme)).deriveColor(0, 1, 1, 0.7));
}
```

**HugoPreviewPane — container background:**

```java
private void applyTheme(Theme theme) {
    if (theme == null) return;

    // Set the pane background (visible behind WebView during loading)
    setBackground(new Background(new BackgroundFill(
            HugoThemeMapper.toColor(HugoThemeMapper.containerBackground(theme)),
            CornerRadii.EMPTY, Insets.EMPTY)));

    // Inject CSS override into the WebView page content
    injectPageTheme(theme);
}
```

### 8.4 WebView Page Content Theming via CSS Injection

Hugo sites ship their own CSS, but the surrounding chrome (scrollbar style, default page background, loading flash) can be harmonized with the docking theme. The component injects a minimal CSS override into the WebEngine using `setUserStyleSheetLocation()` with a data URI.

This technique ensures the page background matches the framework background during load transitions and prevents a white flash when the docking theme is dark.

```java
/**
 * Injects a minimal user stylesheet into the WebEngine to harmonize
 * the page background with the current docking theme.
 * <p>
 * Uses a data URI to avoid writing temporary CSS files.
 * Only overrides the root background and scrollbar colors;
 * all other Hugo CSS is preserved.
 */
private void injectPageTheme(Theme theme) {
    var bg = HugoThemeMapper.toHex(HugoThemeMapper.containerBackground(theme));
    var fg = HugoThemeMapper.toHex(HugoThemeMapper.foreground(theme));
    var accent = HugoThemeMapper.toHex(HugoThemeMapper.accent(theme));

    // Minimal override: only root element background and scrollbar track
    var css = """
            html {
                background-color: %s !important;
                color-scheme: %s;
            }
            ::-webkit-scrollbar {
                width: 10px;
                height: 10px;
            }
            ::-webkit-scrollbar-track {
                background: %s;
            }
            ::-webkit-scrollbar-thumb {
                background: %s;
                border-radius: 5px;
            }
            ::-webkit-scrollbar-thumb:hover {
                background: %s;
            }
            """.formatted(bg, isDarkTheme(theme) ? "dark" : "light",
                    bg, HugoThemeMapper.toRgba(HugoThemeMapper.foreground(theme), 0.3),
                    accent);

    // Encode as data URI — avoids temp file I/O
    var encoded = java.util.Base64.getEncoder().encodeToString(css.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    engine.setUserStyleSheetLocation("data:text/css;base64," + encoded);
}

/**
 * Heuristic to determine if the current theme is "dark".
 * Uses perceived luminance of the background color.
 */
private static boolean isDarkTheme(Theme theme) {
    var bg = HugoThemeMapper.toColor(HugoThemeMapper.containerBackground(theme));
    // Relative luminance: 0.299R + 0.587G + 0.114B
    var luminance = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
    return luminance < 0.5;
}
```

### 8.5 Theme Lifecycle and Reactive Updates

The component reacts to theme changes at any time — including after the Hugo site is already loaded:

```java
// In HugoPreviewPane constructor:
themeProperty.addListener((_, _, newTheme) -> {
    if (newTheme != null) applyTheme(newTheme);
});
```

When the theme changes:
1. **Toolbar + status bar** update immediately (JavaFX property bindings).
2. **Container background** updates immediately.
3. **WebView page CSS** updates via `setUserStyleSheetLocation()` — the WebEngine re-applies the user stylesheet to the current page without triggering a full reload.

This means switching between `Theme.dark()` and `Theme.light()` is instantaneous and does not interrupt the user's browsing position or scroll state.

### 8.6 Theme Integration in the Placeholder View

When Hugo is not installed or the site directory is not configured, a placeholder `Label` is shown instead of the `WebView`. The placeholder also respects the theme:

```java
private void showPlaceholder(String message) {
    var label = new Label(message);
    label.setWrapText(true);

    // Apply theme to placeholder
    var theme = themeProperty.get();
    if (theme != null) {
        label.setFont(theme.contentFont());
        label.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.foreground(theme)));
    }
    // React to future theme changes
    themeProperty.addListener((_, _, t) -> {
        if (t != null) {
            label.setFont(t.contentFont());
            label.setTextFill(HugoThemeMapper.toColor(HugoThemeMapper.foreground(t)));
        }
    });

    var stack = new StackPane(label);
    stack.setPadding(new Insets(24));
    setCenter(stack);
}
```

### 8.7 Updated File Structure (with Theme Additions)

```
papiflyfx-docking-hugo/
└── src/main/java/org/metalib/papifly/fx/hugo/
    ├── HugoPreviewPane.java            ← main component (applyTheme + injectPageTheme)
    ├── HugoServerManager.java          ← process lifecycle (no theme)
    ├── HugoCliDetector.java            ← CLI detection (no theme)
    ├── HugoThemeMapper.java            ← NEW: Theme → color/hex/rgba adapter
    ├── HugoPreviewFactory.java         ← ContentFactory
    ├── HugoPreviewStateAdapter.java    ← ContentStateAdapter
    ├── HugoPreviewState.java           ← state record
    ├── HugoPreviewStateCodec.java      ← Map ↔ record codec
    ├── HugoPreviewToolbar.java         ← navigation bar (themed)
    └── HugoPreviewStatusBar.java       ← server status (themed)
```

### 8.8 Theme Wiring Summary

| Component | Theme source | What is themed |
|---|---|---|
| `HugoPreviewPane` | `themeProperty` (bound to `DockManager`) | Container background, WebView user stylesheet |
| `HugoPreviewToolbar` | `themeProperty` (bound from parent) | Background, border, button colors/hover/pressed, address label font/color |
| `HugoPreviewStatusBar` | `themeProperty` (bound from parent) | Background, border, label fonts/colors |
| `HugoThemeMapper` | Stateless utility | Translates `Theme` → `Color`, hex, rgba strings |
| WebView page content | `engine.setUserStyleSheetLocation()` | Root background, `color-scheme`, scrollbar track/thumb |
| Placeholder label | `themeProperty` listener | Font, text fill |

### 8.9 Dark / Light Theme Behavior

| Element | `Theme.dark()` | `Theme.light()` |
|---|---|---|
| Toolbar background | Dark header color | Light header color |
| Button text | Light foreground | Dark foreground |
| Button hover | Subtle lighter highlight | Subtle darker highlight |
| Address label | Light text on dark bg | Dark text on light bg |
| Status bar | Slightly darker than header | Slightly darker than header |
| WebView `html` background | Dark (matches container) | Light (matches container) |
| WebView `color-scheme` | `dark` (affects form controls, scrollbars) | `light` |
| Scrollbar thumb | Semi-transparent foreground | Semi-transparent foreground |
| Placeholder text | Light on dark | Dark on light |

---

## 9. State Persistence

### 9.1 Persisted Fields

| Field | Type | Description |
|---|---|---|
| `siteDir` | `String` | Absolute path to Hugo project root |
| `relativePath` | `String` | Last viewed page (e.g., `/docs/intro/`) |
| `drafts` | `boolean` | Whether `--buildDrafts` was enabled |

### 9.2 Session JSON Example

When `DockManager.capture()` is called, the Hugo leaf serializes as:

```json
{
  "type": "leaf",
  "title": "Hugo Preview",
  "contentFactoryId": "hugo-preview",
  "contentData": {
    "typeKey": "hugo-preview",
    "contentId": "docs-site",
    "version": 1,
    "state": {
      "siteDir": "/Users/dev/my-hugo-site",
      "relativePath": "/docs/getting-started/",
      "drafts": true
    }
  }
}
```

### 9.3 Restore Flow

1. `DockManager` rebuilds the layout from JSON.
2. For the Hugo leaf, it finds a registered `ContentStateAdapter` with `typeKey = "hugo-preview"`.
3. `HugoPreviewStateAdapter.restore()` creates a new `HugoPreviewPane` and calls `applyState()`.
4. `applyState()` sets the `siteDir`, starts the Hugo server, and (once running) navigates to the remembered `relativePath`.

---

## 10. Resource Cleanup and Disposal

### 10.1 Dispose Contract

`HugoPreviewPane` implements `DisposableContent`. When the dock leaf is closed, `DockLeaf.dispose()` calls `DisposableContent.dispose()` on the content node:

```java
@Override
public void dispose() {
    // 1. Stop the Hugo server process
    serverManager.stop();

    // 2. Clear the WebView DOM to release native WebKit memory
    engine.load("about:blank");

    // 3. Remove the WebView from the scene graph
    setCenter(null);
}
```

### 10.2 Application Shutdown

A shutdown hook should be registered at the application level to ensure the Hugo process is terminated even on abrupt exit:

```java
// In the application's start() method:
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    serverManager.stop();
}));
```

### 10.3 Process Cleanup Strategy

```
destroy()
   │
   ├── waitFor(5 seconds)
   │       │
   │       ├── exited → done
   │       └── still alive → destroyForcibly()
   │                           │
   │                           └── waitFor(3 seconds) → done
   └── InterruptedException → destroyForcibly() + re-interrupt
```

---

## 11. Error Handling

### 11.1 Hugo Not Installed

If `HugoCliDetector.detect()` returns empty, the pane shows a placeholder label:

```
Hugo CLI not found on PATH.
Install Hugo (https://gohugo.io/installation/) and ensure it is on your system PATH.
```

### 11.2 Server Start Failure

If `ProcessBuilder.start()` throws, or the process exits with a non-zero code before printing the readiness URL, the status bar shows the error and the placeholder is displayed.

### 11.3 Page Load Failure

`WebEngine.getLoadWorker().stateProperty()` is monitored. On `FAILED` state, the status bar reports the failure. The user can click "Reload" to retry.

### 11.4 Stream Buffer Saturation Prevention

Both stdout and stderr are consumed in dedicated daemon threads (`gobble()`) to prevent the OS from blocking the Hugo process when its output buffers fill up. This is critical for long-running Hugo server sessions that produce continuous output.

---

## 12. Testing Strategy

### 12.1 Unit Tests (No UI)

| Test class | What it tests |
|---|---|
| `HugoCliDetectorTest` | Detection with mock process; missing binary handling |
| `HugoPreviewStateCodecTest` | Round-trip `toMap()` → `fromMap()` for all field combinations |

Example:

```java
@Test
void roundTrip() {
    var state = new HugoPreviewState("/path/to/site", "/docs/intro/", true);
    var map = HugoPreviewStateCodec.toMap(state);
    var restored = HugoPreviewStateCodec.fromMap(map);

    assertEquals(state.siteDir(), restored.siteDir());
    assertEquals(state.relativePath(), restored.relativePath());
    assertEquals(state.drafts(), restored.drafts());
}

@Test
void fromNullMap() {
    var state = HugoPreviewStateCodec.fromMap(null);
    assertNull(state.siteDir());
    assertEquals("/", state.relativePath());
    assertTrue(state.drafts());
}
```

### 12.2 Integration Tests (Requires Hugo on PATH)

```java
@Test
@EnabledIf("hugoAvailable")
void serverStartsAndStops() throws Exception {
    var manager = new HugoServerManager();
    var siteDir = Path.of("src/test/resources/test-hugo-site");

    manager.start(siteDir, false);

    // Wait for RUNNING state (with timeout)
    await().atMost(Duration.ofSeconds(30))
           .until(() -> manager.getState() == HugoServerManager.State.RUNNING);

    assertNotNull(manager.getServerUrl());
    assertTrue(manager.getServerUrl().startsWith("http://127.0.0.1:"));

    manager.stop();
    assertEquals(HugoServerManager.State.STOPPED, manager.getState());
}

static boolean hugoAvailable() {
    return HugoCliDetector.detect().isPresent();
}
```

### 12.3 UI Tests (TestFX, Headless)

```java
@ExtendWith(ApplicationExtension.class)
class HugoPreviewPaneFxTest {

    @Start
    void start(Stage stage) {
        // Use a test Hugo site or mock
        var pane = new HugoPreviewPane(Path.of("src/test/resources/test-hugo-site"));
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    void toolbarIsVisible(FxRobot robot) {
        assertNotNull(robot.lookup(".button").queryAll());
    }
}
```

Run headless: `./mvnw -pl papiflyfx-docking-hugo -am -Dtestfx.headless=true test`

---

## 13. Threading Model

| Thread | Responsibility |
|---|---|
| **JavaFX Application Thread** | All `WebView`/`WebEngine` calls, UI updates, property listeners |
| **hugo-stdout-gobbler** (daemon) | Reads Hugo process stdout; marshals UI updates via `Platform.runLater()` |
| **hugo-stderr-gobbler** (daemon) | Reads Hugo process stderr; marshals UI updates via `Platform.runLater()` |
| **hugo-exit-watcher** (daemon) | Calls `process.waitFor()` and updates state on exit |

All property updates (`state`, `serverUrl`) use `Platform.runLater()` to ensure JavaFX thread safety. Background threads are daemon threads so they do not prevent JVM shutdown.

---

## 14. Security Considerations

### 14.1 Network Binding

The Hugo server is bound to `127.0.0.1` only. It is **not** accessible from other machines on the network.

### 14.2 Navigation Sandboxing

External URLs are intercepted and opened in the system browser. The WebView never loads remote content.

### 14.3 No JS ↔ Java Bridge

This component does **not** inject a Java object into the WebView's JavaScript context. There is no `JSObject.setMember()` call. This eliminates the entire class of bridge-related security risks (code injection, unauthorized native access).

The only interaction between Java and the web content is:
- `engine.load(url)` — Java → WebView
- `engine.getLocation()` — WebView → Java (read-only)
- `engine.reload()` — Java → WebView
- `engine.executeScript("history.back()")` / `"history.forward()"` — Java → WebView

### 14.4 Process Isolation

Hugo runs as a separate OS process. A crash in Hugo does not affect the JVM. The exit-watcher thread detects unexpected termination and updates the UI state.

---

## 15. Open Design Decisions

| Decision | Options | Recommendation |
|---|---|---|
| **Hugo binary location** | Hard-coded `"hugo"` on PATH vs. configurable path | Start with PATH lookup; add `setHugoBinary(String)` for power users |
| **Multiple Hugo docks** | One server per dock vs. shared server | One server per dock — simpler lifecycle, avoids cross-dock coupling |
| **Build-only mode** | Always `hugo server` vs. optional `hugo build` + `file://` | `hugo server` only — avoids `file://` CORS issues entirely |
| **Log pane** | Inline status bar vs. dedicated log dock | Status bar for v1; dedicated log dock can be added later |
| **Port allocation** | Ephemeral (ServerSocket trick) vs. user-configured | Ephemeral by default; add `setPort(int)` for advanced use |

---

## 16. Implementation Order

1. **`HugoCliDetector`** — no UI, easy to unit test
2. **`HugoServerManager`** — process lifecycle, integration-testable
3. **`HugoPreviewState` + `HugoPreviewStateCodec`** — pure data, unit-testable
4. **`HugoThemeMapper`** — stateless theme adapter, unit-testable
5. **`HugoPreviewToolbar` + `HugoPreviewStatusBar`** — themed UI building blocks
6. **`HugoPreviewPane`** — assemble the full component with theme + CSS injection
7. **`HugoPreviewFactory` + `HugoPreviewStateAdapter`** — docking integration
8. **Maven module setup** — POM, parent module list update
9. **Sample integration** — add to `papiflyfx-docking-samples`
10. **Tests** — unit, integration, UI

---

## 17. Summary

The `papiflyfx-docking-hugo` module provides a zero-dependency, self-contained Hugo preview dock for the PapiflyFX docking framework. It delegates all web serving to Hugo's built-in server (started/stopped as a managed child process), renders the output in a standard JavaFX `WebView`, and integrates cleanly with the framework's theming, session persistence, and lifecycle management. External links are safely delegated to the system browser, and the component validates Hugo CLI availability at runtime with clear error messages when Hugo is not installed.
