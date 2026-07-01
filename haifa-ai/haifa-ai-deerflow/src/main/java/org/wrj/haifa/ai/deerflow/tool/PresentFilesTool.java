package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;

@Component
public class PresentFilesTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ArtifactService artifactService;

    public PresentFilesTool(ArtifactService artifactService) {
        this.artifactService = artifactService;
    }

    @Override
    public String name() {
        return "present_files";
    }

    @Override
    public String description() {
        return "Present registered research artifacts to the user. Arguments: {\"files\": [\"artifactId-or-filename.md\"]}. Only already registered artifacts are allowed.";
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

            StringBuilder sb = new StringBuilder("Presented registered artifacts:\n");
            for (JsonNode file : filesNode) {
                String requested = file.asText();
                java.util.Optional<ArtifactRecord> artifact = artifactService.findVisible(request.threadId(), requested);
                if (artifact.isEmpty()) {
                    return ToolResult.of(name(), "Error: file is not a registered artifact: " + requested);
                }
                ArtifactRecord record = artifact.get();
                sb.append("- ")
                        .append(record.filename())
                        .append(" (artifactId=")
                        .append(record.artifactId())
                        .append(", download=/api/deerflow/artifacts/")
                        .append(record.artifactId())
                        .append("/download)\n");
            }
            return ToolResult.of(name(), sb.toString().trim());
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
