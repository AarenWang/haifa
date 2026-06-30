package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class WriteFileTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;

    public WriteFileTool(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Write content to a file. Arguments: {\"path\": \"relative/path/to/file\", \"content\": \"file content\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("write_file");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isWriteFileEnabled()) {
            return ToolResult.of(name(), "Tool write_file is disabled by security configuration.");
        }
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.of(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                // Natural language fallback
                return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
            }
            String requestedPath = node.has("path") ? node.get("path").asText() : null;
            String content = node.has("content") ? node.get("content").asText() : "";
            if (requestedPath == null || requestedPath.isBlank()) {
                return ToolResult.of(name(), "Error: path is required");
            }
            Path workspacePath = request.workspaceRoot().toAbsolutePath().normalize();
            Path resolved = workspacePath.resolve(requestedPath).normalize();
            Path uploadsPath = Path.of(properties.getUploadsRoot()).toAbsolutePath().normalize();
            Path outputsPath = Path.of(properties.getOutputsRoot()).toAbsolutePath().normalize();

            Path absResolved = resolved.toAbsolutePath().normalize();
            if (!absResolved.startsWith(workspacePath) && !absResolved.startsWith(uploadsPath) && !absResolved.startsWith(outputsPath)) {
                return ToolResult.of(name(), "Security Exception: path is outside allowed directories.");
            }

            Files.createDirectories(resolved.getParent());
            Files.writeString(resolved, content, StandardCharsets.UTF_8);
            return ToolResult.of(name(), "File written successfully: " + requestedPath);
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
