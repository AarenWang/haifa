package org.wrj.haifa.ai.deerflow.sandbox;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class SandboxEnvironmentBuilder {

    private static final List<String> WINDOWS_ESSENTIAL = List.of(
            "SystemRoot", "SystemDrive", "windir", "COMSPEC", "PATHEXT", "TEMP", "TMP",
            "APPDATA", "LOCALAPPDATA", "ProgramData", "CommonProgramFiles", "OS",
            "PROCESSOR_ARCHITECTURE", "NUMBER_OF_PROCESSORS");
    private static final List<String> UNIX_ESSENTIAL = List.of(
            "TERM", "SHELL", "USER", "LOGNAME", "TEMP", "TMP", "TMPDIR", "LANG", "LC_ALL", "LC_CTYPE");

    private final DeerFlowProperties properties;
    private final TrustedEnvironmentPolicy trustedPolicy;
    private final HostShellResolver shellResolver;
    private volatile Map<String, String> cachedProfileEnvironment;

    public SandboxEnvironmentBuilder(DeerFlowProperties properties, TrustedEnvironmentPolicy trustedPolicy,
            HostShellResolver shellResolver) {
        this.properties = properties;
        this.trustedPolicy = trustedPolicy;
        this.shellResolver = shellResolver;
    }

    public Map<String, String> buildRestricted(Map<String, String> explicitEnvironment, List<String> commandArgs, Path workdir) {
        Map<String, String> result = environmentMap();
        copyIfPresent(result, "PATH", System.getenv());
        result.put("PYTHONIOENCODING", "utf-8");
        result.put("PYTHONUTF8", "1");
        copyEssential(result, System.getenv());
        overlayExplicit(result, explicitEnvironment);
        copyIfPresent(result, "PATH", System.getenv());
        copyEssential(result, System.getenv());
        addExecutableParents(result, commandArgs);
        applyHome(result, workdir, true);
        addShellCompatibility(result);
        return Map.copyOf(result);
    }

    public Map<String, String> buildTrusted(Map<String, String> explicitEnvironment, List<String> commandArgs, Path workdir) {
        DeerFlowProperties.Sandbox.LocalTrusted config = properties.getSandbox().getLocalTrusted();
        Map<String, String> inherited = config.isLoadUserProfile() ? captureProfileEnvironment() : System.getenv();
        Map<String, String> result = environmentMap();
        if (config.isInheritEnvironment()) {
            inherited.forEach((key, value) -> {
                if (key != null && value != null && trustedPolicy.shouldInherit(key)) {
                    result.put(key, value);
                }
            });
        }
        if (config.isInheritPath()) {
            copyIfPresent(result, "PATH", inherited);
            copyIfPresent(result, "PATHEXT", inherited);
        }
        overlayExplicit(result, explicitEnvironment);
        addExecutableParents(result, commandArgs);
        result.put("PYTHONIOENCODING", "utf-8");
        result.put("PYTHONUTF8", "1");
        applyHome(result, workdir, config.isIsolateHome());
        addShellCompatibility(result);
        return Map.copyOf(result);
    }

    private Map<String, String> captureProfileEnvironment() {
        Map<String, String> cached = cachedProfileEnvironment;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedProfileEnvironment == null) {
                cachedProfileEnvironment = captureProfileEnvironmentOnce();
            }
            return cachedProfileEnvironment;
        }
    }

    private Map<String, String> captureProfileEnvironmentOnce() {
        Process process = null;
        try {
            HostShell shell = shellResolver.resolveBash(System.getenv());
            String command = profileCommand(properties.getSandbox().getLocalTrusted().getShellInitFiles());
            ProcessBuilder builder = new ProcessBuilder(shell.command(command, true));
            builder.environment().clear();
            builder.environment().putAll(System.getenv());
            addShellCompatibility(builder.environment());
            process = builder.start();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            process.getInputStream().transferTo(stdout);
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out while capturing trusted login-shell environment");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Trusted login-shell environment command exited with " + process.exitValue());
            }
            Map<String, String> result = environmentMap();
            String text = stdout.toString(StandardCharsets.UTF_8);
            for (String line : text.split("\\R")) {
                int separator = line.indexOf('=');
                if (separator > 0) {
                    result.put(line.substring(0, separator), line.substring(separator + 1));
                }
            }
            return Map.copyOf(result);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to capture Local Trusted profile environment: " + ex.getMessage(), ex);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String profileCommand(List<String> files) {
        StringBuilder command = new StringBuilder("set +e;");
        if (files != null) {
            for (String value : files) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                Path path = Path.of(value).toAbsolutePath().normalize();
                if (!Files.isRegularFile(path)) {
                    throw new IllegalArgumentException("Configured shell init file does not exist: " + value);
                }
                String quoted = path.toString().replace("'", "'\\''");
                command.append(" [ -r '").append(quoted).append("' ] && . '").append(quoted).append("' >/dev/null 2>&1 || true;");
            }
        }
        command.append(" env");
        return command.toString();
    }

    private static void overlayExplicit(Map<String, String> target, Map<String, String> source) {
        if (source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null && key.matches("[A-Za-z_][A-Za-z0-9_]*") && value != null && !value.isBlank()) {
                target.put(key, value);
            }
        });
    }

    private static void copyEssential(Map<String, String> target, Map<String, String> source) {
        List<String> names = HostShellResolver.isWindows() ? WINDOWS_ESSENTIAL : UNIX_ESSENTIAL;
        names.forEach(name -> copyIfPresent(target, name, source));
    }

    private static void copyIfPresent(Map<String, String> target, String name, Map<String, String> source) {
        if (source == null) {
            return;
        }
        source.forEach((key, value) -> {
            if (key.equalsIgnoreCase(name) && value != null && !value.isBlank()) {
                target.put(name, value);
            }
        });
    }

    private static void addExecutableParents(Map<String, String> environment, List<String> commandArgs) {
        if (commandArgs == null || commandArgs.isEmpty()) {
            return;
        }
        try {
            Path executable = Path.of(commandArgs.get(0));
            if (!executable.isAbsolute() || executable.getParent() == null) {
                return;
            }
            String existing = HostShellResolver.environmentValue(environment, "PATH", "");
            List<String> entries = new ArrayList<>();
            entries.add(executable.getParent().toString());
            if (!existing.isBlank()) {
                entries.add(existing);
            }
            environment.put("PATH", String.join(java.io.File.pathSeparator, entries));
        } catch (Exception ignored) {
        }
    }

    private static void applyHome(Map<String, String> environment, Path workdir, boolean isolate) {
        if (workdir == null) {
            return;
        }
        if (isolate || !environment.containsKey("HOME")) {
            environment.put("HOME", workdir.toString());
        }
        if (HostShellResolver.isWindows() && (isolate || !environment.containsKey("USERPROFILE"))) {
            environment.put("USERPROFILE", workdir.toString());
        }
    }

    private static void addShellCompatibility(Map<String, String> environment) {
        environment.putIfAbsent("LANG", "C.UTF-8");
        environment.putIfAbsent("LC_ALL", "C.UTF-8");
        if (HostShellResolver.isWindows()) {
            environment.put("MSYS_NO_PATHCONV", "1");
            environment.put("MSYS2_ARG_CONV_EXCL", "*");
        }
    }

    private static Map<String, String> environmentMap() {
        return HostShellResolver.isWindows() ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER) : new java.util.LinkedHashMap<>();
    }
}
