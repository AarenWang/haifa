package org.wrj.haifa.ai.deerflow.agent.loop;

import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;

public record LoopConfig(
        int maxSteps,
        int maxToolCalls,
        long timeoutMs,
        ResearchOptions researchOptions
) {

    public LoopConfig {
        maxSteps = Math.max(1, Math.min(100, maxSteps));
        maxToolCalls = Math.max(1, Math.min(50, maxToolCalls));
        timeoutMs = Math.max(30_000, timeoutMs);
        researchOptions = researchOptions == null ? ResearchOptions.defaults() : researchOptions;
    }

    public static LoopConfig fromDefaults() {
        return new LoopConfig(20, 10, 300_000, ResearchOptions.defaults());
    }
}
