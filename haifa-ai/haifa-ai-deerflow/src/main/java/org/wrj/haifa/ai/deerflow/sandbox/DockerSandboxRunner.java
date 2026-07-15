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
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class DockerSandboxRunner implements SandboxRunner {

    private final DeerFlowProperties properties;

    public DockerSandboxRunner(DeerFlowProperties properties) {
        this.properties = properties;
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
            List<String> command = dockerCommand(request, workdir);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().clear();
            processBuilder.environment().put("PATH", System.getenv().getOrDefault("PATH", ""));
            process = processBuilder.start();

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Thread stdoutThread = streamTo(process.getInputStream(), stdout);
            Thread stderrThread = streamTo(process.getErrorStream(), stderr);

            Duration timeout = request.timeout();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            boolean timedOut = !finished;
            if (timedOut) {
                process.destroyForcibly();
            }
            stdoutThread.join(1_000);
            stderrThread.join(1_000);

            int exitCode = timedOut ? -1 : process.exitValue();
            LocalRestrictedSandboxRunner.TruncatedOutput truncated = LocalRestrictedSandboxRunner.truncate(
                    stdout.toString(), stderr.toString(), properties.getSandbox().getMaxOutputChars());
            return new SandboxResult(sandboxId, exitCode, truncated.stdout(), truncated.stderr(),
                    System.currentTimeMillis() - start, timedOut, truncated.truncated(), Map.of(
                            "sandboxBackend", SandboxBackend.DOCKER.id(),
                            "isolationLevel", "docker",
                            "strongIsolation", true,
                            "sandboxWorkdir", workdir.toString(),
                            "workspaceMounted", workspaceMountAvailable(request),
                            "workspaceMountMode", workspaceMountAvailable(request) ? "readwrite" : "none",
                            "dockerImage", properties.getSandbox().getDockerImage(),
                            "networkEnabled", request.networkEnabled(),
                            "timeoutMs", timeout.toMillis()
                    ));
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            return new SandboxResult(sandboxId, -1, "", "Docker sandbox execution failed: " + ex.getMessage(),
                    System.currentTimeMillis() - start, false, false, Map.of(
                            "sandboxBackend", SandboxBackend.DOCKER.id(),
                            "isolationLevel", "docker",
                            "strongIsolation", true,
                            "sandboxWorkdir", workdir.toString(),
                            "workspaceMounted", workspaceMountAvailable(request),
                            "workspaceMountMode", workspaceMountAvailable(request) ? "readwrite" : "none",
                            "error", ex.getClass().getName()
                    ));
        }
    }

    private List<String> dockerCommand(SandboxRequest request, Path workdir) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add(request.networkEnabled() ? "--network=bridge" : "--network=none");
        command.add("--memory=512m");
        command.add("--cpus=1");
        command.add("--pids-limit=128");
        command.add("--read-only");
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=64m");
        if (workspaceMountAvailable(request)) {
            command.add("--mount");
            command.add("type=bind,source=" + DockerPathMapper.bindSource(request.workspaceRoot())
                    + ",target=" + org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver.VIRTUAL_WORKSPACE_ROOT);
        }
        addMount(command, Path.of(properties.getSkillsRoot()), properties.getSkillsContainerPath(), true);
        addMount(command, Path.of(properties.getUploadsRoot()),
                org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver.VIRTUAL_UPLOADS_ROOT, true);
        addMount(command, Path.of(properties.getOutputsRoot()),
                org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver.VIRTUAL_OUTPUTS_ROOT, false);
        command.add("--mount");
        command.add("type=bind,source=" + DockerPathMapper.bindSource(workdir) + ",target=/sandbox");
        command.add("-w");
        command.add(request.runWorkingDirectory() != null ? "/sandbox"
                : (workspaceMountAvailable(request)
                        ? org.wrj.haifa.ai.deerflow.tool.UserDataPathResolver.VIRTUAL_WORKSPACE_ROOT : "/sandbox"));
        command.add("-e");
        command.add("SANDBOX_WORKDIR=/sandbox");
        request.environment().forEach((key, value) -> {
            if (key != null && key.matches("[A-Za-z_][A-Za-z0-9_]*") && value != null) {
                command.add("-e");
                command.add(key + "=" + value);
            }
        });
        command.add(properties.getSandbox().getDockerImage());
        if (request.cmdArgs() != null && !request.cmdArgs().isEmpty()) {
            command.addAll(request.cmdArgs());
        } else {
            command.add("/bin/bash");
            command.add("-lc");
            command.add(request.command());
        }
        return command;
    }

    private static void addMount(List<String> command, Path source, String target, boolean readOnly) {
        Path normalized = source.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            return;
        }
        command.add("--mount");
        command.add("type=bind,source=" + DockerPathMapper.bindSource(normalized) + ",target=" + target
                + (readOnly ? ",readonly" : ""));
    }

    private Path prepareWorkdir(SandboxRequest request, String sandboxId) {
        String runId = request.runId() == null || request.runId().isBlank() ? "adhoc" : request.runId();
        Path base = Path.of(properties.getWorkspaceRoot(), properties.getSandbox().getWorkdirSubdir(), runId, sandboxId);
        Path normalized = base.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create docker sandbox workdir: " + normalized, ex);
        }
        return normalized;
    }

    private static boolean workspaceMountAvailable(SandboxRequest request) {
        return request.workspaceRoot() != null && Files.isDirectory(request.workspaceRoot());
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
}
