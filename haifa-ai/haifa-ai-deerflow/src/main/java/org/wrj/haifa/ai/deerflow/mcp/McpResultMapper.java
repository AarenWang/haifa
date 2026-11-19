package org.wrj.haifa.ai.deerflow.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Component
public class McpResultMapper {

    private static final int MAX_TEXT_CHARS = 64_000;
    private static final int MAX_STRUCTURED_BYTES = 128_000;
    private final ObjectMapper objectMapper;
    private final ArtifactService artifactService;

    public McpResultMapper(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Autowired
    public McpResultMapper(ObjectMapper objectMapper, ArtifactService artifactService) {
        this.objectMapper = objectMapper;
        this.artifactService = artifactService;
    }

    public ToolResult map(McpToolIdentity identity, McpSchema.CallToolResult result) {
        return map(identity, result, null, null);
    }

    public ToolResult map(McpToolIdentity identity, McpSchema.CallToolResult result, String threadId, String runId) {
        StringBuilder content = new StringBuilder();
        List<Map<String, Object>> richContent = new ArrayList<>();
        for (McpSchema.Content item : result.content() == null ? List.<McpSchema.Content>of() : result.content()) {
            if (item instanceof McpSchema.TextContent text) {
                appendBounded(content, text.text());
            }
            else if (item instanceof McpSchema.ImageContent image) {
                richContent.add(storeOrOmit("image", image.mimeType(), image.data(), threadId, runId));
            }
            else if (item instanceof McpSchema.AudioContent audio) {
                richContent.add(storeOrOmit("audio", audio.mimeType(), audio.data(), threadId, runId));
            }
            else if (item instanceof McpSchema.ResourceLink link) {
                Map<String, Object> safeLink = safeResourceLink(link);
                if (!safeLink.isEmpty()) richContent.add(safeLink);
            }
            else if (item instanceof McpSchema.EmbeddedResource embedded) {
                richContent.add(Map.of("type", "embedded-resource", "value", truncate(String.valueOf(embedded.resource()), 4_000)));
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "mcp");
        metadata.put("connectionName", identity.connectionName());
        metadata.put("originalToolName", identity.originalToolName());
        metadata.put("exposedName", identity.exposedName());
        metadata.put("snapshotVersion", identity.discoverySnapshotVersion());
        metadata.put("risk", identity.localRiskClassification().name());
        metadata.put("semanticType", identity.semanticType().name());
        if (identity.capabilityOwner() != null) metadata.put("capabilityOwner", identity.capabilityOwner().name());
        if (!richContent.isEmpty()) metadata.put("richContent", List.copyOf(richContent));

        Object structured = result.structuredContent();
        if (structured != null) {
            try {
                String json = objectMapper.writeValueAsString(structured);
                if (json.getBytes(StandardCharsets.UTF_8).length <= MAX_STRUCTURED_BYTES) {
                    metadata.put("structuredContent", structured);
                    if (content.isEmpty()) appendBounded(content, json);
                }
                else {
                    metadata.put("structuredContentOmitted", true);
                }
            }
            catch (JsonProcessingException ex) {
                metadata.put("structuredContentInvalid", true);
            }
        }
        boolean truncated = content.length() >= MAX_TEXT_CHARS;
        metadata.put("truncated", truncated);
        metadata.put("errorType", Boolean.TRUE.equals(result.isError()) ? "TOOL_FAILED" : "NONE");
        String safeContent = content.isEmpty() ? (Boolean.TRUE.equals(result.isError()) ? "MCP tool failed" : "MCP tool returned no text") : content.toString();
        return Boolean.TRUE.equals(result.isError())
                ? ToolResult.failed(identity.exposedName(), safeContent, metadata)
                : ToolResult.success(identity.exposedName(), safeContent, metadata);
    }

    private Map<String, Object> storeOrOmit(String type, String mimeType, String base64, String threadId, String runId) {
        String mime = safeMime(mimeType);
        int estimated = decodedSizeEstimate(base64);
        if (artifactService != null && StringUtils.hasText(threadId) && StringUtils.hasText(runId)
                && estimated > 0 && estimated <= 10 * 1024 * 1024) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(base64);
                var artifact = artifactService.storeMcpContent(threadId, runId, decoded, mime);
                return Map.of("type", type, "mimeType", mime, "artifactId", artifact.artifactId(),
                        "bytes", artifact.size());
            }
            catch (IllegalArgumentException ex) {
                return Map.of("type", type, "mimeType", mime, "artifactRejected", true,
                        "estimatedBytes", estimated);
            }
        }
        return Map.of("type", type, "mimeType", mime, "omittedBase64Bytes", estimated);
    }

    private static Map<String, Object> safeResourceLink(McpSchema.ResourceLink link) {
        try {
            URI uri = URI.create(link.uri());
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) return Map.of();
            if (uri.getHost() == null || uri.getUserInfo() != null) return Map.of();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "resource-link");
            result.put("uri", uri.toString());
            if (link.name() != null) result.put("name", truncate(link.name(), 200));
            if (link.mimeType() != null) result.put("mimeType", safeMime(link.mimeType()));
            if (link.size() != null) result.put("size", link.size());
            return result;
        }
        catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private static void appendBounded(StringBuilder builder, String value) {
        if (value == null || builder.length() >= MAX_TEXT_CHARS) return;
        if (!builder.isEmpty()) builder.append('\n');
        int remaining = MAX_TEXT_CHARS - builder.length();
        builder.append(value, 0, Math.min(value.length(), remaining));
    }

    private static int decodedSizeEstimate(String base64) {
        if (base64 == null) return 0;
        return Math.max(0, base64.length() * 3 / 4);
    }

    private static String safeMime(String mime) {
        return mime != null && mime.matches("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+") ? mime : "application/octet-stream";
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
