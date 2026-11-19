package org.wrj.haifa.ai.deerflow.graph;

import org.wrj.haifa.ai.deerflow.agent.lifecycle.RunExecutionContext;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionLimits;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.NoopExecutionHook;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public record GraphChatRuntimeRequest(
        RunExecutionContext executionContext,
        List<MessageRecord> threadHistory
) {
    /** Temporary source-compatible bridge for tests and extensions compiled against phase 61. */
    @Deprecated(forRemoval = true)
    public GraphChatRuntimeRequest(Object ignoredLegacyLoop, Object legacyLimits,
                                   AgentRunConfig runConfig, AgentRequest agentRequest,
                                   AtomicInteger eventSequence, ToolPolicyService toolPolicyService,
                                   List<Skill> activeSkills, List<String> uploadedFileIds,
                                   List<MessageRecord> threadHistory) {
        this(new RunExecutionContext(runConfig, agentRequest, limitsOf(legacyLimits), NoopExecutionHook.INSTANCE,
                eventSequence, toolPolicyService, activeSkills, uploadedFileIds, Set.of(), "", "", 0),
                threadHistory);
    }

    public AgentRunConfig runConfig() { return executionContext.runConfig(); }
    public AgentRequest agentRequest() { return executionContext.agentRequest(); }
    public AtomicInteger eventSequence() { return executionContext.eventSequence(); }
    public ToolPolicyService toolPolicyService() { return executionContext.toolPolicyService(); }
    public List<Skill> activeSkills() { return executionContext.activeSkills(); }
    public List<String> uploadedFileIds() { return executionContext.uploadedFileIds(); }

    private static ExecutionLimits limitsOf(Object legacyLimits) {
        if (legacyLimits == null) {
            return new ExecutionLimits(12, 20, 300_000, ResearchOptions.defaults());
        }
        try {
            int steps = ((Number) legacyLimits.getClass().getMethod("maxSteps").invoke(legacyLimits)).intValue();
            int calls = ((Number) legacyLimits.getClass().getMethod("maxToolCalls").invoke(legacyLimits)).intValue();
            long timeout = ((Number) legacyLimits.getClass().getMethod("timeoutMs").invoke(legacyLimits)).longValue();
            Object research = legacyLimits.getClass().getMethod("researchOptions").invoke(legacyLimits);
            return new ExecutionLimits(steps, calls, timeout,
                    research instanceof ResearchOptions options ? options : ResearchOptions.defaults());
        } catch (ReflectiveOperationException ex) {
            return new ExecutionLimits(12, 20, 300_000, ResearchOptions.defaults());
        }
    }
}
