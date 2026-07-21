package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;

/** Run-scoped runtime references; never serialized into Graph state/checkpoints. */
public record RunExecutionContext(
        AgentRunConfig runConfig,
        AgentRequest agentRequest,
        ExecutionLimits limits,
        AgentExecutionHook hook,
        AtomicInteger eventSequence,
        ToolPolicyService toolPolicyService,
        List<Skill> activeSkills,
        List<String> uploadedFileIds,
        Set<String> allowedToolNames,
        String parentRunId,
        String parentToolCallId,
        int depth
) {
    public RunExecutionContext {
        hook = hook == null ? NoopExecutionHook.INSTANCE : hook;
        eventSequence = eventSequence == null ? new AtomicInteger() : eventSequence;
        activeSkills = activeSkills == null ? List.of() : List.copyOf(activeSkills);
        uploadedFileIds = uploadedFileIds == null ? List.of() : List.copyOf(uploadedFileIds);
        allowedToolNames = allowedToolNames == null ? Set.of() : Set.copyOf(allowedToolNames);
        parentRunId = parentRunId == null ? "" : parentRunId;
        parentToolCallId = parentToolCallId == null ? "" : parentToolCallId;
        depth = Math.max(0, depth);
    }

    public boolean isToolAllowed(String toolName) {
        return allowedToolNames.isEmpty() || allowedToolNames.contains(toolName);
    }
}
