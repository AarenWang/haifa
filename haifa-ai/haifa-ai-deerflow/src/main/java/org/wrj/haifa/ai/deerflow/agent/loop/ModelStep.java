package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.loop.ToolCallResult;

public record ModelStep(
        int stepIndex,
        String prompt,
        String response,
        List<ToolCallResult> toolCalls,
        long startedAt,
        long durationMs
) {
}
