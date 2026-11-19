package org.wrj.haifa.ai.utilitymcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

public class ToolResultFactory {

    private final ObjectMapper objectMapper;
    private final int maxResultBytes;

    public ToolResultFactory(ObjectMapper objectMapper, UtilityMcpProperties properties) {
        this.objectMapper = objectMapper;
        this.maxResultBytes = properties.getMaxResultBytes();
    }

    public McpSchema.CallToolResult success(UtilityResult result) {
        return build(result.asMap(), false);
    }

    public McpSchema.CallToolResult error(UtilityToolException error) {
        return build(Map.of("error", Map.of(
                "code", error.code().name(),
                "message", safeMessage(error.getMessage()),
                "retryable", error.retryable())), true);
    }

    public McpSchema.CallToolResult error(Throwable error) {
        return error(new UtilityToolException(
                UtilityErrorCode.UPSTREAM_UNAVAILABLE,
                "The utility tool could not complete the request",
                true));
    }

    private McpSchema.CallToolResult build(Map<String, Object> structured, boolean isError) {
        try {
            String json = objectMapper.writeValueAsString(structured);
            if (json.getBytes(StandardCharsets.UTF_8).length > maxResultBytes) {
                if (!isError) {
                    return error(new UtilityToolException(
                            UtilityErrorCode.RESULT_TOO_LARGE,
                            "Tool result exceeds the configured size limit",
                            false));
                }
                json = "{\"error\":{\"code\":\"RESULT_TOO_LARGE\",\"message\":\"Tool result exceeds the configured size limit\",\"retryable\":false}}";
                structured = Map.of("error", Map.of(
                        "code", "RESULT_TOO_LARGE",
                        "message", "Tool result exceeds the configured size limit",
                        "retryable", false));
            }
            return McpSchema.CallToolResult.builder()
                    .structuredContent(structured)
                    .addTextContent(json)
                    .isError(isError)
                    .build();
        }
        catch (JsonProcessingException ex) {
            return new McpSchema.CallToolResult("{\"error\":{\"code\":\"UPSTREAM_UNAVAILABLE\",\"message\":\"Result serialization failed\",\"retryable\":false}}", true);
        }
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Utility tool execution failed";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
