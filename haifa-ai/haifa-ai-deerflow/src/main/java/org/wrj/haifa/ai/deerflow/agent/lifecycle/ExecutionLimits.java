package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;

/** Serializable limits used by Graph without depending on the legacy loop package. */
public record ExecutionLimits(int maxSteps, int maxToolCalls, long timeoutMs, ResearchOptions researchOptions) {
    public ExecutionLimits {
        maxSteps = Math.max(1, Math.min(100, maxSteps));
        maxToolCalls = Math.max(1, Math.min(100, maxToolCalls));
        timeoutMs = Math.max(30_000, timeoutMs);
        researchOptions = researchOptions == null ? ResearchOptions.defaults() : researchOptions;
    }
}
