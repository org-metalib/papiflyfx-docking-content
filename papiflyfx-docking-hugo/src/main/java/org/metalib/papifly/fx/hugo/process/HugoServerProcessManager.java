package org.metalib.papifly.fx.hugo.process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class HugoServerProcessManager implements AutoCloseable {

    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        ERROR
    }

    @FunctionalInterface
    public interface CommandFactory {
        List<String> build(Path siteRoot, int port, HugoServerOptions options);
    }

    private static final Duration DEFAULT_READINESS_TIMEOUT = Duration.ofSeconds(12);
    private static final Pattern READINESS_PATTERN =
        Pattern.compile("Web Server is available at (http://[^\\s]+)");
    private static final int PORT_PROBE_ATTEMPTS = 30;
    private static final System.Logger LOG = System.getLogger(HugoServerProcessManager.class.getName());

    private final CommandFactory commandFactory;
    private final Duration readinessTimeout;
    private final ExecutorService ioExecutor;
    private final AtomicReference<Process> processRef = new AtomicReference<>();

    private volatile int boundPort = -1;
    private volatile URI endpoint;
    private volatile State state = State.STOPPED;
    private volatile Consumer<String> logListener;
    private volatile CompletableFuture<Void> readinessByLog = new CompletableFuture<>();
    private volatile boolean portProbeUnavailable;

    public HugoServerProcessManager() {
        this(HugoServerProcessManager::defaultCommand, DEFAULT_READINESS_TIMEOUT);
    }

    public HugoServerProcessManager(CommandFactory commandFactory, Duration readinessTimeout) {
        this.commandFactory = commandFactory;
        this.readinessTimeout = readinessTimeout;
        AtomicInteger threadId = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "hugo-process-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.ioExecutor = Executors.newCachedThreadPool(threadFactory);
    }

    public synchronized URI start(Path siteRoot, int preferredPort) throws Exception {
        return start(siteRoot, HugoServerOptions.defaults(preferredPort));
    }

    public synchronized URI start(Path siteRoot, HugoServerOptions options) throws Exception {
        if (isRunning()) {
            return endpoint();
        }
        if (siteRoot == null || !Files.isDirectory(siteRoot)) {
            state = State.ERROR;
            throw new IllegalArgumentException("Invalid Hugo site root: " + siteRoot);
        }

        int port = selectPort(options.preferredPort(), options.bindAddress());
        ProcessBuilder processBuilder = new ProcessBuilder(commandFactory.build(siteRoot, port, options));
        processBuilder.directory(siteRoot.toFile());
        processBuilder.redirectErrorStream(false);

        state = State.STARTING;
        readinessByLog = new CompletableFuture<>();

        Process process = processBuilder.start();
        processRef.set(process);
        boundPort = port;
        endpoint = URI.create("http://" + options.bindAddress() + ":" + port + "/");

        ioExecutor.submit(new ProcessLogPump(process.getInputStream(), this::onLogLine));
        ioExecutor.submit(new ProcessLogPump(process.getErrorStream(), this::onLogLine));
        ioExecutor.submit(() -> watchProcessExit(process));

        try {
            awaitReadiness(options.bindAddress(), port, readinessTimeout);
            state = State.RUNNING;
            return endpoint;
        } catch (Exception ex) {
            state = State.ERROR;
            stop();
            throw ex;
        }
    }

    public synchronized void stop() {
        Process process = processRef.getAndSet(null);
        if (process == null) {
            boundPort = -1;
            endpoint = null;
            if (state != State.ERROR) {
                state = State.STOPPED;
            }
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            boundPort = -1;
            endpoint = null;
            readinessByLog.completeExceptionally(new IllegalStateException("stopped"));
            state = State.STOPPED;
        }
    }

    public synchronized boolean isRunning() {
        Process process = processRef.get();
        return process != null && process.isAlive();
    }

    public synchronized URI endpoint() {
        if (endpoint == null) {
            throw new IllegalStateException("server not started");
        }
        return endpoint;
    }

    public synchronized int getBoundPort() {
        return boundPort;
    }

    public synchronized State getState() {
        return state;
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
    }

    @Override
    public void close() {
        stop();
        ioExecutor.shutdownNow();
    }

    private void watchProcessExit(Process process) {
        try {
            process.waitFor();
            synchronized (this) {
                if (processRef.get() == process) {
                    processRef.set(null);
                    boundPort = -1;
                    endpoint = null;
                    if (state == State.RUNNING || state == State.STARTING) {
                        state = State.ERROR;
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void onLogLine(String line) {
        if (line == null) {
            return;
        }
        if (READINESS_PATTERN.matcher(line).find()) {
            readinessByLog.complete(null);
        }
        Consumer<String> listener = logListener;
        if (listener != null) {
            listener.accept(line);
        }
    }

    private void awaitReadiness(String bindAddress, int port, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!isRunning()) {
                throw new IllegalStateException("Hugo server exited during startup");
            }
            if (readinessByLog.isDone()) {
                return;
            }
            if (isSocketReachable(bindAddress, port, 200)) {
                return;
            }
            Thread.sleep(80L);
        }
        throw new TimeoutException("Timed out waiting for Hugo server readiness");
    }

    private int selectPort(int preferredPort, String bindAddress) throws Exception {
        if (portProbeUnavailable) {
            return preferredPort;
        }
        for (int i = 0; i < PORT_PROBE_ATTEMPTS; i++) {
            int candidate = preferredPort + i;
            if (isPortFree(bindAddress, candidate)) {
                return candidate;
            }
            if (portProbeUnavailable) {
                return candidate;
            }
        }
        throw new IllegalStateException("No free port available for Hugo server");
    }

    private boolean isPortFree(String bindAddress, int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(bindAddress, port));
            return true;
        } catch (IOException ex) {
            if (isPermissionDenied(ex)) {
                if (!portProbeUnavailable) {
                    portProbeUnavailable = true;
                    LOG.log(
                        System.Logger.Level.WARNING,
                        "Port probing unavailable in this environment; using preferred Hugo port without pre-check",
                        ex
                    );
                }
                return true;
            }
            return false;
        }
    }

    private boolean isSocketReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static List<String> defaultCommand(Path siteRoot, int port, HugoServerOptions options) {
        List<String> command = new ArrayList<>();
        command.add(options.hugoBinary());
        command.add("server");
        command.add("--bind");
        command.add(options.bindAddress());
        command.add("--port");
        command.add(Integer.toString(port));
        command.add("--baseURL");
        command.add("http://" + options.bindAddress() + ":" + port + "/");
        if (options.disableFastRender()) {
            command.add("--disableFastRender");
        }
        if (options.renderToMemory()) {
            command.add("--renderToMemory");
        }
        if (options.buildDrafts()) {
            command.add("--buildDrafts");
        }
        return command;
    }

    private static boolean isPermissionDenied(IOException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return lowerMessage.contains("operation not permitted")
            || lowerMessage.contains("permission denied");
    }
}
