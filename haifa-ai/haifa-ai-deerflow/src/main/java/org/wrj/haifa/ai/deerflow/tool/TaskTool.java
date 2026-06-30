package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TaskTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "task";
    }

    @Override
    public String description() {
        return "Delegate a complex sub-task to a subagent. Arguments: {\"description\": \"subagent purpose\", \"prompt\": \"detailed prompt for subagent\", \"subagent_type\": \"general-purpose\"}";
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
            String desc = node.has("description") ? node.get("description").asText() : "";
            if (prompt == null || prompt.isBlank()) {
                return ToolResult.of(name(), "Error: prompt is required");
            }
            // Mock subagent response for Phase 3
            return ToolResult.of(name(), "Subagent delegated successfully: " + desc + "\nMock response: Task completed successfully.");
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
