package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.utilitymcp.UtilityMcpServerApplication;

class McpConnectionManagerIntegrationTest {

    private static ConfigurableApplicationContext utilityContext;
    private static int utilityPort;

    @BeforeAll
    static void startUtilityServer() {
        utilityContext = new SpringApplicationBuilder(UtilityMcpServerApplication.class)
                .web(WebApplicationType.SERVLET)
                .properties(Map.ofEntries(
                        Map.entry("server.port", "0"),
                        Map.entry("spring.config.name", "utility-integration-test"),
                        Map.entry("spring.main.banner-mode", "off"),
                        Map.entry("spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"),
                        Map.entry("spring.ai.mcp.server.enabled", "true"),
                        Map.entry("spring.ai.mcp.server.name", "haifa-utility-test"),
                        Map.entry("spring.ai.mcp.server.type", "SYNC"),
                        Map.entry("spring.ai.mcp.server.protocol", "STATELESS"),
                        Map.entry("spring.ai.mcp.server.capabilities.tool", "true"),
                        Map.entry("spring.ai.mcp.server.streamable-http.mcp-endpoint", "/mcp"),
                        Map.entry("logging.level.root", "WARN")))
                .run();
        utilityPort = ((ServletWebServerApplicationContext) utilityContext).getWebServer().getPort();
    }

    @AfterAll
    static void stopUtilityServer() {
        if (utilityContext != null) utilityContext.close();
    }

    @Test
    void discoversNamespacesAndCallsUtilityTools() {
        DeerFlowProperties properties = properties();
        ObjectMapper objectMapper = new ObjectMapper();
        McpConnectionManager manager = new McpConnectionManager(properties, objectMapper, new McpResultMapper(objectMapper));
        try {
            manager.start();

            assertThat(manager.isRunning()).isTrue();
            assertThat(manager.snapshot().toolsByExposedName()).containsKeys(
                    "mcp__utility__time_now", "mcp__utility__calculate");
            assertThat(manager.snapshot().connectionStates().get("utility").state()).isEqualTo(McpConnectionState.READY);
            assertThat(new ToolPolicyService(List.of(), properties, manager)
                    .evaluateTool("mcp__utility__time_now", List.of()).allowed()).isTrue();

            ToolResult time = manager.executeToolResult("mcp__utility__time_now", "{\"timezone\":\"Asia/Shanghai\"}");
            assertThat(time.status()).isEqualTo(ToolResult.Status.SUCCESS);
            assertThat(time.content()).contains("Asia/Shanghai");
            assertThat(time.metadata()).containsEntry("connectionName", "utility")
                    .containsEntry("semanticType", "EXTERNAL_READ");

            ToolResult invalid = manager.executeToolResult("mcp__utility__time_now", "{\"timezone\":\"Mars/Olympus\"}");
            assertThat(invalid.status()).isEqualTo(ToolResult.Status.FAILED);
            assertThat(invalid.content()).contains("INVALID_ARGUMENT");
        }
        finally {
            manager.stop();
        }
    }

    @Test
    void doesNotExposeToolsWhenAllowlistIsEmpty() {
        DeerFlowProperties properties = properties();
        properties.getMcp().getServers().get("utility").setAllowedTools(List.of());
        ObjectMapper objectMapper = new ObjectMapper();
        McpConnectionManager manager = new McpConnectionManager(properties, objectMapper, new McpResultMapper(objectMapper));
        try {
            manager.start();
            assertThat(manager.snapshot().toolsByExposedName()).isEmpty();
        }
        finally {
            manager.stop();
        }
    }

    @Test
    void doesNotExposeAllowlistedToolsWithoutTrustedLocalRisk() {
        DeerFlowProperties properties = properties();
        properties.getMcp().getServers().get("utility").setDefaultRisk("UNKNOWN");
        ObjectMapper objectMapper = new ObjectMapper();
        McpConnectionManager manager = new McpConnectionManager(properties, objectMapper, new McpResultMapper(objectMapper));
        try {
            manager.start();
            assertThat(manager.snapshot().toolsByExposedName()).isEmpty();
        }
        finally {
            manager.stop();
        }
    }

    @Test
    void normalizesNamesWithoutRuntimeStringRouting() {
        assertThat(McpConnectionManager.normalize(" Open Meteo ")).isEqualTo("open-meteo");
    }

    private static DeerFlowProperties properties() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getMcp().setEnabled(true);
        DeerFlowProperties.McpServer utility = new DeerFlowProperties.McpServer();
        utility.setEnabled(true);
        utility.setRequired(true);
        utility.setTransport("STREAMABLE_HTTP");
        utility.setUrl("http://127.0.0.1:" + utilityPort);
        utility.setAllowedTools(List.of("time_now", "calculate"));
        utility.setDefaultRisk("READ_ONLY");
        utility.setSemanticMappings(Map.of("time_now", "EXTERNAL_READ", "calculate", "GENERIC"));
        utility.setCapabilityMappings(Map.of("time_now", "TIME", "calculate", "CALCULATION"));
        properties.getMcp().setServers(Map.of("utility", utility));
        properties.getMcp().setCapabilityOwners(Map.of("TIME", "utility", "CALCULATION", "utility"));
        return properties;
    }
}
