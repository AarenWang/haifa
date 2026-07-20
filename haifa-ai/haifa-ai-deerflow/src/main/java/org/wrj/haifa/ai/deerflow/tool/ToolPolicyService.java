package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxBackend;
import org.wrj.haifa.ai.deerflow.sandbox.SandboxExecutionPolicy;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.mcp.McpConnectionManager;
import org.wrj.haifa.ai.deerflow.mcp.McpRiskClassification;

@Component
public class ToolPolicyService {

    private final Set<String> builtinToolNames;
    private final DeerFlowProperties properties;
    private final McpConnectionManager mcpConnectionManager;

    @Autowired
    public ToolPolicyService(List<AgentTool> builtinTools, DeerFlowProperties properties,
            McpConnectionManager mcpConnectionManager) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
        this.mcpConnectionManager = mcpConnectionManager;
        this.builtinToolNames = new java.util.HashSet<>();
        for (AgentTool tool : builtinTools) {
            String name = tool.name();
            String source = DeferredToolCatalog.getSource(name);
            if ("builtin".equals(source) || (!isStandardToolName(name) && !name.startsWith("mcp__"))) {
                this.builtinToolNames.add(name);
            }
        }
    }

    public ToolPolicyService(List<AgentTool> builtinTools) {
        this(builtinTools, new DeerFlowProperties(), null);
    }

    public ToolPolicyService(List<AgentTool> builtinTools, DeerFlowProperties properties) {
        this(builtinTools, properties, null);
    }

    private static boolean isStandardToolName(String name) {
        return switch (name) {
            case "web_search", "web_fetch", "image_search", "ls", "read_file", "glob", "grep",
                 "write_file", "str_replace", "bash", "list_workspace_files", "read_workspace_file",
                 "list_uploaded_files", "read_uploaded_file", "task", "run_script" -> true;
            default -> false;
        };
    }

    public boolean isToolAllowed(String toolName, List<Skill> activeSkills) {
        return evaluateTool(toolName, activeSkills, org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH).allowed();
    }

    public boolean isToolAllowed(String toolName, List<Skill> activeSkills, org.wrj.haifa.ai.deerflow.agent.RunMode mode) {
        return evaluateTool(toolName, activeSkills, mode).allowed();
    }

    public ToolPolicyDecision evaluateTool(String toolName, List<Skill> activeSkills) {
        return evaluateTool(toolName, activeSkills, org.wrj.haifa.ai.deerflow.agent.RunMode.RESEARCH);
    }

    public ToolPolicyDecision evaluateTool(String toolName, List<Skill> activeSkills, org.wrj.haifa.ai.deerflow.agent.RunMode mode) {
        if (toolName == null || toolName.isBlank()) {
            return ToolPolicyDecision.deny("tool name is required");
        }
        // Built-in tools are always allowed
        if (builtinToolNames.contains(toolName)) {
            return ToolPolicyDecision.allow();
        }

        ToolPolicyDecision configuredDecision = configuredToolDecision(toolName);
        if (configuredDecision != null) {
            return configuredDecision;
        }

        if (toolName.startsWith("mcp__")) {
            if (mcpConnectionManager == null) {
                return ToolPolicyDecision.deny("MCP connection manager is unavailable");
            }
            return mcpConnectionManager.findIdentity(toolName)
                    .map(identity -> identity.localRiskClassification() == McpRiskClassification.UNKNOWN
                            ? ToolPolicyDecision.deny("MCP tool has no trusted local risk classification")
                            : ToolPolicyDecision.allow())
                    .orElseGet(() -> ToolPolicyDecision.deny("MCP tool is not present in the active catalog snapshot"));
        }

        if (activeSkills != null) {
            for (Skill skill : activeSkills) {
                Set<String> allowed = skill.allowedTools();
                if (allowed != null && allowed.contains(toolName)) {
                    return ToolPolicyDecision.allow();
                }
            }
        }
        return ToolPolicyDecision.deny("tool is not configured and no active skill allows it");
    }

    private ToolPolicyDecision configuredToolDecision(String toolName) {
        return switch (toolName) {
            case "web_search", "web_fetch", "image_search", "ls", "read_file", "glob", "grep",
                 "list_workspace_files", "read_workspace_file", "list_uploaded_files", "read_uploaded_file",
                 "task" -> ToolPolicyDecision.allow();
            case "write_file" -> properties.isWriteFileEnabled()
                    ? ToolPolicyDecision.allow()
                    : ToolPolicyDecision.deny("write_file is disabled by haifa.ai.deerflow.write-file-enabled=false");
            case "str_replace" -> properties.isStrReplaceEnabled()
                    ? ToolPolicyDecision.allow()
                    : ToolPolicyDecision.deny("str_replace is disabled by haifa.ai.deerflow.str-replace-enabled=false");
            case "bash" -> bashDecision();
            case "run_script" -> runScriptDecision();
            default -> null;
        };
    }

    private ToolPolicyDecision bashDecision() {
        if (!properties.isBashEnabled()) {
            return ToolPolicyDecision.deny("bash is disabled by haifa.ai.deerflow.bash-enabled=false");
        }
        if (properties.getSandbox() == null || !properties.getSandbox().isEnabled()) {
            return ToolPolicyDecision.deny("bash requires haifa.ai.deerflow.sandbox.enabled=true");
        }
        SandboxExecutionPolicy.Decision decision = SandboxExecutionPolicy.evaluate(properties, false);
        return decision.allowed() ? ToolPolicyDecision.allow() : ToolPolicyDecision.deny(decision.reason());
    }

    private ToolPolicyDecision runScriptDecision() {
        if (!properties.isRunScriptEnabled()) {
            return ToolPolicyDecision.deny("run_script is disabled by haifa.ai.deerflow.run-script-enabled=false");
        }
        if (properties.getSandbox() == null || !properties.getSandbox().isEnabled()) {
            return ToolPolicyDecision.deny("run_script requires haifa.ai.deerflow.sandbox.enabled=true");
        }
        SandboxExecutionPolicy.Decision decision = SandboxExecutionPolicy.evaluate(properties, true);
        return decision.allowed() ? ToolPolicyDecision.allow() : ToolPolicyDecision.deny(decision.reason());
    }

    public Set<String> allowedToolsForSkills(List<Skill> activeSkills) {
        Set<String> allowed = new java.util.HashSet<>(builtinToolNames);
        for (String toolName : configuredToolNames()) {
            if (evaluateTool(toolName, activeSkills).allowed()) {
                allowed.add(toolName);
            }
        }
        if (activeSkills != null) {
            for (Skill skill : activeSkills) {
                if (skill.allowedTools() != null) {
                    allowed.addAll(skill.allowedTools());
                }
            }
        }
        return Set.copyOf(allowed);
    }

    private static Set<String> configuredToolNames() {
        return Set.of(
                "web_search", "web_fetch", "image_search", "ls", "read_file", "glob", "grep",
                "write_file", "str_replace", "bash", "list_workspace_files", "read_workspace_file",
                "list_uploaded_files", "read_uploaded_file", "task", "run_script"
        );
    }

}
