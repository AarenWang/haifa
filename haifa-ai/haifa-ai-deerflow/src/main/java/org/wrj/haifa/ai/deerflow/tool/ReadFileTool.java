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
public class ReadFileTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CHARS = 15_000;
    private final DeerFlowProperties properties;

    public ReadFileTool(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read the content of a file. Arguments: {\"path\": \"relative/path/to/file\"}";
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

            Path resolved;
            Path workspacePath = request.workspaceRoot().toAbsolutePath().normalize();
            final String VIRTUAL_OUTPUTS_BASE = "/mnt/user-data/outputs";
            if (requestedPath.startsWith(VIRTUAL_OUTPUTS_BASE + "/")) {
                // Virtual path from ToolOutputBudgetMiddleware externalization
                String relativePath = requestedPath.substring(VIRTUAL_OUTPUTS_BASE.length() + 1);
                resolved = Path.of(properties.getOutputsRoot()).resolve(relativePath).normalize();
            } else {
                resolved = workspacePath.resolve(requestedPath).normalize();
            }

            Path uploadsPath = Path.of(properties.getUploadsRoot()).toAbsolutePath().normalize();
            Path outputsPath = Path.of(properties.getOutputsRoot()).toAbsolutePath().normalize();
            Path skillsPath = Path.of(properties.getSkillsRoot()).toAbsolutePath().normalize();

            Path absResolved = resolved.toAbsolutePath().normalize();
            if (!absResolved.startsWith(workspacePath)
                    && !absResolved.startsWith(uploadsPath)
                    && !absResolved.startsWith(outputsPath)
                    && !absResolved.startsWith(skillsPath)) {
                return ToolResult.of(name(), "Security Exception: path is outside allowed directories.");
            }

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
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
