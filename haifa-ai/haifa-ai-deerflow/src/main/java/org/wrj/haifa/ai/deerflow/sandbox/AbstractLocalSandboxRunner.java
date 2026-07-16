package org.wrj.haifa.ai.deerflow.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;

abstract class AbstractLocalSandboxRunner implements SandboxRunner {

    protected final DeerFlowProperties properties;
    protected final UserDataPathResolver pathResolver;
    private final SandboxEnvironmentBuilder environmentBuilder;
    private final HostShellResolver shellResolver;
    private final ProcessOutputDecoder outputDecoder;
    private final SandboxSecretRedactor secretRedactor;

    AbstractLocalSandboxRunner(DeerFlowProperties properties, UserDataPathResolver pathResolver,
            SandboxEnvironmentBuilder environmentBuilder, HostShellResolver shellResolver,
            ProcessOutputDecoder outputDecoder, SandboxSecretRedactor secretRedactor) {
        this.properties = properties;
        this.pathResolver = pathResolver != null ? pathResolver : new UserDataPathResolver(properties);
        this.environmentBuilder = environmentBuilder;
        this.shellResolver = shellResolver;
        this.outputDecoder = outputDecoder;
        this.secretRedactor = secretRedactor;
    }

    @Override
    public SandboxResult run(SandboxRequest request) {
        String sandboxId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        Path workdir = request.runWorkingDirectory() == null
                ? prepareWorkdir(request, sandboxId)
                : validateWorkdir(request.runWorkingDirectory());
        Process process = null;
        Map<String, String> processEnvironment = Map.of();
        try {
            String resolvedCommand = pathResolver.rewriteContainerPathsToLocal(request.command());
            if (HostShellResolver.isWindows()) {
                resolvedCommand = resolvedCommand.replace('\\', '/');
            }
            List<String> processCommand = new ArrayList<>();
            String shellKind = "direct";
            if (request.cmdArgs() != null && !request.cmdArgs().isEmpty()) {
                request.cmdArgs().forEach(arg -> processCommand.add(pathResolver.rewriteContainerPathsToLocal(arg)));
            } else {
                Map<String, String> shellEnvironment = buildEnvironment(request.environment(), List.of(), workdir);
                HostShell shell = shellResolver.resolveBash(shellEnvironment);
                processCommand.addAll(shell.command(resolvedCommand, false));
                shellKind = shell.kind() == HostShell.Kind.GIT_BASH ? "git-bash" : "bash";
            }

            processEnvironment = buildEnvironment(request.environment(), processCommand, workdir);
            ProcessBuilder processBuilder = new ProcessBuilder(processCommand);
            processBuilder.directory(workdir.toFile());
            processBuilder.environment().clear();
            processBuilder.environment().putAll(processEnvironment);

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
                closeProcessStreams(process);
            }
            stdoutThread.join(1_000);
            stderrThread.join(1_000);

            int exitCode = timedOut ? -1 : process.exitValue();
            String decodedStdout = secretRedactor.redact(outputDecoder.decode(stdout.toByteArray()), processEnvironment);
            String decodedStderr = secretRedactor.redact(outputDecoder.decode(stderr.toByteArray()), processEnvironment);
            TruncatedOutput truncated = truncate(decodedStdout, decodedStderr,
                    properties.getSandbox().getMaxOutputChars());
            String maskedStdout = pathResolver.maskLocalPathsToContainer(truncated.stdout());
            String maskedStderr = pathResolver.maskLocalPathsToContainer(truncated.stderr());

            long durationMs = System.currentTimeMillis() - start;
            return new SandboxResult(sandboxId, exitCode, maskedStdout, maskedStderr, durationMs, timedOut,
                    truncated.truncated(), metadata(workdir, request, timeout, shellKind, false, null));
        } catch (Exception ex) {
            if (process != null) {
                terminateProcessTree(process);
            }
            long durationMs = System.currentTimeMillis() - start;
            String message = secretRedactor.redact("Sandbox execution failed: " + ex.getMessage(), processEnvironment);
            return new SandboxResult(sandboxId, -1, "", pathResolver.maskLocalPathsToContainer(message), durationMs,
                    false, false, metadata(workdir, request, request.timeout(), "unavailable", true,
                            ex.getClass().getName()));
        }
    }

    protected abstract Map<String, String> buildEnvironment(Map<String, String> explicitEnvironment,
            List<String> commandArgs, Path workdir);

    protected abstract String backendId();

    protected abstract String isolationLevel();

    protected abstract String environmentPolicy();

    protected abstract String isolationWarning();

    protected boolean exposePhysicalWorkdir() {
        return false;
    }

    protected final SandboxEnvironmentBuilder environmentBuilder() {
        return environmentBuilder;
    }

    private Map<String, Object> metadata(Path workdir, SandboxRequest request, Duration timeout, String shellKind,
            boolean failed, String errorType) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sandboxBackend", backendId());
        metadata.put("isolationLevel", isolationLevel());
        metadata.put("strongIsolation", false);
        metadata.put("hostExecution", true);
        metadata.put("environmentPolicy", environmentPolicy());
        metadata.put("shellKind", shellKind);
        metadata.put("isolationWarning", isolationWarning());
        metadata.put("sandboxWorkdir", exposePhysicalWorkdir()
                ? workdir.toString()
                : pathResolver.maskLocalPathsToContainer(workdir.toString()));
        metadata.put("workspaceMounted", false);
        metadata.put("networkEnabled", request.networkEnabled());
        metadata.put("timeoutMs", timeout.toMillis());
        if (failed && errorType != null) {
            metadata.put("error", errorType);
        }
        return Map.copyOf(metadata);
    }

    private Path validateWorkdir(Path requested) {
        Path workdir = requested.toAbsolutePath().normalize();
        Path allowedRoot = Path.of(properties.getWorkspaceRoot(), properties.getSandbox().getWorkdirSubdir())
                .toAbsolutePath().normalize();
        if (!workdir.startsWith(allowedRoot)) {
            throw new SecurityException("Access denied: runWorkingDirectory '" + workdir
                    + "' is outside of allowed sandbox root '" + allowedRoot + "'");
        }
        try {
            Files.createDirectories(workdir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create runWorkingDirectory: " + workdir, ex);
        }
        return workdir;
    }

    private Path prepareWorkdir(SandboxRequest request, String sandboxId) {
        String runId = request.runId() == null || request.runId().isBlank() ? "adhoc" : request.runId();
        Path normalized = Path.of(properties.getWorkspaceRoot(), properties.getSandbox().getWorkdirSubdir(), runId,
                sandboxId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create sandbox workdir: " + normalized, ex);
        }
        return normalized;
    }

    private static void terminateProcessTree(Process process) {
        List<ProcessHandle> descendants = process.descendants().toList().reversed();
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        descendants.forEach(handle -> waitForExit(handle.onExit(), 5_000));
        waitForExit(process.toHandle().onExit(), 5_000);
        process.descendants().toList().reversed().forEach(handle -> {
            handle.destroyForcibly();
            waitForExit(handle.onExit(), 2_000);
        });
    }

    private static void closeProcessStreams(Process process) {
        try { process.getInputStream().close(); } catch (IOException ignored) { }
        try { process.getErrorStream().close(); } catch (IOException ignored) { }
        try { process.getOutputStream().close(); } catch (IOException ignored) { }
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
