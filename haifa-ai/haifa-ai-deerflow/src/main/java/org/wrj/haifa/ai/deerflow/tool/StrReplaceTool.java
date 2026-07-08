package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class StrReplaceTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DeerFlowProperties properties;
    private final UserDataPathResolver pathResolver;

    public StrReplaceTool(DeerFlowProperties properties) {
        this.properties = properties;
        this.pathResolver = new UserDataPathResolver(properties);
    }

    @Override
    public String name() {
        return "str_replace";
    }

    @Override
    public String description() {
        return "Replace a string in a writable file. Provide path, old_str, and new_str arguments. Writable roots are /mnt/user-data/workspace and /mnt/user-data/outputs.";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("str_replace");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isStrReplaceEnabled()) {
            return ToolResult.of(name(), "Tool str_replace is disabled by security configuration.");
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
            String oldStr = node.has("old_str") ? node.get("old_str").asText() : null;
            String newStr = node.has("new_str") ? node.get("new_str").asText() : null;

            if (requestedPath == null || requestedPath.isBlank()) {
                return ToolResult.of(name(), "Error: path is required");
            }
            if (oldStr == null) {
                return ToolResult.of(name(), "Error: old_str is required");
            }
            if (newStr == null) {
                return ToolResult.of(name(), "Error: new_str is required");
            }

            Path resolved = pathResolver.resolveWritable(requestedPath, request.workspaceRoot());

            if (!Files.isRegularFile(resolved)) {
                return ToolResult.of(name(), "Error: file does not exist: " + requestedPath);
            }

            String content = Files.readString(resolved, StandardCharsets.UTF_8);
            if (!content.contains(oldStr)) {
                return ToolResult.of(name(), "Error: target string old_str not found in file.");
            }

            // Replace exactly one occurrence or all?
            // "Apply patch" style usually replaces all occurrences or first occurrence. Let's do replaceFirst or replace.
            // Let's replace the first occurrence, which is safer.
            String updatedContent = content.replaceFirst(java.util.regex.Pattern.quote(oldStr), java.util.regex.Matcher.quoteReplacement(newStr));
            Files.writeString(resolved, updatedContent, StandardCharsets.UTF_8);
            return ToolResult.of(name(), "String replaced successfully in: " + requestedPath);
        } catch (IllegalArgumentException e) {
            return ToolResult.of(name(), "Security Exception: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.of(name(), "Error: " + e.getMessage());
        }
    }
}
