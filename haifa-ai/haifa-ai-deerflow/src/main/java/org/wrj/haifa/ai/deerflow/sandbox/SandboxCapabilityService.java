package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class SandboxCapabilityService {

    private final DeerFlowProperties properties;
    private final RuntimeExecutableResolver executableResolver;

    public SandboxCapabilityService(DeerFlowProperties properties, RuntimeExecutableResolver executableResolver) {
        this.properties = properties;
        this.executableResolver = executableResolver;
    }

    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!properties.getSandbox().isEnabled()) {
            result.put("status", "DISABLED");
            return Map.copyOf(result);
        }
        SandboxExecutionPolicy.Decision execution = SandboxExecutionPolicy.evaluate(properties, true);
        if (!execution.allowed()) {
            result.put("status", "DENIED");
            result.put("reason", execution.reason());
            return Map.copyOf(result);
        }
        SandboxBackend backend = execution.backend();
        result.put("backend", backend.id());
        Map<String, Object> runtimes = new LinkedHashMap<>();
        boolean degraded = false;
        for (String language : allowedLanguages()) {
            Map<String, Object> capability = new LinkedHashMap<>();
            try {
                String executable = executableResolver.resolve(language, backend);
                capability.put("status", "UP");
                capability.put("executable", backend == SandboxBackend.DOCKER
                        ? executable : Path.of(executable).getFileName().toString());
            } catch (RuntimeException ex) {
                degraded = true;
                capability.put("status", "DOWN");
                capability.put("reason", "Runtime capability is unavailable; configure sandbox.executables."
                        + language + " or use a compatible Docker image");
            }
            runtimes.put(language, Map.copyOf(capability));
        }
        result.put("status", degraded ? "DEGRADED" : "UP");
        result.put("runtimes", Map.copyOf(runtimes));
        return Map.copyOf(result);
    }

    private java.util.List<String> allowedLanguages() {
        String configured = properties.getSandbox().getAllowedScriptLanguages();
        if (configured == null || configured.isBlank()) {
            return java.util.List.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
