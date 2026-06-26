package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

public record AgentRuntimeContext(
        AgentRunConfig config,
        AgentRequest request,
        List<ToolResult> toolResults,
        DeerFlowProperties properties
) {

    public AgentRuntimeContext withToolResults(List<ToolResult> toolResults) {
        return new AgentRuntimeContext(config, request, toolResults, properties);
    }
}
