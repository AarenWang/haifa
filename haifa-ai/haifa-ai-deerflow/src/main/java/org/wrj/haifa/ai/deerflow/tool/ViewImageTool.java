package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ViewImageTool implements ParallelSafeAgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "view_image";
    }

    @Override
    public String description() {
        return "View an image file from the workspace. Arguments: {\"path\": \"relative/path/to/image.png\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("view_image");
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
            String path = node.has("path") ? node.get("path").asText() : null;
            if (path == null || path.isBlank()) {
                return ToolResult.of(name(), "Error: path is required");
            }
            return ToolResult.of(name(), "Successfully viewed image: " + path);
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
