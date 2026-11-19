package org.wrj.haifa.ai.deerflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

class McpConnectionFailurePolicyTest {

    @Test
    void optionalFailureIsDegradedButRequiredFailureFailsStartup() {
        DeerFlowProperties optionalProperties = properties(false);
        McpConnectionManager optional = manager(optionalProperties);
        optional.start();
        assertThat(optional.isRunning()).isTrue();
        assertThat(optional.snapshot().connectionStates().get("offline").state()).isEqualTo(McpConnectionState.DEGRADED);
        optional.stop();

        McpConnectionManager required = manager(properties(true));
        assertThatThrownBy(required::start).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("offline");
        assertThat(required.isRunning()).isFalse();
    }

    private static DeerFlowProperties properties(boolean required) {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getMcp().setEnabled(true);
        DeerFlowProperties.McpServer server = new DeerFlowProperties.McpServer();
        server.setEnabled(true);
        server.setRequired(required);
        server.setUrl("http://127.0.0.1:1");
        server.setRequestTimeoutMs(1_000);
        server.setAllowedTools(List.of("read"));
        server.setDefaultRisk("READ_ONLY");
        properties.getMcp().setServers(Map.of("offline", server));
        return properties;
    }

    private static McpConnectionManager manager(DeerFlowProperties properties) {
        ObjectMapper mapper = new ObjectMapper();
        return new McpConnectionManager(properties, mapper, new McpResultMapper(mapper));
    }
}
