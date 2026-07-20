package org.wrj.haifa.ai.deerflow.tool.execution;

public enum ToolConcurrencyMode {
    PARALLEL_SAFE,
    SERIAL_PER_RUN,
    SERIAL_PER_RESOURCE,
    EXCLUSIVE
}
