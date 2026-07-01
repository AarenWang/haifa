package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public record SandboxRequest(
        String command,
        Path workspaceRoot,
        Path runWorkingDirectory,
        Duration timeout,
        Map<String, String> environment,
        boolean networkEnabled,
        String runId
) {

    public SandboxRequest {
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }
}
