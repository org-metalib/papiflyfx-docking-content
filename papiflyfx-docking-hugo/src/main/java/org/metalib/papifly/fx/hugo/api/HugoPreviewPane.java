package org.metalib.papifly.fx.hugo.api;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.hugo.process.HugoCliProbe;
import org.metalib.papifly.fx.hugo.process.HugoServerOptions;
import org.metalib.papifly.fx.hugo.process.HugoServerProcessManager;
import org.metalib.papifly.fx.hugo.web.UrlPolicy;
import org.metalib.papifly.fx.hugo.web.WebViewNavigator;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class HugoPreviewPane extends BorderPane implements DisposableContent, HugoRibbonActions {

    private final HugoPreviewConfig config;
    private final HugoCliProbe cliProbe;
    private final HugoServerProcessManager processManager;

    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private final WebViewNavigator navigator = new WebViewNavigator(webEngine);
    private final HugoPreviewToolbar toolbar;
    private final HugoPreviewStatusBar statusBar = new HugoPreviewStatusBar();
    private final Label placeholderLabel = new Label();
    private final StackPane contentStack = new StackPane();

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.dark());

    private final ExecutorService lifecycleExecutor;

    private ChangeListener<Theme> themeChangeListener;
    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<String> locationChangeListener;
    private ChangeListener<Worker.State> loadStateListener;

    private volatile boolean cliAvailable;
    private volatile boolean disposed;
    private volatile String lastRelativePath = "/";
    private volatile boolean drafts = true;
    private volatile boolean suppressExternalGuard;
    private volatile Path currentSiteRoot;

    private final Thread shutdownHook;

    public HugoPreviewPane(HugoPreviewConfig config) {
        this(config, new HugoCliProbe(), new HugoServerProcessManager());
    }

    HugoPreviewPane(HugoPreviewConfig config, HugoCliProbe cliProbe, HugoServerProcessManager processManager) {
        this.config = config;
        this.cliProbe = cliProbe;
        this.processManager = processManager;
        this.currentSiteRoot = config.siteRoot();

        AtomicInteger threadId = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "hugo-preview-lifecycle-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.lifecycleExecutor = Executors.newSingleThreadExecutor(threadFactory);

        this.toolbar = new HugoPreviewToolbar(
            () -> startServerAndLoad(lastRelativePath),
            this::stopServer,
            navigator::back,
            navigator::forward,
            navigator::reload,
            () -> openInBrowser(navigator.currentLocation())
        );

        webView.setId("hugo-preview-webview");
        webView.setContextMenuEnabled(false);
        placeholderLabel.setId("hugo-preview-placeholder");
        placeholderLabel.setVisible(false);

        contentStack.getChildren().setAll(webView, placeholderLabel);

        setTop(toolbar);
        setCenter(contentStack);
        setBottom(statusBar);

        bindEngineSignals();
        processManager.setLogListener(line -> runOnFx(() -> statusBar.setMessage(line)));

        applyPreviewChrome();

        shutdownHook = new Thread(() -> {
            try {
                processManager.stop();
            } catch (Exception ignored) {
            }
        }, "hugo-preview-shutdown");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
        }

        cliAvailable = cliProbe.isAvailable(Duration.ofSeconds(3));
        if (!cliAvailable) {
            setServerState(HugoServerProcessManager.State.ERROR, -1);
            statusBar.setMessage("Hugo CLI is not available in PATH");
            showPlaceholder("Hugo CLI is not available in PATH");
            return;
        }

        if (config.autoStart()) {
            startServerAndLoad(config.basePath());
        } else {
            setServerState(HugoServerProcessManager.State.STOPPED, -1);
            showPlaceholder("Preview stopped");
        }
    }

    public void startServerAndLoad(String relativePath) {
        if (disposed) {
            return;
        }
        String normalizedPath = normalizeRelativePath(relativePath);
        lastRelativePath = normalizedPath;

        if (!cliAvailable) {
            setServerState(HugoServerProcessManager.State.ERROR, -1);
            statusBar.setMessage("Hugo CLI is not available in PATH");
            showPlaceholder("Hugo CLI is not available in PATH");
            return;
        }

        if (currentSiteRoot == null || !Files.isDirectory(currentSiteRoot)) {
            setServerState(HugoServerProcessManager.State.ERROR, -1);
            statusBar.setMessage("Invalid Hugo site root");
            showPlaceholder("Invalid Hugo site root");
            return;
        }

        setServerState(HugoServerProcessManager.State.STARTING, -1);
        statusBar.setMessage("Starting Hugo server");
        hidePlaceholder();

        lifecycleExecutor.submit(() -> {
            try {
                HugoServerOptions options = new HugoServerOptions(
                    "hugo",
                    config.preferredPort(),
                    "127.0.0.1",
                    true,
                    false,
                    drafts
                );
                URI endpoint = processManager.start(currentSiteRoot, options);
                URI target = endpoint.resolve(normalizedPath);
                runOnFx(() -> {
                    if (disposed) {
                        return;
                    }
                    setServerState(HugoServerProcessManager.State.RUNNING, processManager.getBoundPort());
                    statusBar.setMessage("Running: " + endpoint);
                    hidePlaceholder();
                    navigator.navigate(target.toString());
                });
            } catch (Exception ex) {
                runOnFx(() -> {
                    if (disposed) {
                        return;
                    }
                    setServerState(HugoServerProcessManager.State.ERROR, -1);
                    statusBar.setMessage("Failed to start Hugo server: " + ex.getMessage());
                    showPlaceholder("Failed to start Hugo server");
                });
            }
        });
    }

    public void stopServer() {
        lifecycleExecutor.submit(() -> {
            processManager.stop();
            runOnFx(() -> {
                if (disposed) {
                    return;
                }
                setServerState(HugoServerProcessManager.State.STOPPED, -1);
                statusBar.setMessage("Stopped");
                navigator.navigate("about:blank");
                showPlaceholder("Preview stopped");
            });
        });
    }

    public HugoPreviewState captureState() {
        String siteDir = currentSiteRoot == null ? null : currentSiteRoot.toAbsolutePath().normalize().toString();
        return new HugoPreviewState(siteDir, currentRelativePath(), drafts);
    }

    public void applyState(HugoPreviewState state) {
        if (state == null) {
            return;
        }
        if (state.siteDir() != null && !state.siteDir().isBlank()) {
            try {
                currentSiteRoot = Path.of(state.siteDir()).toAbsolutePath().normalize();
            } catch (Exception ignored) {
            }
        }
        drafts = state.drafts();
        lastRelativePath = normalizeRelativePath(state.relativePath());
        startServerAndLoad(lastRelativePath);
    }

    public void navigateTo(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        lastRelativePath = normalized;
        if (processManager.isRunning()) {
            URI target = processManager.endpoint().resolve(normalized);
            navigator.navigate(target.toString());
            return;
        }
        startServerAndLoad(normalized);
    }

    public void showPlaceholder(String message) {
        placeholderLabel.setText(message == null || message.isBlank() ? "Preview unavailable" : message);
        placeholderLabel.setVisible(true);
    }

    public String currentRelativePath() {
        return lastRelativePath;
    }

    public HugoPreviewConfig getConfig() {
        return config;
    }

    public Path getCurrentSiteRoot() {
        return currentSiteRoot;
    }

    public boolean isDrafts() {
        return drafts;
    }

    @Override
    public boolean isServerRunning() {
        return processManager.isRunning();
    }

    @Override
    public boolean canRunHugoCommands() {
        return !disposed && cliAvailable && currentSiteRoot != null && Files.isDirectory(currentSiteRoot);
    }

    @Override
    public void toggleServer() {
        if (isServerRunning()) {
            stopServer();
        } else {
            startServerAndLoad(lastRelativePath);
        }
    }

    @Override
    public void newContent(String relativePath) {
        runHugoCommand(
            "Creating Hugo content...",
            List.of("hugo", "new", normalizeNewContentPath(relativePath)),
            "Hugo content created",
            true
        );
    }

    @Override
    public void build() {
        runHugoCommand(
            "Building Hugo site...",
            List.of("hugo", "--gc", "--minify"),
            "Hugo build completed",
            false
        );
    }

    @Override
    public void mod(String subCommand) {
        String normalizedSubCommand = (subCommand == null || subCommand.isBlank()) ? "tidy" : subCommand.trim();
        runHugoCommand(
            "Running hugo mod " + normalizedSubCommand + "...",
            List.of("hugo", "mod", normalizedSubCommand),
            "hugo mod " + normalizedSubCommand + " completed",
            false
        );
    }

    @Override
    public void env() {
        runHugoCommand(
            "Inspecting Hugo environment...",
            List.of("hugo", "env"),
            "Hugo environment captured",
            false
        );
    }

    @Override
    public void frontMatterTemplate() {
        publishStatusMessage("Front matter template tools are available in the Hugo editor context");
    }

    @Override
    public void insertShortcode(String shortcodeName) {
        String normalized = shortcodeName == null || shortcodeName.isBlank() ? "shortcode" : shortcodeName.trim();
        publishStatusMessage("Shortcode helper selected: " + normalized);
    }

    public void bindThemeProperty(ObjectProperty<Theme> externalTheme) {
        unbindThemeProperty();
        if (externalTheme == null) {
            return;
        }
        boundThemeProperty = externalTheme;
        themeChangeListener = (obs, oldTheme, newTheme) -> syncThemeBinding(newTheme);
        boundThemeProperty.addListener(themeChangeListener);
        syncThemeBinding(boundThemeProperty.get());
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

    @Override
    public void dispose() {
        disposed = true;
        unbindThemeProperty();
        if (locationChangeListener != null) {
            webEngine.locationProperty().removeListener(locationChangeListener);
        }
        if (loadStateListener != null) {
            webEngine.getLoadWorker().stateProperty().removeListener(loadStateListener);
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
        }
        processManager.close();
        lifecycleExecutor.shutdownNow();
        runOnFx(() -> {
            navigator.navigate("about:blank");
            setCenter(null);
        });
    }

    private void bindEngineSignals() {
        locationChangeListener = (obs, oldLocation, newLocation) -> {
            if (newLocation == null || newLocation.isBlank()) {
                return;
            }
            toolbar.setAddress(newLocation);
            URI target = UrlPolicy.safeCreate(newLocation);
            if (target != null && target.getRawPath() != null && !target.getRawPath().isBlank()) {
                lastRelativePath = normalizeRelativePath(target.getRawPath());
            }
            enforceExternalNavigationPolicy(oldLocation, newLocation, target);
        };
        webEngine.locationProperty().addListener(locationChangeListener);

        loadStateListener = (obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                statusBar.setMessage("Loaded");
            } else if (newState == Worker.State.FAILED) {
                statusBar.setMessage("Load failed");
                showPlaceholder("Load failed");
            }
        };
        webEngine.getLoadWorker().stateProperty().addListener(loadStateListener);
    }

    private void enforceExternalNavigationPolicy(String oldLocation, String newLocation, URI target) {
        if (config.allowExternalNavigation() || suppressExternalGuard || target == null) {
            return;
        }
        if ("about".equalsIgnoreCase(target.getScheme())) {
            return;
        }
        if (!processManager.isRunning()) {
            return;
        }
        URI endpoint;
        try {
            endpoint = processManager.endpoint();
        } catch (Exception ex) {
            return;
        }
        if (UrlPolicy.isSameOrigin(endpoint, target)) {
            return;
        }
        openInBrowser(newLocation);
        String rollback = (oldLocation == null || oldLocation.isBlank()) ? endpoint.toString() : oldLocation;
        suppressExternalGuard = true;
        runOnFx(() -> {
            try {
                navigator.navigate(rollback);
                statusBar.setMessage("Opened external link in browser");
            } finally {
                suppressExternalGuard = false;
            }
        });
    }

    private void openInBrowser(String location) {
        if (location == null || location.isBlank()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(location));
            }
        } catch (Exception ex) {
            statusBar.setMessage("Failed to open browser: " + ex.getMessage());
        }
    }

    private void applyPreviewChrome() {
        Theme activeTheme = themeProperty.get() == null ? Theme.dark() : themeProperty.get();
        Theme fallback = UiCommonThemeSupport.fallbackTheme(activeTheme);
        String background = UiStyleSupport.paintToCss(activeTheme.background(),
            UiStyleSupport.paintToCss(fallback.background(), "transparent"));
        setStyle("-fx-background-color: " + background + ";");
        contentStack.setStyle("-fx-background-color: " + background + ";");
        toolbar.applyTheme(activeTheme);
        statusBar.applyTheme(activeTheme);
        placeholderLabel.setFont(activeTheme.contentFont() == null ? fallback.contentFont() : activeTheme.contentFont());
        placeholderLabel.setTextFill(UiCommonThemeSupport.textPrimary(activeTheme));
        webEngine.setUserStyleSheetLocation(null);
    }

    private void setServerState(HugoServerProcessManager.State state, int port) {
        statusBar.setServerState(state, port);
        toolbar.setServerState(state);
    }

    private void syncThemeBinding(Theme theme) {
        themeProperty.set(theme == null ? Theme.dark() : theme);
        applyPreviewChrome();
    }

    private void hidePlaceholder() {
        placeholderLabel.setVisible(false);
    }

    private String normalizeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String normalizeNewContentPath(String relativePath) {
        String normalized = relativePath == null || relativePath.isBlank()
            ? "content/posts/ribbon-post.md"
            : relativePath.trim();
        normalized = normalized.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void runHugoCommand(
        String busyMessage,
        List<String> command,
        String successMessage,
        boolean reloadPreview
    ) {
        if (!canRunHugoCommands()) {
            publishStatusMessage("Hugo command unavailable: site root or CLI is not ready");
            return;
        }
        List<String> commandLine = List.copyOf(command);
        Path siteRoot = currentSiteRoot;
        publishStatusMessage(busyMessage);
        lifecycleExecutor.submit(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
                processBuilder.directory(siteRoot.toFile());
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                CommandOutputCapture outputCapture = CommandOutputCapture.start(process, 16);
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    outputCapture.await(Duration.ofSeconds(1));
                    publishStatusMessage("Hugo command timed out");
                    return;
                }
                String output = outputCapture.read(Duration.ofSeconds(1));
                if (process.exitValue() != 0) {
                    String failure = output.isBlank() ? "Exit code " + process.exitValue() : output;
                    publishStatusMessage("Hugo command failed: " + failure);
                    return;
                }
                if (reloadPreview && processManager.isRunning()) {
                    runOnFx(() -> navigateTo(lastRelativePath));
                }
                String resolvedMessage = output.isBlank() ? successMessage : successMessage + ": " + output;
                publishStatusMessage(resolvedMessage);
            } catch (Exception ex) {
                publishStatusMessage("Hugo command failed: " + ex.getMessage());
            }
        });
    }

    private record CommandOutputCapture(
        List<String> lines,
        AtomicReference<IOException> failure,
        Thread outputThread
    ) {
        static CommandOutputCapture start(Process process, int maxLines) {
            List<String> lines = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<IOException> failure = new AtomicReference<>();
            Thread outputThread = new Thread(() -> drain(process, maxLines, lines, failure), "papiflyfx-hugo-command-output");
            outputThread.setDaemon(true);
            outputThread.start();
            return new CommandOutputCapture(lines, failure, outputThread);
        }

        String read(Duration timeout) throws IOException {
            await(timeout);
            IOException exception = failure.get();
            if (exception != null) {
                throw exception;
            }
            return snapshot();
        }

        void await(Duration timeout) {
            try {
                outputThread.join(Math.max(1L, timeout.toMillis()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private String snapshot() {
            synchronized (lines) {
                return String.join(" | ", lines);
            }
        }

        private static void drain(
            Process process,
            int maxLines,
            List<String> lines,
            AtomicReference<IOException> failure
        ) {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && lines.size() < maxLines) {
                        lines.add(trimmed);
                    }
                }
            } catch (IOException ex) {
                failure.set(ex);
            }
        }
    }

    private void publishStatusMessage(String message) {
        runOnFx(() -> statusBar.setMessage(message));
    }

    private void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }
}
