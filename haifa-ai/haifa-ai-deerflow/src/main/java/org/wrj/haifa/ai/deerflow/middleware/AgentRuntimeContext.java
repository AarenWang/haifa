package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

public record AgentRuntimeContext(
        AgentRunConfig config,
        AgentRequest request,
        List<ToolResult> toolResults,
        DeerFlowProperties properties,
        List<Skill> activeSkills
) {

    public AgentRuntimeContext {
        activeSkills = activeSkills == null ? List.of() : List.copyOf(activeSkills);
    }

    public static AgentRuntimeContext of(AgentRunConfig config, AgentRequest request,
            List<ToolResult> toolResults, DeerFlowProperties properties) {
        return new AgentRuntimeContext(config, request, toolResults, properties, List.of());
    }

    public static AgentRuntimeContext of(AgentRunConfig config, AgentRequest request,
            List<ToolResult> toolResults, DeerFlowProperties properties, List<Skill> activeSkills) {
        return new AgentRuntimeContext(config, request, toolResults, properties, activeSkills);
    }

    public AgentRuntimeContext withToolResults(List<ToolResult> toolResults) {
        return new AgentRuntimeContext(config, request, toolResults, properties, activeSkills);
    }

    public AgentRuntimeContext withActiveSkills(List<Skill> activeSkills) {
        return new AgentRuntimeContext(config, request, toolResults, properties, activeSkills);
    }

    public AgentRuntimeContext withRequest(AgentRequest request) {
        return new AgentRuntimeContext(config, request, toolResults, properties, activeSkills);
    }
}
