package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AskClarificationTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "ask_clarification";
    }

    @Override
    public String description() {
        return "Ask the user a clarifying question when information is missing or requirements are ambiguous. Arguments: {\"question\": \"Is X correct?\", \"clarification_type\": \"missing_info\", \"context\": \"Need this to start coding\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("ask_clarification");
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
            String question = node.has("question") ? node.get("question").asText() : null;
            if (question == null || question.isBlank()) {
                return ToolResult.of(name(), "Error: question is required");
            }
            return ToolResult.of(name(), "Clarification requested from user:\nQuestion: " + question);
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
