package org.wrj.haifa.ai.utilitymcp.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpProtocolIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void initializesListsAndCallsStatelessStreamableHttpServer() {
        var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
                .endpoint("/mcp")
                .build();
        try (McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("haifa-test-client", "1.0"))
                .requestTimeout(Duration.ofSeconds(5))
                .build()) {
            McpSchema.InitializeResult initialized = client.initialize();
            assertThat(initialized.protocolVersion()).isEqualTo("2025-11-25");
            assertThat(initialized.serverInfo().name()).isEqualTo("haifa-utility");
            assertThat(initialized.capabilities().tools()).isNotNull();
            assertThat(initialized.capabilities().resources()).isNull();
            assertThat(initialized.capabilities().prompts()).isNull();

            McpSchema.ListToolsResult listed = client.listTools();
            assertThat(listed.tools()).hasSize(19);
            assertThat(listed.tools()).extracting(McpSchema.Tool::name).contains(
                    "time_now", "calculate", "microsoft_docs_search",
                    "microsoft_docs_fetch", "microsoft_code_sample_search");

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    "time_now", Map.of("timezone", "Asia/Shanghai")));
            assertThat(result.isError()).isFalse();
            assertThat(result.structuredContent()).isInstanceOf(Map.class);
            assertThat(result.content()).isNotEmpty();

            McpSchema.CallToolResult invalid = client.callTool(new McpSchema.CallToolRequest(
                    "time_now", Map.of("timezone", "Mars/Olympus")));
            assertThat(invalid.isError()).isTrue();
            assertThat(invalid.content().getFirst()).asString().contains("INVALID_ARGUMENT");
        }
    }
}
