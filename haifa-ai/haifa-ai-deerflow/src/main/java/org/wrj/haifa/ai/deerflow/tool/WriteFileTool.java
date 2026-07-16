package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class WriteFileTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico",
            "zip", "gz", "tar", "7z", "rar", "jar", "class", "exe", "dll");
    private final DeerFlowProperties properties;
    private final ArtifactService artifactService;
    private final UserDataPathResolver pathResolver;

    public WriteFileTool(DeerFlowProperties properties, ArtifactService artifactService) {
        this.properties = properties;
        this.artifactService = artifactService;
        this.pathResolver = new UserDataPathResolver(properties);
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Write UTF-8 text content to a text file. Provide path and content arguments. Relative paths write under /mnt/user-data/workspace. User-facing text deliverables should use /mnt/user-data/outputs. Binary formats such as PDF, Office documents, archives, and images must be created by a format-specific tool or script. Do not write to uploads.";
    }

    @Override
    public java.util.List<org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract> completionContracts() {
        return java.util.List.of(new org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract(
                org.wrj.haifa.ai.deerflow.completion.CompletionRequirementType.FILE_MUTATION,
                org.wrj.haifa.ai.deerflow.completion.EvidenceType.FILE_CHANGE,
                "workspace file write"));
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("write_file");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!properties.isWriteFileEnabled()) {
            return ToolResult.denied(name(), "Tool write_file is disabled by security configuration.",
                    Map.of("denied", true, "reason", "write_file disabled"));
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
                // Natural language fallback
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
            String content = node.has("content") ? node.get("content").asText() : "";
            if (requestedPath == null || requestedPath.isBlank()) {
                return ToolResult.failed(name(), "Error: path is required");
            }
            if (hasBinaryExtension(requestedPath)) {
                return ToolResult.failed(name(), "Error: write_file only writes UTF-8 text and cannot create binary file '"
                        + requestedPath + "'. Use a format-specific generator and verify the resulting file instead.",
                        Map.of("error", true, "reason", "BINARY_FORMAT_REQUIRES_GENERATOR"));
            }
            Path resolved = pathResolver.resolveWritable(requestedPath, request.workspaceRoot());
            Path outputsPath = pathResolver.outputsRoot();

            Files.createDirectories(resolved.getParent());
            Files.writeString(resolved, content, StandardCharsets.UTF_8);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("path", resolved.toString());
            metadata.put("virtualPath", pathResolver.toVirtualPath(resolved));
            if (StringUtils.hasText(request.threadId()) && StringUtils.hasText(request.runId())
                    && resolved.startsWith(outputsPath)) {
                ArtifactRecord artifact = artifactService.register(request.threadId(), request.runId(), resolved, null);
                String downloadUrl = "/api/deerflow/artifacts/" + artifact.artifactId() + "/download";
                metadata.put("artifactId", artifact.artifactId());
                metadata.put("filename", artifact.filename());
                metadata.put("mimeType", artifact.mimeType());
                metadata.put("size", artifact.size());
                metadata.put("downloadUrl", downloadUrl);
                return ToolResult.success(name(),
                        "File written and registered for download: " + artifact.filename()
                                + " (" + downloadUrl + ")",
                        metadata);
            }
            return ToolResult.success(name(), "File written successfully: " + requestedPath, metadata);
        } catch (IllegalArgumentException e) {
            return ToolResult.denied(name(), "Security Exception: " + e.getMessage(),
                    Map.of("denied", true, "reason", e.getMessage()));
        } catch (Exception e) {
            return ToolResult.failed(name(), "Error: " + e.getMessage());
        }
    }

    private static boolean hasBinaryExtension(String path) {
        String filename = Path.of(path.replace('\\', '/')).getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 && BINARY_EXTENSIONS.contains(filename.substring(dot + 1));
    }
}
