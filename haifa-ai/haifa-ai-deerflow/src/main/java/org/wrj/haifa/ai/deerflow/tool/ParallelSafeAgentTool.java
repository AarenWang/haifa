package org.wrj.haifa.ai.deerflow.tool;

import org.wrj.haifa.ai.deerflow.tool.execution.ToolConcurrencyMode;

/** Marker restricted to trusted, side-effect-free local tool implementations. */
public interface ParallelSafeAgentTool extends AgentTool {
    @Override
    default ToolConcurrencyMode concurrencyMode() {
        return ToolConcurrencyMode.PARALLEL_SAFE;
    }
}
