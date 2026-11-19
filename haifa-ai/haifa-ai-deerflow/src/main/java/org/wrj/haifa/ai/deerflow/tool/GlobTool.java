package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class GlobTool implements ParallelSafeAgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_RESULTS = 100;

    @Override
    public String name() {
        return "glob";
    }

    @Override
    public String description() {
        return "Find files in the workspace matching a glob pattern. Arguments: {\"pattern\": \"**/*.md\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("glob");
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
            String pattern = node.has("pattern") ? node.get("pattern").asText() : null;
            if (pattern == null || pattern.isBlank()) {
                return ToolResult.of(name(), "Error: pattern is required");
            }

            // Standardize pattern syntax: if it doesn't start with glob:, add it.
            String syntaxPattern = pattern.startsWith("glob:") ? pattern : "glob:" + pattern;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxPattern);

            Path root = request.workspaceRoot().toAbsolutePath().normalize();
            List<String> matched = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> paths = stream
                        .filter(Files::isRegularFile)
                        .toList();
                for (Path p : paths) {
                    Path rel = root.relativize(p.toAbsolutePath().normalize());
                    // PathMatcher requires matching relative or absolute paths correctly depending on syntax.
                    // We match using relativized path.
                    if (matcher.matches(rel)) {
                        matched.add(rel.toString().replace('\\', '/'));
                    }
                    if (matched.size() >= MAX_RESULTS) {
                        break;
                    }
                }
            }
            return ToolResult.of(name(), matched.isEmpty() ? "No files matched pattern: " + pattern : String.join("\n", matched));
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
