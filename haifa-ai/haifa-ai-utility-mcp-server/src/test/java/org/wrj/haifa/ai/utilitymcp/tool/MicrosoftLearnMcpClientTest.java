package org.wrj.haifa.ai.utilitymcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityErrorCode;
import org.wrj.haifa.ai.utilitymcp.mcp.UtilityToolException;

class MicrosoftLearnMcpClientTest {

    @Test
    void rejectsNonHttpsRemoteEndpointsOutsideExplicitLoopbackTests() {
        UtilityMcpProperties.MicrosoftLearn config = new UtilityMcpProperties.MicrosoftLearn();
        config.setEndpoint(URI.create("http://example.com/api/mcp"));

        assertThatThrownBy(() -> new MicrosoftLearnMcpClient(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use HTTPS");
    }

    @Test
    void disabledIntegrationDoesNotAttemptAConnection() {
        UtilityMcpProperties.MicrosoftLearn config = new UtilityMcpProperties.MicrosoftLearn();
        config.setEnabled(false);
        try (MicrosoftLearnMcpClient client = new MicrosoftLearnMcpClient(config)) {
            assertThatThrownBy(() -> client.call("microsoft_docs_search", Map.of("query", "Azure")))
                    .isInstanceOfSatisfying(UtilityToolException.class,
                            ex -> assertThat(ex.code()).isEqualTo(UtilityErrorCode.UPSTREAM_UNAVAILABLE));
        }
    }
}
