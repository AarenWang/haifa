package org.wrj.haifa.ai.deerflow.tool.execution;

import java.util.function.Supplier;

public record ToolExecutionTask<T>(
        String callId,
        ToolConcurrencyMode concurrencyMode,
        String resourceKey,
        Supplier<T> action
) {
    public ToolExecutionTask {
        concurrencyMode = concurrencyMode == null ? ToolConcurrencyMode.SERIAL_PER_RUN : concurrencyMode;
        resourceKey = resourceKey == null ? "" : resourceKey.trim();
        if (concurrencyMode == ToolConcurrencyMode.SERIAL_PER_RESOURCE && resourceKey.isBlank()) {
            concurrencyMode = ToolConcurrencyMode.SERIAL_PER_RUN;
        }
        if (action == null) {
            throw new IllegalArgumentException("Tool action is required");
        }
    }
}
