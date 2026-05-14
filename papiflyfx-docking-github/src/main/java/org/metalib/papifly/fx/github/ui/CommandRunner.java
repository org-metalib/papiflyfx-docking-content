package org.metalib.papifly.fx.github.ui;

import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CommandRunner implements AutoCloseable {

    private final ExecutorService executorService;
    private final Consumer<Runnable> uiExecutor;
    private final boolean synchronous;
    private final AtomicBoolean closed;

    public CommandRunner() {
        this(Executors.newSingleThreadExecutor(new DaemonThreadFactory()), Platform::runLater, false);
    }

    public CommandRunner(boolean synchronous) {
        this(null, Runnable::run, synchronous);
    }

    public CommandRunner(ExecutorService executorService, Consumer<Runnable> uiExecutor, boolean synchronous) {
        this.executorService = executorService;
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.synchronous = synchronous;
        this.closed = new AtomicBoolean(false);
    }

    public <T> void run(Callable<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        if (closed.get()) {
            return;
        }
        if (synchronous) {
            executeInline(action, onSuccess, onError);
            return;
        }
        if (executorService == null) {
            executeInline(action, onSuccess, onError);
            return;
        }
        executorService.submit(() -> executeInline(action, onSuccess, onError));
    }

    private <T> void executeInline(Callable<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        try {
            T value = action.call();
            uiExecutor.accept(() -> onSuccess.accept(value));
        } catch (Throwable throwable) {
            uiExecutor.accept(() -> onError.accept(throwable));
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && executorService != null) {
            executorService.shutdownNow();
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "github-toolbar-command-runner");
            thread.setDaemon(true);
            return thread;
        }
    }
}
