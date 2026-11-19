package org.wrj.haifa.ai.deerflow.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ReadWorkspaceFileTool implements ParallelSafeAgentTool {

    private static final int MAX_CHARS = 12_000;
    private static final Pattern QUOTED_PATH = Pattern.compile("[\"'`]([^\"'`]+)[\"'`]");
    private static final Pattern READ_PATH = Pattern.compile("(?i)(?:read|open|查看|读取)\\s+([^\\s,;]+)");

    /**
     * Pattern to extract JSON string values for keys like "filepath", "file_path", "path", or "file".
     * Matches: "filepath":"pom.xml" or "path": "src/main/resources/app.yml" etc.
     */
    private static final Pattern JSON_PATH_VALUE = Pattern.compile(
            "\"(?:filepath|file_path|path|file)\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public String name() {
        return "read_workspace_file";
    }

    @Override
    public String description() {
        return "Reads a UTF-8 text file inside the configured workspace root.";
    }

    @Override
    public boolean supports(String userMessage) {
        String text = userMessage == null ? "" : userMessage.toLowerCase();
        return text.contains("read ") || text.contains("open ") || text.contains("查看") || text.contains("读取");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        Optional<String> pathCandidate = extractPath(request.userMessage());
        if (pathCandidate.isEmpty()) {
            return ToolResult.of(name(), "No readable file path was found in the user message.");
        }
        try {
            Path path = PathPolicy.resolveExistingInsideWorkspace(request.workspaceRoot(), pathCandidate.get());
            if (!Files.isRegularFile(path)) {
                return ToolResult.of(name(), "Path is not a regular file: " + pathCandidate.get());
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS) + "\n...[truncated]";
            }
            return ToolResult.of(name(), content);
        }
        catch (IllegalArgumentException | IOException ex) {
            return ToolResult.of(name(), "Failed to read file: " + ex.getMessage());
        }
    }

    private static Optional<String> extractPath(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        // 1. Try parsing as JSON tool arguments (e.g. {"filepath":"pom.xml"})
        String trimmed = userMessage.trim();
        if (trimmed.startsWith("{")) {
            Matcher jsonMatcher = JSON_PATH_VALUE.matcher(trimmed);
            if (jsonMatcher.find()) {
                return Optional.of(jsonMatcher.group(1));
            }
        }

        // 2. Fallback: extract from natural language with quoted path
        Matcher quoted = QUOTED_PATH.matcher(userMessage);
        if (quoted.find()) {
            return Optional.of(quoted.group(1));
        }

        // 3. Fallback: extract from natural language with read/open keywords
        Matcher read = READ_PATH.matcher(userMessage);
        if (read.find()) {
            return Optional.of(read.group(1));
        }

        return Optional.empty();
    }
}
