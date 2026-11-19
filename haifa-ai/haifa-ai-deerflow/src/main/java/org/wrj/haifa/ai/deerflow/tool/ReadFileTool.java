package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class ReadFileTool implements ParallelSafeAgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CHARS = 15_000;
    private final UserDataPathResolver pathResolver;

    public ReadFileTool(DeerFlowProperties properties) {
        this.pathResolver = new UserDataPathResolver(properties);
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read the content of a file. Provide a path argument. Supports relative workspace paths and /mnt/user-data uploads, workspace, or outputs virtual paths.";
    }

    @Override
    public String inputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "path": {
                      "type": "string",
                      "description": "Relative workspace path, or a /mnt/user-data/uploads, /mnt/user-data/workspace, or /mnt/user-data/outputs virtual path."
                    }
                  },
                  "required": ["path"],
                  "additionalProperties": false
                }
                """;
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("read_file");
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
            String requestedPath = null;
            if (node.has("path")) {
                requestedPath = node.get("path").asText();
            } else if (node.has("filepath")) {
                requestedPath = node.get("filepath").asText();
            } else if (node.has("file_path")) {
                requestedPath = node.get("file_path").asText();
            } else if (node.has("file")) {
                requestedPath = node.get("file").asText();
            }

            if (requestedPath == null || requestedPath.isBlank()) {
                return ToolResult.of(name(), "Error: path is required");
            }

            Path resolved = pathResolver.resolveReadable(requestedPath, request.workspaceRoot());

            if (!Files.exists(resolved)) {
                return ToolResult.of(name(), "Error: file does not exist: " + requestedPath);
            }

            if (Files.isDirectory(resolved)) {
                return ToolResult.of(name(), "Error: path is a directory: " + requestedPath);
            }

            String content = Files.readString(resolved, StandardCharsets.UTF_8);
            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS) + "\n...[truncated]";
            }
            return ToolResult.of(name(), content);
        } catch (IllegalArgumentException e) {
            return ToolResult.of(name(), "Security Exception: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
