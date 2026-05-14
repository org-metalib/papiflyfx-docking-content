package org.metalib.papifly.fx.hugo.process;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class ProcessLogPump implements Runnable {

    private final InputStream inputStream;
    private final Consumer<String> lineConsumer;

    public ProcessLogPump(InputStream inputStream, Consumer<String> lineConsumer) {
        this.inputStream = inputStream;
        this.lineConsumer = lineConsumer;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        } catch (Exception ignored) {
        }
    }
}
