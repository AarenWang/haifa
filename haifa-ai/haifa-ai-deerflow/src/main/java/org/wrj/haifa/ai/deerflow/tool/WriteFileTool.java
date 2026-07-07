package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class WriteFileTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;
    private final ArtifactService artifactService;

    public WriteFileTool(DeerFlowProperties properties, ArtifactService artifactService) {
        this.properties = properties;
        this.artifactService = artifactService;
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Write content to a file. Arguments: {\"path\": \"relative/path/to/file\", \"content\": \"file content\"}. For user-facing deliverables, write under outputs/ so the file is registered as a downloadable artifact.";
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
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("path", absResolved.toString());
            if (StringUtils.hasText(request.threadId()) && StringUtils.hasText(request.runId())
                    && absResolved.startsWith(outputsPath)) {
                ArtifactRecord artifact = artifactService.register(request.threadId(), request.runId(), absResolved, null);
                String downloadUrl = "/api/deerflow/artifacts/" + artifact.artifactId() + "/download";
                metadata.put("artifactId", artifact.artifactId());
                metadata.put("filename", artifact.filename());
                metadata.put("mimeType", artifact.mimeType());
                metadata.put("size", artifact.size());
                metadata.put("downloadUrl", downloadUrl);
                return ToolResult.of(name(),
                        "File written and registered for download: " + artifact.filename()
                                + " (" + downloadUrl + ")",
                        metadata);
            }
            return ToolResult.of(name(), "File written successfully: " + requestedPath, metadata);
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
