package org.wrj.haifa.ai.deerflow.sandbox;

import java.util.Map;

public record SandboxResult(
        String sandboxId,
        int exitCode,
        String stdout,
        String stderr,
        long durationMs,
        boolean timedOut,
        boolean outputTruncated,
        Map<String, Object> metadata
) {

    public SandboxResult {
        sandboxId = sandboxId == null ? "" : sandboxId;
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
