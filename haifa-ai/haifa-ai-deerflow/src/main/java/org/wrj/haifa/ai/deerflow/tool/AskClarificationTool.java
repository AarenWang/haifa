package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;

import java.util.Map;

@Component
public class AskClarificationTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ClarificationStore clarificationStore;

    public AskClarificationTool(ClarificationStore clarificationStore) {
        this.clarificationStore = clarificationStore;
    }

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

            String type = node.has("clarification_type") ? node.get("clarification_type").asText() : "missing_info";
            String context = node.has("context") ? node.get("context").asText() : "";

            ClarificationRecord record = clarificationStore.create(
                    request.threadId(),
                    request.runId(),
                    question,
                    type,
                    context
            );

            return ToolResult.of(
                    name(),
                    "Clarification requested from user:\nQuestion: " + question,
                    Map.of(
                            "clarificationRequired", true,
                            "clarificationId", record.clarificationId(),
                            "question", question,
                            "clarificationType", type
                    )
            );
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
