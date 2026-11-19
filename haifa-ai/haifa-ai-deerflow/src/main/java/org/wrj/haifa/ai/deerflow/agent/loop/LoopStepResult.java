package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;

public record LoopStepResult(
        int stepIndex,
        ModelStep modelStep,
        List<ToolCallResult> toolResults,
        StopReason stopReason
) {

    public enum StopReason {
        MAX_STEPS_REACHED,
        MAX_TOOL_CALLS_REACHED,
        TIMEOUT,
        FINAL_ANSWER,
        ERROR,
        CANCELLED
    }
}
