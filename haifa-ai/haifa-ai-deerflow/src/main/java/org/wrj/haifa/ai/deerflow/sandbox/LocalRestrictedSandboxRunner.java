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
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver;

@Component
public class LocalRestrictedSandboxRunner implements SandboxRunner {

    private static final List<String> WINDOWS_ESSENTIAL_ENVS = List.of(
        "SystemRoot", "SystemDrive", "windir", "COMSPEC", "PATHEXT", "TEMP", "TMP",
        "APPDATA", "LOCALAPPDATA", "ProgramData", "CommonProgramFiles", "OS",
        "PROCESSOR_ARCHITECTURE", "NUMBER_OF_PROCESSORS"
    );

    private static final List<String> UNIX_ESSENTIAL_ENVS = List.of(
        "TERM", "SHELL", "USER", "LOGNAME", "TEMP", "TMP", "TMPDIR",
        "LANG", "LC_ALL", "LC_CTYPE"
    );

    private final DeerFlowProperties properties;
    private final UserDataPathResolver pathResolver;

    public LocalRestrictedSandboxRunner(DeerFlowProperties properties) {
        this(properties, null);
    }

    @Autowired
    public LocalRestrictedSandboxRunner(DeerFlowProperties properties, UserDataPathResolver pathResolver) {
        this.properties = properties;
        this.pathResolver = pathResolver != null ? pathResolver : new UserDataPathResolver(properties);
    }

    @Override
    public SandboxResult run(SandboxRequest request) {
        String sandboxId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        Path workdir;
        if (request.runWorkingDirectory() != null) {
            workdir = request.runWorkingDirectory().toAbsolutePath().normalize();
            Path allowedRoot = Path.of(properties.getWorkspaceRoot(), properties.getSandbox().getWorkdirSubdir()).toAbsolutePath().normalize();
            if (!workdir.startsWith(allowedRoot)) {
                throw new SecurityException("Access denied: runWorkingDirectory '" + workdir + "' is outside of allowed sandbox root '" + allowedRoot + "'");
            }
            try {
                Files.createDirectories(workdir);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to create runWorkingDirectory: " + workdir, ex);
            }
        } else {
            workdir = prepareWorkdir(request, sandboxId);
        }
        Process process = null;
        try {
            String resolvedCommand = pathResolver.rewriteContainerPathsToLocal(request.command());
            List<String> processCmd = new ArrayList<>();
            if (request.cmdArgs() != null && !request.cmdArgs().isEmpty()) {
                for (String arg : request.cmdArgs()) {
                    processCmd.add(pathResolver.rewriteContainerPathsToLocal(arg));
                }
            } else {
                processCmd = shellCommand(resolvedCommand);
            }
            ProcessBuilder processBuilder = new ProcessBuilder(processCmd);
            processBuilder.directory(workdir.toFile());
            Map<String, String> processEnv = processBuilder.environment();
            processEnv.clear();
            processEnv.put("PATH", System.getenv().getOrDefault("PATH", ""));
            processEnv.put("PYTHONIOENCODING", "utf-8");

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                processEnv.put("HOME", workdir.toString());
                processEnv.put("USERPROFILE", workdir.toString());
                for (String key : WINDOWS_ESSENTIAL_ENVS) {
                    String val = System.getenv(key);
                    if (val != null) {
                        processEnv.put(key, val);
                    }
                }
            } else {
                String home = System.getenv("HOME");
                if (home != null) {
                    processEnv.put("HOME", home);
                }
                for (String key : UNIX_ESSENTIAL_ENVS) {
                    String val = System.getenv(key);
                    if (val != null) {
                        processEnv.put(key, val);
                    }
                }
                processEnv.putIfAbsent("LANG", "en_US.UTF-8");
                processEnv.putIfAbsent("LC_ALL", "en_US.UTF-8");
            }
            processEnv.putAll(request.environment());

            process = processBuilder.start();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread stdoutThread = streamTo(process.getInputStream(), stdout);
            Thread ThreadStderr = streamTo(process.getErrorStream(), stderr);

            Duration timeout = request.timeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            boolean timedOut = !finished;
            if (timedOut) {
                terminateProcessTree(process);
            }
            stdoutThread.join(1_000);
            ThreadStderr.join(1_000);

            int exitCode = timedOut ? -1 : process.exitValue();
            TruncatedOutput truncated = truncate(stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8), properties.getSandbox().getMaxOutputChars());
            
            String maskedStdout = pathResolver.maskLocalPathsToContainer(truncated.stdout());
            String maskedStderr = pathResolver.maskLocalPathsToContainer(truncated.stderr());

            long durationMs = System.currentTimeMillis() - start;
            return new SandboxResult(sandboxId, exitCode, maskedStdout, maskedStderr, durationMs, timedOut,
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
        Path base = Path.of(properties.getWorkspaceRoot(), properties.getSandbox().getWorkdirSubdir(), runId, sandboxId);
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
