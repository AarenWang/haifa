package org.wrj.haifa.ai.deerflow.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record SandboxRequest(
        String command,
        List<String> cmdArgs,
        Path workspaceRoot,
        Path runWorkingDirectory,
        Duration timeout,
        Map<String, String> environment,
        boolean networkEnabled,
        String runId
) {

    public SandboxRequest(
            String command,
            Path workspaceRoot,
            Path runWorkingDirectory,
            Duration timeout,
            Map<String, String> environment,
            boolean networkEnabled,
            String runId
    ) {
        this(command, List.of(), workspaceRoot, runWorkingDirectory, timeout, environment, networkEnabled, runId);
    }

    public SandboxRequest {
        cmdArgs = cmdArgs == null ? List.of() : List.copyOf(cmdArgs);
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }
}
