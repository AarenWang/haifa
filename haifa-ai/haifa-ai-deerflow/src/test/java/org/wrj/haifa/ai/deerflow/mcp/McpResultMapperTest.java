package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

class McpResultMapperTest {

    @TempDir
    Path tempDir;

    private final McpResultMapper mapper = new McpResultMapper(new ObjectMapper());
    private final McpToolIdentity identity = new McpToolIdentity(
            "mcp__wiki__summary", "wiki", "summary", 7, "summary", "{}", null,
            McpRiskClassification.READ_ONLY, McpSemanticType.KNOWLEDGE_FETCH,
            McpCapabilityOwner.ENCYCLOPEDIA_FETCH);

    @Test
    void preservesStructuredContentAndSafeResourceLinks() {
        McpSchema.ResourceLink link = McpSchema.ResourceLink.builder()
                .name("source")
                .uri("https://example.com/page")
                .mimeType("text/html")
                .build();
        McpSchema.CallToolResult source = new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("answer"), link), false,
                Map.of("title", "Example"), Map.of());

        ToolResult result = mapper.map(identity, source);

        assertThat(result.status()).isEqualTo(ToolResult.Status.SUCCESS);
        assertThat(result.content()).isEqualTo("answer");
        assertThat(result.metadata()).containsEntry("structuredContent", Map.of("title", "Example"));
        assertThat(result.metadata().get("richContent").toString()).contains("https://example.com/page");
    }

    @Test
    void mapsMcpToolErrorsToFailed() {
        ToolResult result = mapper.map(identity, new McpSchema.CallToolResult("invalid argument", true));
        assertThat(result.status()).isEqualTo(ToolResult.Status.FAILED);
        assertThat(result.content()).contains("invalid argument");
    }

    @Test
    void storesImageContentAsArtifactInsteadOfMessageBase64() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setOutputsRoot(tempDir.resolve("outputs").toString());
        ArtifactService artifacts = new ArtifactService(properties);
        McpResultMapper artifactMapper = new McpResultMapper(new ObjectMapper(), artifacts);
        String png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        McpSchema.CallToolResult source = new McpSchema.CallToolResult(
                List.of(new McpSchema.ImageContent((McpSchema.Annotations) null, png, "image/png")), false);

        ToolResult result = artifactMapper.map(identity, source, "thread-1", "run-1");

        assertThat(result.metadata().get("richContent").toString()).contains("artifactId").doesNotContain(png);
        assertThat(artifacts.list("thread-1", "run-1")).hasSize(1);
    }
}
