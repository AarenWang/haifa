package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class LsTool implements ParallelSafeAgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_FILES = 100;
    private final UserDataPathResolver pathResolver;

    public LsTool(DeerFlowProperties properties) {
        this.pathResolver = new UserDataPathResolver(properties);
    }

    @Override
    public String name() {
        return "ls";
    }

    @Override
    public String description() {
        return "List files and directories. Optional path supports relative workspace paths and /mnt/user-data virtual roots.";
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

            Path resolved = requestedPath == null || requestedPath.isBlank()
                    ? pathResolver.workspaceRoot()
                    : pathResolver.resolveReadable(requestedPath, request.workspaceRoot());

            if (!Files.exists(resolved)) {
                return ToolResult.notFound(name(), "Error: path does not exist: " + requestedPath);
            }

            if (!Files.isDirectory(resolved)) {
                return ToolResult.success(name(), "Path is a file: " + requestedPath + "\nSize: " + Files.size(resolved) + " bytes", java.util.Map.of());
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
                return ToolResult.success(name(), content.isBlank() ? "(directory is empty)" : content, java.util.Map.of());
            }
        } catch (IllegalArgumentException e) {
            return ToolResult.denied(name(), "Security Exception: " + e.getMessage(),
                    java.util.Map.of("denied", true, "reason", e.getMessage()));
        } catch (Exception e) {
            return ToolResult.failed(name(), "Error: " + e.getMessage());
        }
    }
}
