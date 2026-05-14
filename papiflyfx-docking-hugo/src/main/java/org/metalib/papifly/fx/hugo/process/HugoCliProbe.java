package org.metalib.papifly.fx.hugo.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class HugoCliProbe {

    private final List<String> command;

    public HugoCliProbe() {
        this(List.of("hugo", "version"));
    }

    public HugoCliProbe(String binary) {
        this(List.of(binary, "version"));
    }

    public HugoCliProbe(List<String> command) {
        this.command = List.copyOf(command);
    }

    public boolean isAvailable(Duration timeout) {
        return probeVersion(timeout).isPresent();
    }

    public Optional<String> probeVersion(Duration timeout) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    return Optional.of("hugo");
                }
                return Optional.of(line.strip());
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
