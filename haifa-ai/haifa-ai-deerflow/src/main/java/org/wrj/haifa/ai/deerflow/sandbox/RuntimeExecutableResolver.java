package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class RuntimeExecutableResolver {

    private final DeerFlowProperties properties;
    private final SandboxEnvironmentBuilder environmentBuilder;
    private final HostShellResolver shellResolver;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public RuntimeExecutableResolver(DeerFlowProperties properties) {
        this(properties,
                new SandboxEnvironmentBuilder(properties,
                        new TrustedEnvironmentPolicy(properties),
                        new HostShellResolver(properties)),
                new HostShellResolver(properties));
    }

    @Autowired
    public RuntimeExecutableResolver(DeerFlowProperties properties, SandboxEnvironmentBuilder environmentBuilder,
            HostShellResolver shellResolver) {
        this.properties = properties;
        this.environmentBuilder = environmentBuilder;
        this.shellResolver = shellResolver;
    }

    public String resolve(String language, SandboxBackend backend) {
        String normalized = normalizeLanguage(language);
        if (backend == SandboxBackend.DOCKER) {
            return dockerCommand(normalized);
        }
        String cacheKey = backend.id() + ":" + normalized;
        return cache.computeIfAbsent(cacheKey, ignored -> resolveHost(normalized, backend));
    }

    private String resolveHost(String normalized, SandboxBackend backend) {
        Map<String, String> environment = backend == SandboxBackend.LOCAL_TRUSTED
                ? environmentBuilder.buildTrusted(Map.of(), List.of(), Path.of(properties.getWorkspaceRoot()))
                : environmentBuilder.buildRestricted(Map.of(), List.of(), Path.of(properties.getWorkspaceRoot()));
        if ("bash".equals(normalized)) {
            Path executable = shellResolver.resolveBash(environment).executable();
            probe(normalized, executable, environment);
            return executable.toString();
        }
        String configured = configured(normalized);
        if (!configured.isBlank()) {
            Path path = Path.of(configured);
            if (!path.isAbsolute()) {
                throw unavailable(normalized, "configured executable must be an absolute path");
            }
            Path executable = validate(normalized, path);
            probe(normalized, executable, environment);
            return executable.toString();
        }
        for (String candidate : candidates(normalized)) {
            Path resolved = findOnPath(candidate, environment);
            if (resolved != null) {
                Path executable = validate(normalized, resolved);
                probe(normalized, executable, environment);
                return executable.toString();
            }
        }
        throw unavailable(normalized, "executable was not found on the effective PATH");
    }

    private static void probe(String language, Path executable, Map<String, String> environment) {
        List<String> command = switch (language) {
            case "powershell" -> List.of(executable.toString(), "-NoProfile", "-Command", "$PSVersionTable.PSVersion.ToString()");
            default -> List.of(executable.toString(), "--version");
        };
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.environment().clear();
            builder.environment().putAll(environment);
            process = builder.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw unavailable(language, "version probe timed out for " + executable);
            }
            if (process.exitValue() != 0) {
                throw unavailable(language, "version probe exited with " + process.exitValue());
            }
        } catch (java.io.IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw unavailable(language, "version probe failed for " + executable + ": " + ex.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String configured(String language) {
        String value = properties.getSandbox().getExecutables().get(language);
        if ((value == null || value.isBlank()) && "python3".equals(language)) {
            value = properties.getSandbox().getExecutables().get("python");
        }
        return value == null ? "" : value.trim();
    }

    private static Path validate(String language, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw unavailable(language, "configured executable is not a regular file: " + normalized);
        }
        if (!HostShellResolver.isWindows() && !Files.isExecutable(normalized)) {
            throw unavailable(language, "configured executable is not executable: " + normalized);
        }
        return normalized;
    }

    private static Path findOnPath(String command, Map<String, String> environment) {
        String path = HostShellResolver.environmentValue(environment, "PATH",
                environment == null ? System.getenv("PATH") : "");
        if (path == null || path.isBlank()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        names.add(command);
        if (HostShellResolver.isWindows() && !command.contains(".")) {
            String pathExt = HostShellResolver.environmentValue(environment, "PATHEXT", ".EXE;.CMD;.BAT;.COM");
            for (String extension : pathExt.split(";")) {
                if (!extension.isBlank()) {
                    names.add(command + extension.toLowerCase(Locale.ROOT));
                    names.add(command + extension.toUpperCase(Locale.ROOT));
                }
            }
        }
        for (String directory : path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            if (directory == null || directory.isBlank()) {
                continue;
            }
            for (String name : names) {
                Path candidate = Path.of(directory, name).toAbsolutePath().normalize();
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static List<String> candidates(String language) {
        return switch (language) {
            case "node" -> List.of("node", "node.exe");
            case "python" -> HostShellResolver.isWindows() ? List.of("python", "python.exe") : List.of("python", "python3");
            case "python3" -> HostShellResolver.isWindows() ? List.of("python3", "python", "python.exe") : List.of("python3", "python");
            case "powershell" -> HostShellResolver.isWindows() ? List.of("pwsh", "powershell", "pwsh.exe", "powershell.exe") : List.of("pwsh", "powershell");
            default -> throw unavailable(language, "unsupported language");
        };
    }

    private static String dockerCommand(String language) {
        return switch (language) {
            case "python", "python3", "node", "bash" -> language;
            case "powershell" -> "pwsh";
            default -> throw unavailable(language, "unsupported language");
        };
    }

    private static String normalizeLanguage(String language) {
        return language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalStateException unavailable(String language, String reason) {
        String key = language == null || language.isBlank() ? "<language>" : language;
        return new IllegalStateException("Runtime capability '" + key + "' is unavailable: " + reason
                + ". Configure haifa.ai.deerflow.sandbox.executables." + key + " or use docker.");
    }
}
