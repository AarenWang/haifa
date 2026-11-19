package org.wrj.haifa.ai.deerflow.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.skill.Skill;

public class ToolEnvironmentBuilder {

    public static final String STABLE_TOOL_INSTRUCTION = """
            Use available structured tools when external information or actions are required.
            Tool execution is managed by DeerFlow runtime.
            Do not invent tool results or write tool calls manually in XML, JSON, markdown, or prose.
            When no further tool call is needed, respond with the final answer in normal assistant text.
            """;

    public record ToolEnvironment(String systemInstruction, List<ModelToolDefinition> toolDefinitions) {}

    public static ToolEnvironment build(ToolRegistry toolRegistry, ToolPolicyService toolPolicyService, List<Skill> activeSkills, RunMode mode) {
        return build(toolRegistry, toolPolicyService, activeSkills, mode, null);
    }

    public static ToolEnvironment build(ToolRegistry toolRegistry, ToolPolicyService toolPolicyService,
            List<Skill> activeSkills, RunMode mode, String runId) {
        List<ModelToolDefinition> toolDefinitions = new ArrayList<>();
        if (toolRegistry != null && toolRegistry.modelVisibleTools(runId) != null) {
            for (AgentTool tool : toolRegistry.modelVisibleTools(runId)) {
                if (tool == null || !StringUtils.hasText(tool.name())) {
                    continue;
                }
                if (toolPolicyService != null && !toolPolicyService.evaluateTool(tool.name(), activeSkills, mode).allowed()) {
                    continue;
                }
                toolDefinitions.add(new ModelToolDefinition(tool.name(), tool.description(), tool.inputSchema()));
            }
        }
        toolDefinitions.sort(Comparator.comparing(ModelToolDefinition::name));

        return new ToolEnvironment(STABLE_TOOL_INSTRUCTION.trim(), toolDefinitions);
    }
}
