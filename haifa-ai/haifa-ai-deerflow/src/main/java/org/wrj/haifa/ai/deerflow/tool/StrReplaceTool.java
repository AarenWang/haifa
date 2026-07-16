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
    public java.util.List<org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract> completionContracts() {
        return java.util.List.of(new org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract(
                org.wrj.haifa.ai.deerflow.completion.CompletionRequirementType.FILE_MUTATION,
                org.wrj.haifa.ai.deerflow.completion.EvidenceType.FILE_CHANGE,
                "workspace text replacement"));
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("str_replace");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isStrReplaceEnabled()) {
            return ToolResult.denied(name(), "Tool str_replace is disabled by security configuration.",
                    java.util.Map.of("denied", true, "reason", "str_replace disabled"));
        }
        try {
            String jsonInput = request.userMessage();
            if (jsonInput == null || jsonInput.isBlank()) {
                return ToolResult.failed(name(), "Error: arguments JSON required");
            }
            JsonNode node;
            try {
                node = MAPPER.readTree(jsonInput);
            } catch (Exception jsonEx) {
                return ToolResult.failed(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
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
                return ToolResult.failed(name(), "Error: path is required");
            }
            if (oldStr == null) {
                return ToolResult.failed(name(), "Error: old_str is required");
            }
            if (newStr == null) {
                return ToolResult.failed(name(), "Error: new_str is required");
            }

            Path resolved = pathResolver.resolveWritable(requestedPath, request.workspaceRoot());

            if (!Files.isRegularFile(resolved)) {
                return ToolResult.failed(name(), "Error: file does not exist: " + requestedPath);
            }

            String content = Files.readString(resolved, StandardCharsets.UTF_8);
            if (!content.contains(oldStr)) {
                return ToolResult.failed(name(), "Error: target string old_str not found in file.");
            }

            // Replace exactly one occurrence or all?
            // "Apply patch" style usually replaces all occurrences or first occurrence. Let's do replaceFirst or replace.
            // Let's replace the first occurrence, which is safer.
            String updatedContent = content.replaceFirst(java.util.regex.Pattern.quote(oldStr), java.util.regex.Matcher.quoteReplacement(newStr));
            Files.writeString(resolved, updatedContent, StandardCharsets.UTF_8);
            return ToolResult.success(name(), "String replaced successfully in: " + requestedPath, java.util.Map.of(
                    "path", resolved.toString(), "virtualPath", pathResolver.toVirtualPath(resolved)));
        } catch (IllegalArgumentException e) {
            return ToolResult.denied(name(), "Security Exception: " + e.getMessage(),
                    java.util.Map.of("denied", true, "reason", e.getMessage()));
        } catch (Exception e) {
            return ToolResult.failed(name(), "Error: " + e.getMessage());
        }
    }
}
