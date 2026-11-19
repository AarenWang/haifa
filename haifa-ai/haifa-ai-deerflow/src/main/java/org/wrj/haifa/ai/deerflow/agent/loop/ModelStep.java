package org.wrj.haifa.ai.deerflow.agent.loop;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.cache.ModelCallPurpose;
import org.wrj.haifa.ai.deerflow.model.cache.ModelUsage;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheEligibility;
import org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint;

public record ModelStep(
        int stepIndex,
        String prompt,
        String response,
        List<ToolCallResult> toolCalls,
        long startedAt,
        long durationMs,
        ModelUsage usage,
        PromptFingerprint promptFingerprint,
        ModelCallPurpose purpose,
        PromptCacheEligibility eligibility
) {
    public ModelStep {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        usage = usage == null ? ModelUsage.empty() : usage;
        promptFingerprint = promptFingerprint == null ? PromptFingerprint.empty() : promptFingerprint;
        purpose = purpose == null ? ModelCallPurpose.AGENT_STEP : purpose;
        eligibility = eligibility == null ? PromptCacheEligibility.UNKNOWN : eligibility;
    }

    public ModelStep(int stepIndex, String prompt, String response, List<ToolCallResult> toolCalls, long startedAt, long durationMs) {
        this(stepIndex, prompt, response, toolCalls, startedAt, durationMs, ModelUsage.empty(), PromptFingerprint.empty(), ModelCallPurpose.AGENT_STEP, PromptCacheEligibility.UNKNOWN);
    }
}
