package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class LsTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_FILES = 100;
    private final DeerFlowProperties properties;

    public LsTool(DeerFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "ls";
    }

    @Override
    public String description() {
        return "List files and directories. Arguments: {\"path\": \"relative/path\"} (path is optional)";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("ls");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        try {
            String jsonInput = request.userMessage();
            String requestedPath = "";
            if (jsonInput != null && !jsonInput.isBlank() && jsonInput.trim().startsWith("{")) {
                try {
                    JsonNode node = MAPPER.readTree(jsonInput);
                    if (node.has("path")) {
                        requestedPath = node.get("path").asText();
                    }
                } catch (Exception jsonEx) {
                    // Ignore and use default
                }
            }

            Path workspacePath = request.workspaceRoot().toAbsolutePath().normalize();
            Path resolved = workspacePath.resolve(requestedPath).normalize();
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
                return ToolResult.of(name(), "Error: path does not exist: " + requestedPath);
            }

            if (!Files.isDirectory(resolved)) {
                return ToolResult.of(name(), "Path is a file: " + requestedPath + "\nSize: " + Files.size(resolved) + " bytes");
            }

            try (Stream<Path> paths = Files.list(resolved)) {
                String content = paths
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .limit(MAX_FILES)
                        .map(path -> {
                            String suffix = Files.isDirectory(path) ? "/" : "";
                            return resolved.relativize(path).toString().replace('\\', '/') + suffix;
                        })
                        .collect(Collectors.joining("\n"));
                return ToolResult.of(name(), content.isBlank() ? "(directory is empty)" : content);
            }
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
