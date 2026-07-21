package org.wrj.haifa.ai.deerflow.graph;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionLimits;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.NoopExecutionHook;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;

/** Deprecated request wrapper retained only to audit the unreachable research graph. */
@Deprecated(forRemoval = true)
public record GraphResearchRuntimeRequest(RunExecutionContext executionContext, String systemPrompt,
        String userPrompt, List<MessageRecord> threadHistory) {

    @Deprecated(forRemoval = true)
    public GraphResearchRuntimeRequest(Object ignoredLegacyLoop, Object legacyLimits, AgentRunConfig runConfig,
            AgentRequest agentRequest, String systemPrompt, String userPrompt, AtomicInteger eventSequence,
            ToolPolicyService policy, List<Skill> skills, List<String> uploads, List<MessageRecord> history) {
        this(new RunExecutionContext(runConfig, agentRequest, limitsOf(legacyLimits), NoopExecutionHook.INSTANCE,
                eventSequence, policy, skills, uploads, Set.of(), "", "", 0), systemPrompt, userPrompt, history);
    }

    public AgentRunConfig runConfig() { return executionContext.runConfig(); }
    public AgentRequest agentRequest() { return executionContext.agentRequest(); }
    public AtomicInteger eventSequence() { return executionContext.eventSequence(); }
    public List<Skill> activeSkills() { return executionContext.activeSkills(); }
    public ExecutionLimits limits() { return executionContext.limits(); }

    private static ExecutionLimits limitsOf(Object legacyLimits) {
        try {
            int steps = ((Number) legacyLimits.getClass().getMethod("maxSteps").invoke(legacyLimits)).intValue();
            int calls = ((Number) legacyLimits.getClass().getMethod("maxToolCalls").invoke(legacyLimits)).intValue();
            long timeout = ((Number) legacyLimits.getClass().getMethod("timeoutMs").invoke(legacyLimits)).longValue();
            return new ExecutionLimits(steps, calls, timeout, ResearchOptions.defaults());
        } catch (Exception ex) {
            return new ExecutionLimits(12, 20, 300_000, ResearchOptions.defaults());
        }
    }
}
