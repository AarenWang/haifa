package org.wrj.haifa.ai.deerflow.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class LocalRestrictedSandboxRunner implements SandboxRunner {

    private final DeerFlowProperties properties;

    public LocalRestrictedSandboxRunner(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public SandboxResult run(SandboxRequest request) {
        String sandboxId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        Path workdir = prepareWorkdir(request, sandboxId);
        Process process = null;
        try {
            List<String> shellCommand = shellCommand(request.command());
            ProcessBuilder processBuilder = new ProcessBuilder(shellCommand);
            processBuilder.directory(workdir.toFile());
            processBuilder.environment().clear();
            processBuilder.environment().put("PATH", System.getenv().getOrDefault("PATH", ""));
            processBuilder.environment().put("HOME", workdir.toString());
            processBuilder.environment().put("USERPROFILE", workdir.toString());
            processBuilder.environment().putAll(request.environment());

            process = processBuilder.start();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread stdoutThread = streamTo(process.getInputStream(), stdout);
            Thread stderrThread = streamTo(process.getErrorStream(), stderr);

            Duration timeout = request.timeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            boolean timedOut = !finished;
            if (timedOut) {
                terminateProcessTree(process);
            }
            stdoutThread.join(1_000);
            stderrThread.join(1_000);

            int exitCode = timedOut ? -1 : process.exitValue();
            TruncatedOutput truncated = truncate(stdout.toString(), stderr.toString(), properties.getSandbox().getMaxOutputChars());
            long durationMs = System.currentTimeMillis() - start;
            return new SandboxResult(sandboxId, exitCode, truncated.stdout(), truncated.stderr(), durationMs, timedOut,
                    truncated.truncated(), Map.of(
                            "sandboxBackend", SandboxBackend.LOCAL.id(),
                            "isolationLevel", "local-restricted",
                            "strongIsolation", false,
                            "isolationWarning", "local backend runs a host process with a restricted cwd/env; use docker for filesystem isolation",
                            "sandboxWorkdir", workdir.toString(),
                            "workspaceMounted", false,
                            "networkEnabled", request.networkEnabled(),
                            "timeoutMs", timeout.toMillis()
                    ));
        } catch (Exception ex) {
            if (process != null) {
                terminateProcessTree(process);
            }
            long durationMs = System.currentTimeMillis() - start;
            return new SandboxResult(sandboxId, -1, "", "Sandbox execution failed: " + ex.getMessage(), durationMs,
                    false, false, Map.of(
                            "sandboxBackend", SandboxBackend.LOCAL.id(),
                            "isolationLevel", "local-restricted",
                            "strongIsolation", false,
                            "isolationWarning", "local backend runs a host process with a restricted cwd/env; use docker for filesystem isolation",
                            "sandboxWorkdir", workdir.toString(),
                            "workspaceMounted", false,
                            "error", ex.getClass().getName()
                    ));
        }
    }

    private Path prepareWorkdir(SandboxRequest request, String sandboxId) {
        String runId = request.runId() == null || request.runId().isBlank() ? "adhoc" : request.runId();
        Path base = Path.of(properties.getOutputsRoot(), properties.getSandbox().getWorkdirSubdir(), runId, sandboxId);
        Path normalized = base.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create sandbox workdir: " + normalized, ex);
        }
        return normalized;
    }

    private static void terminateProcessTree(Process process) {
        List<ProcessHandle> descendants = process.descendants().toList();
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        waitForExit(process.toHandle().onExit(), 2_000);
        descendants.forEach(handle -> waitForExit(handle.onExit(), 2_000));
    }

    private static void waitForExit(CompletableFuture<ProcessHandle> exit, long timeoutMs) {
        try {
            exit.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    private static Thread streamTo(java.io.InputStream inputStream, ByteArrayOutputStream target) {
        Thread thread = new Thread(() -> {
            try (inputStream; target) {
                inputStream.transferTo(target);
            } catch (IOException ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static List<String> shellCommand(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> cmd = new ArrayList<>();
        if (os.contains("win")) {
            cmd.add("cmd.exe");
            cmd.add("/c");
        } else {
            cmd.add("/bin/bash");
            cmd.add("-lc");
        }
        cmd.add(command);
        return cmd;
    }

    static TruncatedOutput truncate(String stdout, String stderr, int maxOutputChars) {
        String out = stdout == null ? "" : stdout;
        String err = stderr == null ? "" : stderr;
        int limit = Math.max(1_000, maxOutputChars);
        if (out.length() + err.length() <= limit) {
            return new TruncatedOutput(out, err, false);
        }
        int stdoutLimit = Math.max(0, limit / 2);
        int stderrLimit = Math.max(0, limit - stdoutLimit);
        return new TruncatedOutput(truncateSingle(out, stdoutLimit), truncateSingle(err, stderrLimit), true);
    }

    private static String truncateSingle(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 64) {
            return value.substring(0, maxChars);
        }
        int head = maxChars / 2;
        int tail = maxChars - head - 32;
        return value.substring(0, head) + "\n[... sandbox output truncated ...]\n"
                + value.substring(value.length() - Math.max(0, tail));
    }

    record TruncatedOutput(String stdout, String stderr, boolean truncated) {
    }
}
