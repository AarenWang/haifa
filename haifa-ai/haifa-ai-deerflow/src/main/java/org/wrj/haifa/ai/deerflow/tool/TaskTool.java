package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRegistry;
import org.wrj.haifa.ai.deerflow.subagent.SubagentResult;
import org.wrj.haifa.ai.deerflow.subagent.SubagentRuntime;

/**
 * Upgraded TaskTool that delegates to a real SubagentRuntime instead of returning a mock response.
 *
 * <p>Model-visible parameters:
 * <ul>
 *   <li>{@code description} — short (3-5 word) description for logging/display</li>
 *   <li>{@code prompt} — detailed prompt for the subagent</li>
 *   <li>{@code subagent_type} (Java alias {@code subagentType}) — type of subagent</li>
 *   <li>{@code context} — optional parent context snapshot</li>
 *   <li>{@code allowed_tools} / {@code toolGroups} — optional tool restriction</li>
 *   <li>{@code model} — optional model override (inherits parent model by default)</li>
 *   <li>{@code timeout} — optional timeout in seconds</li>
 *   <li>{@code max_turns} — optional max turns</li>
 * </ul>
 *
 * <p>Security rules:
 * <ul>
 *   <li>Unknown {@code subagent_type} returns a structured failure with available types</li>
 *   <li>Tool permissions are intersected with parent policy — subagent never gets more tools than parent</li>
 *   <li>Subagent tool set always excludes {@code task} to prevent recursive nesting</li>
 * </ul>
 */
@Component
public class TaskTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SubagentRuntime subagentRuntime;
    private final SubagentRegistry subagentRegistry;

    public TaskTool(SubagentRuntime subagentRuntime, SubagentRegistry subagentRegistry) {
        this.subagentRuntime = subagentRuntime;
        this.subagentRegistry = subagentRegistry;
    }

    @Override
    public String name() {
        return "task";
    }

    @Override
    public String description() {
        return "Delegate a complex sub-task to a subagent. Arguments: {"
                + "\"description\": \"subagent purpose\", "
                + "\"prompt\": \"detailed prompt for subagent\", "
                + "\"subagent_type\": \"general-purpose\", "
                + "\"allowed_tools\": [\"web_search\",\"web_fetch\"], "
                + "\"model\": \"inherit\", "
                + "\"timeout\": 900, "
                + "\"max_turns\": 50}"
                + "\nAvailable subagent types: " + String.join(", ", subagentRegistry.getAvailableNames());
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("task");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.of(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }

            String prompt = node.has("prompt") ? node.get("prompt").asText() : null;
            String description = node.has("description") ? node.get("description").asText() : "";
            if (prompt == null || prompt.isBlank()) {
                return ToolResult.of(name(), "Error: prompt is required");
            }

            String subagentType = node.has("subagent_type") ? node.get("subagent_type").asText()
                    : (node.has("subagentType") ? node.get("subagentType").asText() : "general-purpose");

            // Validate subagent type
            if (!subagentRegistry.isAvailable(subagentType)) {
                String available = String.join(", ", subagentRegistry.getAvailableNames());
                return ToolResult.of(name(), "Error: Unknown subagent type '" + subagentType
                        + "'. Available types: " + available);
            }

            // Extract optional parameters
            List<String> allowedTools = parseStringList(node, "allowed_tools");
            String modelOverride = node.has("model") ? node.get("model").asText() : null;
            Integer timeout = node.has("timeout") ? node.get("timeout").asInt() : null;
            Integer maxTurns = node.has("max_turns") ? node.get("max_turns").asInt() : null;

            // Run subagent if runtime is available
            if (subagentRuntime != null) {
                SubagentResult result = subagentRuntime.execute(
                        description, prompt, subagentType,
                        request.threadId(), request.runId(),
                        allowedTools, modelOverride, timeout, maxTurns,
                        request.mode(), request.activeSkills()
                );

                return formatSubagentResult(result);
            }

            // Fallback when runtime is not wired (should not happen in production)
            return ToolResult.of(name(), "Subagent runtime not available. "
                    + "Would delegate: [" + subagentType + "] " + description);

        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }

    private ToolResult formatSubagentResult(SubagentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Subagent result [").append(result.status()).append("]\n");
        if (!result.summary().isBlank()) {
            sb.append("Summary:\n").append(result.summary()).append("\n");
        }
        if (!result.sourceIds().isEmpty()) {
            sb.append("Sources found: ").append(String.join(", ", result.sourceIds())).append("\n");
        }
        if (!result.evidenceIds().isEmpty()) {
            sb.append("Evidence extracted: ").append(String.join(", ", result.evidenceIds())).append("\n");
        }
        if (!result.error().isBlank()) {
            sb.append("Error: ").append(result.error()).append("\n");
        }
        sb.append("Duration: ").append(result.durationMs()).append("ms\n");
        if (!result.tokenUsage().isEmpty()) {
            sb.append("Token usage: ").append(result.tokenUsage()).append("\n");
        }

        return ToolResult.of(name(), sb.toString().trim(), Map.of(
                "subagent", true,
                "status", result.status(),
                "subagentStatus", result.status(),
                "taskId", result.taskId(),
                "parentRunId", result.parentRunId(),
                "sourceIds", result.sourceIds(),
                "evidenceIds", result.evidenceIds(),
                "durationMs", result.durationMs()
        ));
    }

    private List<String> parseStringList(JsonNode node, String fieldName) {
        if (!node.has(fieldName)) {
            return null;
        }
        JsonNode arr = node.get(fieldName);
        if (!arr.isArray()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : arr) {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        }
        return result.isEmpty() ? null : result;
    }
}
