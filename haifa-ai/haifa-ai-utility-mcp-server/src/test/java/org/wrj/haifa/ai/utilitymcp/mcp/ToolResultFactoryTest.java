package org.wrj.haifa.ai.utilitymcp.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.utilitymcp.config.UtilityMcpProperties;

class ToolResultFactoryTest {

    @Test
    void returnsStructuredContentAndTextFallback() {
        ToolResultFactory factory = new ToolResultFactory(new ObjectMapper(), new UtilityMcpProperties());

        var result = factory.success(UtilityResult.local(Map.of("value", 42)));

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst()).asString().contains("value");
    }

    @Test
    void mapsBusinessErrorsWithoutProtocolFailure() {
        ToolResultFactory factory = new ToolResultFactory(new ObjectMapper(), new UtilityMcpProperties());

        var result = factory.error(UtilityToolException.invalid("bad timezone"));

        assertThat(result.isError()).isTrue();
        assertThat(result.content().getFirst()).asString().contains("INVALID_ARGUMENT");
    }
}
