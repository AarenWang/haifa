package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class HostShellResolver {

    private final DeerFlowProperties properties;

    public HostShellResolver(DeerFlowProperties properties) {
        this.properties = properties;
    }

    public HostShell resolveBash(Map<String, String> environment) {
        List<Path> candidates = new ArrayList<>();
        addConfigured(candidates, properties.getSandbox().getLocalTrusted().getShellExecutable());
        addConfigured(candidates, properties.getSandbox().getExecutables().get("bash"));
        if (isWindows()) {
            String programFiles = environmentValue(environment, "ProgramFiles", "C:\\Program Files");
            String programFilesX86 = environmentValue(environment, "ProgramFiles(x86)", "C:\\Program Files (x86)");
            String localAppData = environmentValue(environment, "LOCALAPPDATA", "");
            candidates.add(Path.of(programFiles, "Git", "bin", "bash.exe"));
            candidates.add(Path.of(programFilesX86, "Git", "bin", "bash.exe"));
            if (!localAppData.isBlank()) {
                candidates.add(Path.of(localAppData, "Programs", "Git", "bin", "bash.exe"));
            }
            addFromPath(candidates, environment, List.of("bash.exe", "bash"));
        } else {
            addFromPath(candidates, environment, List.of("bash"));
            candidates.add(Path.of("/usr/bin/bash"));
            candidates.add(Path.of("/bin/bash"));
        }
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (isWindows() && isWindowsSubsystemLauncher(normalized)) {
                continue;
            }
            if (Files.isRegularFile(normalized) && (isWindows() || Files.isExecutable(normalized))) {
                HostShell.Kind kind = isWindows() ? HostShell.Kind.GIT_BASH : HostShell.Kind.BASH;
                return new HostShell(normalized, kind);
            }
        }
        throw new IllegalStateException("Bash executable is unavailable. Configure HAIFA_DEERFLOW_BASH_EXECUTABLE or HAIFA_DEERFLOW_TRUSTED_SHELL; Windows requires Git Bash.");
    }

    private static boolean isWindowsSubsystemLauncher(Path candidate) {
        String normalized = candidate.toString().replace('/', '\\').toLowerCase(Locale.ROOT);
        String systemRoot = System.getenv().getOrDefault("SystemRoot", "C:\\Windows")
                .replace('/', '\\').toLowerCase(Locale.ROOT);
        return normalized.equals(systemRoot + "\\system32\\bash.exe");
    }

    private static void addConfigured(List<Path> candidates, String configured) {
        if (configured == null || configured.isBlank()) {
            return;
        }
        Path path = Path.of(configured.trim());
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Configured shell executable must be an absolute path: " + configured);
        }
        candidates.add(path);
    }

    private static void addFromPath(List<Path> candidates, Map<String, String> environment, List<String> names) {
        String value = environmentValue(environment, "PATH", environment == null ? System.getenv("PATH") : "");
        if (value == null || value.isBlank()) {
            return;
        }
        for (String entry : value.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            for (String name : names) {
                candidates.add(Path.of(entry, name));
            }
        }
    }

    static String environmentValue(Map<String, String> environment, String key, String fallback) {
        if (environment != null) {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
