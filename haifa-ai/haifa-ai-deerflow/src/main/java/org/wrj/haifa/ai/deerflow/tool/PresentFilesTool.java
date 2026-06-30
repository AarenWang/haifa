package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PresentFilesTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "present_files";
    }

    @Override
    public String description() {
        return "Present final deliverables to the user. Arguments: {\"files\": [\"relative/path/to/file1\", \"relative/path/to/file2\"]}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("present_files");
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
            JsonNode filesNode = node.has("files") ? node.get("files") : null;
            if (filesNode == null || !filesNode.isArray()) {
                return ToolResult.of(name(), "Error: files array is required");
            }

            StringBuilder sb = new StringBuilder("Presented the following files to the user:\n");
            for (JsonNode file : filesNode) {
                sb.append("- ").append(file.asText()).append("\n");
            }
            return ToolResult.of(name(), sb.toString().trim());
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
